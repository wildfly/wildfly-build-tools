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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.stream.XMLStreamException;
import org.jboss.logging.Logger;
import org.wildfly.build.Locations;
import org.wildfly.build.featurepack.model.FeaturePackBuild;
import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.pack.model.Pack;
import org.wildfly.build.pack.model.PackFactory;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ModuleParser;

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
 */
public class FeaturePackBuilder {

    private static final Logger logger = Logger.getLogger(FeaturePackBuilder.class);

    public static void build(FeaturePackBuild build, File serverDirectory, ArtifactResolver artifactResolver) {
        final Map<String, String> artifactVersionMap = new TreeMap<>();
        final List<Pack> dependencyPacks = new ArrayList<>();
        //List of errors that were encountered. These will be reported at the end so they are all reported in one go.
        final List<String> errors = new ArrayList<>();
        final Set<ModuleIdentifier> knownModules = new HashSet<>();
        FileInputStream configStream = null;
        try {
            for (String server : build.getDependencies()) {
                File dep = artifactResolver.getArtifact(server);
                if(dep == null) {
                    throw new RuntimeException("Could not resolve dependency " + server);
                }
                artifactVersionMap.put(server, artifactResolver.getVersion(server));
                Pack pack = PackFactory.createPack(dep);
                dependencyPacks.add(pack);
                knownModules.addAll(pack.getModules().keySet());
            }
            processModulesDirectory(knownModules, serverDirectory, artifactResolver, artifactVersionMap, errors);
            writeVersionProperties(serverDirectory, artifactVersionMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(configStream);
        }
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

    private static void writeVersionProperties(File serverDirectory, Map<String, String> artifactVersionMap) {
        Properties p = new Properties();
        p.putAll(artifactVersionMap);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(serverDirectory, Locations.VERSIONS_PROPERTIES));
            p.store(out, "Versions of the components in this pack");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(out);
        }
    }

    private static void processModulesDirectory(Set<ModuleIdentifier> packProvidedModules, File serverDirectory, final ArtifactResolver artifactResolver, final Map<String, String> artifactVersionMap,  final List<String> errors) throws IOException {
        final Path modulesDir = Paths.get(new File(serverDirectory, "modules").getAbsolutePath());
        final HashSet<ModuleIdentifier> knownModules = new HashSet<>(packProvidedModules);
        final Map<ModuleIdentifier, Set<ModuleIdentifier>> requiredDepds = new HashMap<>();
        Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().equals("module.xml")) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    ModuleParseResult result = ModuleParser.parse(modulesDir, file);
                    knownModules.add(result.getIdentifier());
                    for (String artifactName : result.getArtifacts()) {
                        if(artifactName.startsWith("${") && artifactName.endsWith("}")) {
                            String prop = artifactName.substring(2, artifactName.length() - 1);
                            String version = artifactResolver.getVersion(prop);
                            if(version == null) {
                                errors.add("Could not determine version for artifact " + artifactName + " from resolver " + artifactResolver);
                            }
                            artifactVersionMap.put(prop, version);
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

    private static void safeClose(final Closeable... closeable) {
        for (Closeable c : closeable) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    getLog().error("Failed to close resource", e);
                }
            }
        }
    }

    static Logger getLog() {
        return logger;
    }

}
