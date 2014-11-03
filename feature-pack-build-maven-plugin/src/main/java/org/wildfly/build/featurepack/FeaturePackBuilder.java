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

import org.jboss.logging.Logger;
import org.wildfly.build.ArtifactVersionOverrider;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.featurepack.model.FeaturePackBuild;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.FeaturePackDescription;
import org.wildfly.build.pack.model.FeaturePackDescriptionXMLWriter10;
import org.wildfly.build.pack.model.FeaturePackFactory;
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

    public static void build(FeaturePackBuild build, File serverDirectory, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver, ArtifactVersionOverrider artifactVersionOverrider) {

        //List of errors that were encountered. These will be reported at the end so they are all reported in one go.
        final List<String> errors = new ArrayList<>();
        final Set<ModuleIdentifier> knownModules = new HashSet<>();
        final Map<String, Artifact> artifactRefs = new HashMap<>();
        final FeaturePackDescription featurePackDescription = new FeaturePackDescription(build.getDependencies(), build.getConfig(), build.getCopyArtifacts(), build.getFilePermissions());
        try {
            processDependencies(build.getDependencies(), knownModules, new HashSet<String>(), buildArtifactResolver, artifactFileResolver, artifactVersionOverrider, artifactRefs);
            processModulesDirectory(knownModules, serverDirectory, buildArtifactResolver, artifactRefs, errors);
            processArtifactRefs(featurePackDescription, buildArtifactResolver, artifactVersionOverrider, artifactRefs);
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

    private static void processDependencies(List<String> dependencies, Set<ModuleIdentifier> knownModules, Set<String> featurePacksProcessed, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver, ArtifactVersionOverrider artifactVersionOverrider, Map<String, Artifact> artifactRefs) {
        for (String dependency : dependencies) {
            if (!featurePacksProcessed.add(dependency)) {
                continue;
            }
            Artifact dependencyArtifact = FeaturePack.resolveArtifact(dependency, buildArtifactResolver);
            // load the dependency feature pack
            FeaturePack dependencyFeaturePack = FeaturePackFactory.createPack(dependencyArtifact, artifactFileResolver, buildArtifactResolver, artifactVersionOverrider);
            // put its artifact to the artifact refs
            artifactRefs.put(dependency, dependencyArtifact);
            // process it
            processDependency(dependencyFeaturePack, knownModules, buildArtifactResolver, artifactRefs);
        }
    }

    private static void processDependency(FeaturePack dependencyFeaturePack, Set<ModuleIdentifier> knownModules, ArtifactResolver buildArtifactResolver, final Map<String, Artifact> artifactRefs) {
        // collect artifact refs overridden by the build resolver
        for (String dependencyArtifactRef : dependencyFeaturePack.getDescription().getArtifactRefs().keySet()) {
            Artifact artifactOverride = buildArtifactResolver.getArtifact(dependencyArtifactRef);
            if (artifactOverride != null) {
                artifactRefs.put(dependencyArtifactRef, artifactOverride);
            }
        }
        // collect all modules
        knownModules.addAll(dependencyFeaturePack.getFeaturePackModules().keySet());
        // process its dependencies too
        for (FeaturePack featurePack : dependencyFeaturePack.getDependencies()) {
            processDependency(featurePack, knownModules, buildArtifactResolver, artifactRefs);
        }
    }

    private static void processModulesDirectory(Set<ModuleIdentifier> packProvidedModules, File serverDirectory, final ArtifactResolver buildArtifactResolver, final Map<String, Artifact> artifactRefs, final List<String> errors) throws IOException {
        final Path modulesDir = Paths.get(new File(serverDirectory, Locations.MODULES).getAbsolutePath());
        final HashSet<ModuleIdentifier> knownModules = new HashSet<>(packProvidedModules);
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
                        Artifact artifact = Artifact.resolve(artifactName.getArtifactCoords(), buildArtifactResolver);
                        if(artifact == null) {
                            errors.add("Could not determine version for artifact " + artifactName);
                        }
                        artifactRefs.put(artifactName.getArtifactCoords(), artifact);
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

                } catch (XMLStreamException e) {
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

    private static void processArtifactRefs(FeaturePackDescription featurePackDescription, ArtifactResolver artifactResolver, ArtifactVersionOverrider artifactVersionOverrider, Map<String, Artifact> artifactRefs) {
        // resolve copy-artifact versions and add to map
        for (CopyArtifact copyArtifact : featurePackDescription.getCopyArtifacts()) {
            final String artifactName = copyArtifact.getArtifact();
            final Artifact artifact = Artifact.resolve(artifactName, artifactResolver);
            if(artifact == null) {
                throw new RuntimeException("Could not resolve artifact for copy artifact " + copyArtifact.getArtifact());
            }
            artifactRefs.put(artifactName, artifact);
        }
        if (artifactVersionOverrider != null) {
            // gather overriden artifacts
            artifactRefs.putAll(artifactVersionOverrider.getOverriddenArtifacts());
        }
        // fill feature pack description versions
        featurePackDescription.getArtifactRefs().putAll(artifactRefs);
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
        FeaturePackDescriptionXMLWriter10.INSTANCE.write(featurePackDescription, outputFile);
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
