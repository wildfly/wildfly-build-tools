package org.wildfly.build.util;

import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.Artifact;

/**
 * @author Eduardo Martins
 */
public class ModuleArtifactPropertyResolver implements PropertyResolver {

    private final ArtifactResolver artifactResolver;

    public ModuleArtifactPropertyResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public String resolveProperty(String property) {
        Artifact artifact = artifactResolver.getArtifact(property);
        if (artifact != null) {
            final StringBuilder sb = new StringBuilder(artifact.getGACE().getGroupId()).append(':').append(artifact.getGACE().getArtifactId()).append(':').append(artifact.getVersion());
            if (artifact.getGACE().getClassifier() != null) {
                sb.append(':').append(artifact.getGACE().getClassifier());
            }
            return sb.toString();
        } else {
            return null;
        }
    }
}
