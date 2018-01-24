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
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.common.model.CopyArtifact;
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
import org.wildfly.build.util.PropertyResolver;

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

    public static void build(FeaturePackBuild build, File serverDirectory, PropertyResolver propertyResolver, ArtifactResolver artifactResolver, ArtifactFileResolver artifactFileResolver) {

        //List of errors that were encountered. These will be reported at the end so they are all reported in one go.
        final List<String> errors = new ArrayList<>();
        final Set<ModuleIdentifier> knownModules = new HashSet<>();
        final Map<Artifact.GACE, String> artifactVersionMap = new HashMap<>();
        final FeaturePackDescription featurePackDescription = new FeaturePackDescription(build.getDependencies(), build.getConfig(), build.getCopyArtifacts(), build.getFilePermissions());
        try {
            processDependencies(build.getDependencies(), knownModules, new HashSet<String>(), propertyResolver, artifactResolver, artifactFileResolver, artifactVersionMap);
            processModulesDirectory(knownModules, serverDirectory, propertyResolver, artifactResolver, artifactVersionMap, errors);
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

    private static void processDependencies(List<String> dependencies, Set<ModuleIdentifier> knownModules, Set<String> featurePacksProcessed, PropertyResolver propertyResolver, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver, final Map<Artifact.GACE, String> artifactVersionMap) {
        for (String dependency : dependencies) {
            if (!featurePacksProcessed.add(dependency)) {
                continue;
            }
            Artifact dependencyArtifact = buildArtifactResolver.getArtifact(dependency);
            if(dependencyArtifact == null) {
                dependencyArtifact = buildArtifactResolver.getArtifact(dependency + ":zip"); //feature packs should be zip artifacts
            }
            if (dependencyArtifact == null) {
                throw new RuntimeException("Could not find artifact for " + dependency);
            }
            // load the dependency feature pack
            FeaturePack dependencyFeaturePack = FeaturePackFactory.createPack(dependencyArtifact, propertyResolver, artifactFileResolver, new FeaturePackArtifactResolver(Collections.<Artifact>emptyList()));
            // put its artifact to the version map
            artifactVersionMap.put(dependencyFeaturePack.getArtifact().getGACE(), dependencyFeaturePack.getArtifact().getVersion());
            // process it
            processDependency(dependencyFeaturePack, knownModules, buildArtifactResolver, artifactVersionMap);
        }
    }

    private static void processDependency(FeaturePack dependencyFeaturePack, Set<ModuleIdentifier> knownModules, ArtifactResolver buildArtifactResolver, Map<Artifact.GACE, String> artifactVersionMap) {
        // the new feature pack may override an artifact version for its dependencies, if that's the case it goes to the version map too
        for (Artifact dependencyVersionArtifact : dependencyFeaturePack.getDescription().getArtifactVersions()) {
            if (!artifactVersionMap.containsKey(dependencyVersionArtifact.getGACE())) {
                Artifact artifact = buildArtifactResolver.getArtifact(dependencyVersionArtifact.getGACE());
                if (artifact != null) {
                    artifactVersionMap.put(artifact.getGACE(), artifact.getVersion());
                }
            }
        }
        knownModules.addAll(dependencyFeaturePack.getFeaturePackModules().keySet());
        // process its dependencies too
        for (FeaturePack featurePack : dependencyFeaturePack.getDependencies()) {
            processDependency(featurePack, knownModules, buildArtifactResolver, artifactVersionMap);
        }
    }

    private static void processModulesDirectory(Set<ModuleIdentifier> packProvidedModules, File serverDirectory,
            final PropertyResolver propertyResolver, final ArtifactResolver artifactResolver,
            final Map<Artifact.GACE, String> artifactVersionMap, final List<String> errors) throws IOException {
        final Path modulesDir = Paths.get(new File(serverDirectory, Locations.MODULES).getAbsolutePath());
        if (Files.exists(modulesDir)) {
            final ModuleParser moduleParser = new ModuleParser(propertyResolver);
            final HashSet<ModuleIdentifier> knownModules = new HashSet<>(packProvidedModules);
            final Map<ModuleIdentifier, Set<ModuleIdentifier>> requiredDepds = new HashMap<>();
            Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().equals("module.xml")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        ModuleParseResult result = moduleParser.parse(file);
                        knownModules.add(result.getIdentifier());
                        for (ModuleParseResult.ArtifactName artifactName : result.getArtifacts()) {

                            Artifact artifact;
                            if(artifactName.hasVersion()) {
                                artifact = artifactName.getArtifact();
                            } else {
                                artifact = artifactResolver.getArtifact(artifactName.getArtifactCoords());
                            }
                            if(artifact == null) {
                                errors.add("Could not determine version for artifact " + artifactName);
                            } else {
                                artifactVersionMap.put(artifact.getGACE(), artifact.getVersion());
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

    private static void processVersions(FeaturePackDescription featurePackDescription, ArtifactResolver artifactResolver, Map<Artifact.GACE, String> artifactVersionMap) {
        // resolve copy-artifact versions and add to map
        for (CopyArtifact copyArtifact : featurePackDescription.getCopyArtifacts()) {
            CopyArtifact.ArtifactName artifactName = copyArtifact.getArtifact();

            final Artifact artifact;
            if (artifactName.hasVersion()) {
                artifact = artifactName.getArtifact();
            } else {
                artifact = artifactResolver.getArtifact(artifactName.getArtifactCoords());
                if(artifact == null) {
                    throw new RuntimeException("Could not resolve artifact for copy artifact " + copyArtifact.getArtifact());
                }
                artifactVersionMap.put(artifact.getGACE(), artifact.getVersion());
            }
        }
        // fill feature pack description versions
        for (Map.Entry<Artifact.GACE, String> mapEntry : artifactVersionMap.entrySet()) {
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
