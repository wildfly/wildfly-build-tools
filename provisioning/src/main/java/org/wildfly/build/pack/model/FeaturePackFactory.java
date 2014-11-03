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

package org.wildfly.build.pack.model;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.ArtifactVersionOverrider;
import org.wildfly.build.Locations;
import org.wildfly.build.util.MapArtifactResolver;
import org.wildfly.build.util.PropertyResolver;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Factory class that creates a feature pack from its artifact.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class FeaturePackFactory {

    private static final String CONFIGURATION_ENTRY_NAME_PREFIX = Locations.CONFIGURATION + "/";
    private static final String MODULES_ENTRY_NAME_PREFIX = Locations.MODULES + "/";
    private static final String CONTENT_ENTRY_NAME_PREFIX = Locations.CONTENT + "/";

    /**
     *
     * @param artifact the feature pack's artifact
     * @param artifactFileResolver the artifact -> artifact file resolver
     * @return
     */
    public static FeaturePack createPack(final Artifact artifact, final ArtifactFileResolver artifactFileResolver) {
        return createPack(artifact, artifactFileResolver, null, null, new HashSet<Artifact>());
    }

    /**
     *
     * @param artifact the feature pack's artifact
     * @param artifactFileResolver the artifact -> artifact file resolver
     * @param artifactResolverParent if provided, the feature pack's artifact resolver will first try resolving artifacts from its parent
     * @param artifactVersionOverrider if provided, will be used to override versions of artifacts referenced by the feature pack
     * @return
     */
    public static FeaturePack createPack(final Artifact artifact, final ArtifactFileResolver artifactFileResolver, ArtifactResolver artifactResolverParent, ArtifactVersionOverrider artifactVersionOverrider) {
        return createPack(artifact, artifactFileResolver, artifactResolverParent, artifactVersionOverrider, new HashSet<Artifact>());
    }

    private static FeaturePack createPack(final Artifact artifact, final ArtifactFileResolver artifactFileResolver, ArtifactResolver artifactResolverParent, ArtifactVersionOverrider artifactVersionOverrider, Set<Artifact> processedFeaturePacks) {
        if (!processedFeaturePacks.add(artifact)) {
            throw new IllegalStateException("Cyclic dependency, feature pack "+artifact+" already processed! Feature packs: "+processedFeaturePacks);
        }
        // resolve feature pack artifact file
        File artifactFile = artifactFileResolver.getArtifactFile(artifact);
        if(artifactFile == null) {
            throw new RuntimeException("Could not resolve artifact file for feature package  " + artifact);
        }
        // process the artifact file
        try(JarFile jar = new JarFile(artifactFile)) {
            // create list of files in the artifact file
            final List<String> configurationFiles = new ArrayList<>();
            final List<String> modulesFiles = new ArrayList<>();
            final List<String> contentFiles = new ArrayList<>();
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                final String entryName = entry.getName();
                if (entryName.startsWith(CONFIGURATION_ENTRY_NAME_PREFIX)) {
                    configurationFiles.add(entryName);
                } else if (entryName.startsWith(MODULES_ENTRY_NAME_PREFIX)) {
                    modulesFiles.add(entryName);
                } else if (entryName.startsWith(CONTENT_ENTRY_NAME_PREFIX)) {
                    contentFiles.add(entryName);
                }
            }
            // create description
            final FeaturePackDescription description = createFeaturePackDescription(jar);
            // create feature pack artifact resolver
            final ArtifactResolver artifactRefsResolver = new MapArtifactResolver(description.getArtifactRefs());
            if (artifactVersionOverrider != null) {
                artifactVersionOverrider.override(artifactRefsResolver);
            }
            ArtifactResolver featurePackArtifactResolver = new DelegatingArtifactResolver(artifactResolverParent, artifactRefsResolver);
            // create dependencies feature packs
            final List<FeaturePack> dependencies = new ArrayList<>();
            for (String dependency : description.getDependencies()) {
                dependencies.add(createPack(FeaturePack.resolveArtifact(dependency, featurePackArtifactResolver), artifactFileResolver, featurePackArtifactResolver, artifactVersionOverrider, new HashSet<>(processedFeaturePacks)));
            }
            return new FeaturePack(artifactFile, artifact, description, dependencies, featurePackArtifactResolver, configurationFiles, modulesFiles, contentFiles);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create feature pack from " + artifact, e);
        }
    }

    private static FeaturePackDescription createFeaturePackDescription(JarFile jar) throws IOException, XMLStreamException {
        ZipEntry zipEntry = jar.getEntry(Locations.FEATURE_PACK_DESCRIPTION);
        if (zipEntry == null) {
            throw new IllegalArgumentException("feature pack description not found");
        }
        FeaturePackDescriptionXMLParser parser = new FeaturePackDescriptionXMLParser(PropertyResolver.NO_OP);
        try(InputStream inputStream = jar.getInputStream(zipEntry)) {
            return parser.parse(inputStream);
        }
    }
}
