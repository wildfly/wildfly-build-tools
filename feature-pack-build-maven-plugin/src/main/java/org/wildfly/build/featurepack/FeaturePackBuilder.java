/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.featurepack;

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
import org.wildfly.build.pack.model.FeaturePackDescriptionXMLWriter10;
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
        final Map<Artifact.GACE, String> artifactVersionMap = new HashMap<>();
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

    private static void processDependencies(List<String> dependencies, Set<ModuleIdentifier> knownModules, Set<String> featurePacksProcessed, ArtifactResolver buildArtifactResolver, ArtifactFileResolver artifactFileResolver, final Map<Artifact.GACE, String> artifactVersionMap) {
        for (String dependency : dependencies) {
            if (!featurePacksProcessed.add(dependency)) {
                continue;
            }
            Artifact dependencyArtifact = buildArtifactResolver.getArtifact(dependency);
            // load the dependency feature pack
            FeaturePack dependencyFeaturePack = FeaturePackFactory.createPack(dependencyArtifact, artifactFileResolver, new FeaturePackArtifactResolver(Collections.<Artifact>emptyList()));
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
        knownModules.addAll(dependencyFeaturePack.getModules().keySet());
        // process its dependencies too
        for (FeaturePack featurePack : dependencyFeaturePack.getDependencies()) {
            processDependency(featurePack, knownModules, buildArtifactResolver, artifactVersionMap);
        }
    }

    private static void processModulesDirectory(Set<ModuleIdentifier> packProvidedModules, File serverDirectory, final ArtifactResolver artifactResolver, final Map<Artifact.GACE, String> artifactVersionMap,  final List<String> errors) throws IOException {
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
                    for (String artifactName : result.getArtifacts()) {
                        if(artifactName.startsWith("${") && artifactName.endsWith("}")) {
                            String prop = artifactName.substring(2, artifactName.length() - 1);
                            if (prop.contains("?")) {
                                prop = prop.substring(0, prop.indexOf('?'));
                            }

                            Artifact artifact = artifactResolver.getArtifact(prop);
                            if(artifact == null) {
                                errors.add("Could not determine version for artifact " + artifactName);
                            }
                            artifactVersionMap.put(artifact.getGACE(), artifact.getVersion());
                        } else {
                            getLog().error("Hard coded artifact " + artifactName);
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

    private static void processVersions(FeaturePackDescription featurePackDescription, ArtifactResolver artifactResolver, Map<Artifact.GACE, String> artifactVersionMap) {
        // resolve copy-artifact versions and add to map
        for (CopyArtifact copyArtifact : featurePackDescription.getCopyArtifacts()) {
            final Artifact artifact = artifactResolver.getArtifact(copyArtifact.getArtifact());
            if(artifact == null) {
                throw new RuntimeException("Could not resolve artifact for copy artifact " + copyArtifact.getArtifact());
            }
            artifactVersionMap.put(artifact.getGACE(), artifact.getVersion());
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
