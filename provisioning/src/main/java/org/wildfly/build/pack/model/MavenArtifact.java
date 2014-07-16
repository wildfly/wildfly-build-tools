package org.wildfly.build.pack.model;

/**
 * A representation of a maven artifact
 *
 * @author Stuart Douglas
 */
public class MavenArtifact {

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;

    public MavenArtifact(String groupId, String artifactId, String classifier, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getVersion() {
        return version;
    }
}
