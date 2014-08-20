package org.wildfly.build.pack.model;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.Locations;
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
 * Factory class that creates a feature pack from its artifact coordinates.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class FeaturePackFactory {

    private static final String CONFIGURATION_ENTRY_NAME_PREFIX = Locations.CONFIGURATION + "/";
    private static final String MODULES_ENTRY_NAME_PREFIX = Locations.MODULES + "/";
    private static final String CONTENT_ENTRY_NAME_PREFIX = Locations.CONTENT + "/";

    public static FeaturePack createPack(final String artifactCoords, final ArtifactResolver artifactResolver, final ArtifactFileResolver artifactFileResolver) {
        return createPack(artifactCoords, artifactResolver, artifactFileResolver, new HashSet<String>());
    }

    /**
     *
     * @param artifactCoords the coordinates of the feature pack artifact
     * @param artifactResolver the coordinates -> artifact resolver
     * @param artifactFileResolver the artifact -> artifact file resolver
     * @param processedFeaturePacks a set containing all parent feature packs, useful to detect cyclic dependencies
     * @return
     */
    private static FeaturePack createPack(final String artifactCoords, final ArtifactResolver artifactResolver, final ArtifactFileResolver artifactFileResolver, Set<String> processedFeaturePacks) {
        if (!processedFeaturePacks.add(artifactCoords)) {
            throw new IllegalStateException("Cyclic dependency, feature pack "+artifactCoords+" already processed! Feature packs: "+processedFeaturePacks);
        }
        // resolve feature pack artifact
        final Artifact artifact = artifactResolver.getArtifact(artifactCoords);
        if(artifact == null) {
            throw new RuntimeException("Could not resolve artifact for feature package " + artifactCoords);
        }
        // resolve feature pack artifact file
        File artifactFile = artifactFileResolver.getArtifactFile(artifact);
        if(artifactFile == null) {
            throw new RuntimeException("Could not resolve artifact file for feature package  " + artifactCoords);
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
            final FeaturePackArtifactResolver featurePackArtifactResolver = new FeaturePackArtifactResolver(description, artifactResolver);
            // create dependencies feature packs
            final List<FeaturePack> dependencies = new ArrayList<>();
            for (String dependency : description.getDependencies()) {
                dependencies.add(createPack(dependency, featurePackArtifactResolver, artifactFileResolver, new HashSet<>(processedFeaturePacks)));
            }
            return new FeaturePack(artifactFile, artifact, description, dependencies, featurePackArtifactResolver, configurationFiles, modulesFiles, contentFiles);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create feature pack from " + artifactCoords, e);
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
