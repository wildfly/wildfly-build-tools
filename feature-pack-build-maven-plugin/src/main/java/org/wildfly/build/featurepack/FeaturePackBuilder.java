/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.build.featurepack;

import nu.xom.ParsingException;
import org.jboss.logging.Logger;
import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.featurepack.model.FeaturePackBuild;
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.FeaturePackArtifactResolver;
import org.wildfly.build.pack.model.FeaturePackDescription;
import org.wildfly.build.pack.model.FeaturePackDescriptionXMLWriter11;
import org.wildfly.build.pack.model.FeaturePackFactory;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.util.FileUtils;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ModuleParser;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task that builds a feature pack. In general this task assumes that some other tool will copy the files from the build
 * to the target directory (e.g. maven).
 *
 * This tool will then verify the modules directory against the listed dependency feature packs, to make sure there
 * are no unresolved non-optional module references. It also resolves the versions of artifact and adds it to
 * the versions.properties file, and creates the feature-pack.xml file.
 *
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class FeaturePackBuilder {

    private static final Logger logger = Logger.getLogger(FeaturePackBuilder.class);

    public static void build(FeaturePackBuild build, File serverDirectory, ArtifactResolver artifactResolver, ArtifactFileResolver artifactFileResolver) {

        //List of errors that were encountered. These will be reported at the end so they are all reported in one go.
        final List<String> errors = new ArrayList<>();
        final Set<ModuleIdentifier> knownModules = new HashSet<>();
        final Map<Artifact, String> artifactVersionMap = new HashMap<>();
        final FeaturePackDescription featurePackDescription = new FeaturePackDescription(build.getDependencies(), build.getConfig(), build.getCopyArtifacts(), build.getFilePermissions());
        try {
            processDependencies(build.getDependencies(), knownModules, new HashSet<String>(), artifactResolver, artifactFileResolver, artifactVersionMap);
            processModulesDirectory(knownModules, serverDirectory, artifactResolver, artifactVersionMap, errors);
            processVersions(featurePackDescription, artifactResolver, artifactVersionMap);
            processContentsDirectory(build, serverDirectory);
            writeFeaturePackXml(featurePackDescription, serverDirectory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Some errors were encountered creating the feature pack\n");
                for(String error : errors) {
                    sb.append(error);
                    sb.append("\n");
                }
                throw new RuntimeException(sb.toString());
            }
        }
    }

    private static void processDependencies(List<String> dependencies, Set<ModuleIdentifier> knownModules, Set<String> featurePacksProcessed, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver, final Map<Artifact, String> artifactVersionMap) {
        for (String dependency : dependencies) {
            if (!featurePacksProcessed.add(dependency)) {
                continue;
            }
            Artifact artifact = Artifact.parse(dependency);
            if(artifact.getPackaging() == null) {
                artifact = new Artifact(artifact.getGroupId(), artifact.getArtifactId(), "zip", artifact.getClassifier(), artifact.getVersion());
            }
            Artifact dependencyArtifact = buildArtifactResolver.getArtifact(artifact);
            if (dependencyArtifact == null) {
                throw new RuntimeException("Could not find artifact for " + dependency);
            }
            // load the dependency feature pack
            FeaturePack dependencyFeaturePack = FeaturePackFactory.createPack(dependencyArtifact, artifactFileResolver, new FeaturePackArtifactResolver(Collections.<Artifact>emptyList()));
            // put its artifact to the version map
            artifactVersionMap.put(dependencyFeaturePack.getArtifact().getUnversioned(), dependencyFeaturePack.getArtifact().getVersion());
            // process it
            processDependency(dependencyFeaturePack, knownModules, buildArtifactResolver, artifactVersionMap);
        }
    }

    private static void processDependency(FeaturePack dependencyFeaturePack, Set<ModuleIdentifier> knownModules, ArtifactResolver buildArtifactResolver, Map<Artifact, String> artifactVersionMap) {
        // the new feature pack may override an artifact version for its dependencies, if that's the case it goes to the version map too
        for (Artifact dependencyVersionArtifact : dependencyFeaturePack.getDescription().getArtifactVersions()) {
            if (!artifactVersionMap.containsKey(dependencyVersionArtifact.getUnversioned())) {
                Artifact artifact = buildArtifactResolver.getArtifact(dependencyVersionArtifact.getUnversioned());
                if (artifact != null) {
                    artifactVersionMap.put(artifact.getUnversioned(), artifact.getVersion());
                }
            }
        }
        knownModules.addAll(dependencyFeaturePack.getFeaturePackModules().keySet());
        // process its dependencies too
        for (FeaturePack featurePack : dependencyFeaturePack.getDependencies()) {
            processDependency(featurePack, knownModules, buildArtifactResolver, artifactVersionMap);
        }
    }

    private static void processModulesDirectory(Set<ModuleIdentifier> packProvidedModules, File serverDirectory, final ArtifactResolver artifactResolver, final Map<Artifact, String> artifactVersionMap,  final List<String> errors) throws IOException {
        final Path modulesDir = Paths.get(new File(serverDirectory, Locations.MODULES).getAbsolutePath());
        if (Files.exists(modulesDir)) {
            final HashSet<ModuleIdentifier> knownModules = new HashSet<>(packProvidedModules);
            knownModules.add(ModuleIdentifier.fromString("java.se"));
            knownModules.add(ModuleIdentifier.fromString("java.sql"));
            knownModules.add(ModuleIdentifier.fromString("java.base"));
            knownModules.add(ModuleIdentifier.fromString("java.compiler"));
            knownModules.add(ModuleIdentifier.fromString("java.corba"));
            knownModules.add(ModuleIdentifier.fromString("java.datatransfer"));
            knownModules.add(ModuleIdentifier.fromString("java.desktop"));
            knownModules.add(ModuleIdentifier.fromString("java.instrument"));
            knownModules.add(ModuleIdentifier.fromString("java.logging"));
            knownModules.add(ModuleIdentifier.fromString("java.management"));
            knownModules.add(ModuleIdentifier.fromString("java.management.rmi"));
            knownModules.add(ModuleIdentifier.fromString("java.naming"));
            knownModules.add(ModuleIdentifier.fromString("java.prefs"));
            knownModules.add(ModuleIdentifier.fromString("java.rmi"));
            knownModules.add(ModuleIdentifier.fromString("java.scripting"));
            knownModules.add(ModuleIdentifier.fromString("java.security.jgss"));
            knownModules.add(ModuleIdentifier.fromString("java.security.sasl"));
            knownModules.add(ModuleIdentifier.fromString("java.smartcardio"));
            knownModules.add(ModuleIdentifier.fromString("java.sql.rowset"));
            knownModules.add(ModuleIdentifier.fromString("java.transaction"));
            knownModules.add(ModuleIdentifier.fromString("java.xml.crypto"));
            knownModules.add(ModuleIdentifier.fromString("javafx.base"));
            knownModules.add(ModuleIdentifier.fromString("javafx.controls"));
            knownModules.add(ModuleIdentifier.fromString("javafx.graphics"));
            knownModules.add(ModuleIdentifier.fromString("javafx.media"));
            knownModules.add(ModuleIdentifier.fromString("javafx.swing"));
            knownModules.add(ModuleIdentifier.fromString("javafx.web"));
            knownModules.add(ModuleIdentifier.fromString("jdk.accessibility"));
            knownModules.add(ModuleIdentifier.fromString("jdk.attach"));
            knownModules.add(ModuleIdentifier.fromString("jdk.compiler"));
            knownModules.add(ModuleIdentifier.fromString("jdk.httpserver"));
            knownModules.add(ModuleIdentifier.fromString("jdk.jartool"));
            knownModules.add(ModuleIdentifier.fromString("jdk.javadoc"));
            knownModules.add(ModuleIdentifier.fromString("jdk.jconsole"));
            knownModules.add(ModuleIdentifier.fromString("jdk.jdi"));
            knownModules.add(ModuleIdentifier.fromString("jdk.jfr"));
            knownModules.add(ModuleIdentifier.fromString("jdk.jsobject"));
            knownModules.add(ModuleIdentifier.fromString("jdk.management"));
            knownModules.add(ModuleIdentifier.fromString("jdk.management.cmm"));
            knownModules.add(ModuleIdentifier.fromString("jdk.management.jfr"));
            knownModules.add(ModuleIdentifier.fromString("jdk.management.resource"));
            knownModules.add(ModuleIdentifier.fromString("jdk.net"));
            knownModules.add(ModuleIdentifier.fromString("jdk.plugin.dom"));
            knownModules.add(ModuleIdentifier.fromString("jdk.scripting.nashorn"));
            knownModules.add(ModuleIdentifier.fromString("jdk.sctp"));
            knownModules.add(ModuleIdentifier.fromString("jdk.security.auth"));
            knownModules.add(ModuleIdentifier.fromString("jdk.security.jgss"));
            knownModules.add(ModuleIdentifier.fromString("jdk.unsupported"));
            knownModules.add(ModuleIdentifier.fromString("jdk.xml.dom"));
            knownModules.add(ModuleIdentifier.fromString("java.jnlp"));
            knownModules.add(ModuleIdentifier.fromString("java.xml"));
            knownModules.add(ModuleIdentifier.fromString("javafx.fxml"));
            knownModules.add(ModuleIdentifier.fromString("org.jboss.modules"));
            final Map<ModuleIdentifier, Set<ModuleIdentifier>> requiredDepds = new HashMap<>();
            Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().equals("module.xml")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        ModuleParseResult result = ModuleParser.parse(file);
                        knownModules.add(result.getIdentifier());
                        for (ModuleParseResult.ArtifactName artifactName : result.getArtifacts()) {

                            Artifact artifact;
                            boolean include = true;
                            if(artifactName.hasVersion()) {
                                include = false;
                                artifact = artifactName.getArtifact();
                            } else {
                                artifact = artifactResolver.getArtifact(artifactName.getArtifact());
                            }
                            if(artifact == null) {
                                errors.add("Could not determine version for artifact " + artifactName);
                            } else if(include) {
                                artifactVersionMap.put(artifact.getUnversioned(), artifact.getVersion());
                            }
                        }
                        for(ModuleParseResult.ModuleDependency dep : result.getDependencies()) {
                            if(!dep.isOptional()) {
                                Set<ModuleIdentifier> dependees = requiredDepds.get(dep.getModuleId());
                                if(dependees == null) {
                                    requiredDepds.put(dep.getModuleId(), dependees = new HashSet<>());
                                }
                                dependees.add(result.getIdentifier());
                            }
                        }

                    } catch (ParsingException e) {
                        throw new RuntimeException(e);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            //now look for unresolved dependencies
            for(Map.Entry<ModuleIdentifier, Set<ModuleIdentifier>> dep : requiredDepds.entrySet()) {
                if(!knownModules.contains(dep.getKey())) {
                    errors.add("Missing module " + dep.getKey() + ". Module was required by " + dep.getValue());
                }
            }
        }
    }

    private static void processVersions(FeaturePackDescription featurePackDescription, ArtifactResolver artifactResolver, Map<Artifact, String> artifactVersionMap) {
        // resolve copy-artifact versions and add to map
        for (CopyArtifact copyArtifact : featurePackDescription.getCopyArtifacts()) {
            if(copyArtifact.getArtifact().getVersion() == null) {
                Artifact artifact = copyArtifact.getArtifact(artifactResolver);
                artifactVersionMap.put(artifact.getUnversioned(), artifact.getVersion());
            }
        }
        // fill feature pack description versions
        for (Map.Entry<Artifact, String> mapEntry : artifactVersionMap.entrySet()) {
            featurePackDescription.getArtifactVersions().add(new Artifact(mapEntry.getKey(), mapEntry.getValue()));
        }
    }


    private static void processContentsDirectory(final FeaturePackBuild build, File serverDirectory) throws IOException {
        final File baseDir = new File(serverDirectory, Locations.CONTENT);
        // make dirs
        for (String dir : build.getMkDirs()) {
            File file = new File(baseDir, dir);
            if(!file.isDirectory()) {
                if(!file.mkdirs()) {
                    throw new RuntimeException("Could not create directory " + file);
                }
            }
        }
        // line endings
        final Path baseDirPath = Paths.get(baseDir.getAbsolutePath());
        if (!Files.exists(baseDirPath)){
            return;
        }
        Files.walkFileTree(baseDirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = baseDirPath.relativize(file).toString();
                for (FileFilter fileFilter : build.getUnix()) {
                    if (fileFilter.matches(relative)) {
                        toUnixLineEndings(file);
                    }
                }
                for (FileFilter fileFilter : build.getWindows()) {
                    if (fileFilter.matches(relative)) {
                        toWindowsLineEndings(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void writeFeaturePackXml(FeaturePackDescription featurePackDescription, File serverDirectory) throws IOException, XMLStreamException {
        final File outputFile = new File(serverDirectory, Locations.FEATURE_PACK_DESCRIPTION);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        FeaturePackDescriptionXMLWriter11.INSTANCE.write(featurePackDescription, outputFile);
    }

    private static void toUnixLineEndings(Path file) throws IOException {
        Pattern pattern = Pattern.compile("\\r\\n", Pattern.MULTILINE);
        String content = FileUtils.readFile(file.toFile());
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("\n");
        FileUtils.copyFile(new ByteArrayInputStream(content.getBytes("UTF-8")), file.toFile());
    }

    private static void toWindowsLineEndings(Path file) throws IOException {
        Pattern pattern = Pattern.compile("(?<!\\r)\\n", Pattern.MULTILINE);
        String content = FileUtils.readFile(file.toFile());
        Matcher matcher = pattern.matcher(content);
        content = matcher.replaceAll("\r\n");
        FileUtils.copyFile(new ByteArrayInputStream(content.getBytes("UTF-8")), file.toFile());
    }

    static Logger getLog() {
        return logger;
    }

}
