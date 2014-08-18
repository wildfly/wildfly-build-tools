package org.wildfly.build.plugin;

import org.apache.maven.project.MavenProject;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.Artifact;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public class MavenProjectArtifactResolver implements ArtifactResolver {

    private final Map<String, Artifact> artifactMap;

    public MavenProjectArtifactResolver(MavenProject mavenProject) {
        this.artifactMap = new HashMap<>();
        for (org.apache.maven.artifact.Artifact mavenProjectArtifact : mavenProject.getArtifacts()) {
            final Artifact artifact = new Artifact(mavenProjectArtifact.getGroupId(), mavenProjectArtifact.getArtifactId(), mavenProjectArtifact.getClassifier(), mavenProjectArtifact.getType(), mavenProjectArtifact.getVersion());
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
