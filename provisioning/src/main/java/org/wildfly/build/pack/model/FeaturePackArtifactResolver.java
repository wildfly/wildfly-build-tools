package org.wildfly.build.pack.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.wildfly.build.ArtifactResolver;

/**
 * @author Eduardo Martins
 */
public class FeaturePackArtifactResolver implements ArtifactResolver {

    private final Map<String, Artifact> artifactMap;

    public FeaturePackArtifactResolver(Collection<Artifact> artifactVersions) {
        this.artifactMap = new HashMap<>();
        for (Artifact artifact : artifactVersions) {
            StringBuilder sb = new StringBuilder();
            sb.append(artifact.getGACE().getGroupId());
            sb.append(':');
            sb.append(artifact.getGACE().getArtifactId());
            if (artifact.getGACE().getClassifier() != null && !artifact.getGACE().getClassifier().isEmpty()) {
                artifactMap.put(sb.append("::").append(artifact.getGACE().getClassifier()).toString(), artifact);
            } else {
                artifactMap.put(sb.toString(), artifact);
            }
        }
    }

    @Override
    public Artifact getArtifact(String artifactCoords) {
        return artifactMap.get(artifactCoords);
    }

    @Override
    public Artifact getArtifact(Artifact.GACE GACE) {
        StringBuilder sb = new StringBuilder();
        sb.append(GACE.getGroupId());
        sb.append(':');
        sb.append(GACE.getArtifactId());
        if (GACE.getClassifier() != null) {
            sb.append("::").append(GACE.getClassifier());
        }
        return getArtifact(sb.toString());
    }
}
