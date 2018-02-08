package org.wildfly.build.pack.model;

import java.util.Comparator;
import java.util.Objects;

import org.wildfly.build.logger.ProvisioningLogger;

/**
 * A representation of a maven GAV, with the version being optional.
 * <p>
 * This is represented as:
 * <p>
 * groupId:artifactId[:packaging[:classifier[:version]]]
 * <p>
 * Semi colons are not optional, so to repsent a traditional maven GAV we need:
 * <p>
 * group:artifact:::version.
 * <p>
 * This is because unlike a normal maven GAV the version is optional, so group:artifact:type and group:artifact:version
 * are ambiguous.
 */
public class Artifact implements Comparable<Artifact> {
    private final String groupId;
    private final String artifactId;
    private final String packaging;
    private final String classifier;
    private final String version;

    public Artifact(String groupId, String artifactId, String packaging, String classifier, String version) {
        if (groupId == null) {
            throw new IllegalArgumentException("null groupId");
        }
        this.groupId = groupId;
        if (artifactId == null) {
            throw new IllegalArgumentException("null artifactId");
        }
        this.artifactId = artifactId;
        if (packaging != null && !packaging.equals("jar") && !packaging.isEmpty()) {
            this.packaging = packaging;
        } else {
            this.packaging = null;
        }
        if (classifier != null && !classifier.isEmpty()) {
            this.classifier = classifier;
        } else {
            this.classifier = null;
        }
        if (version != null && !version.isEmpty()) {
            this.version = version;
        } else {
            this.version = null;
        }
    }

    public Artifact(Artifact artifact, String newVersion) {
        this(artifact.groupId, artifact.artifactId, artifact.packaging, artifact.classifier, newVersion);
    }

    public static Artifact parse(String description) {
        String[] parts = description.split(":");
        switch (parts.length) {
            case 2:
                return new Artifact(parts[0], parts[1], null, null, null);
            case 3:
                return new Artifact(parts[0], parts[1], parts[2], null, null);
            case 4:
                return new Artifact(parts[0], parts[1], parts[2], parts[3], null);
            case 5:
                return new Artifact(parts[0], parts[1], parts[2], parts[3], parts[4]);
            default:
                throw ProvisioningLogger.ROOT_LOGGER.cannotParseArtifact(description);
        }
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

    public String getPackaging() {
        return packaging;
    }

    public String getVersion() {
        return version;
    }

    public Artifact getUnversioned() {
        if(version == null) {
            return this;
        }
        return new Artifact(this, null);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artifact artifact = (Artifact) o;

        if (groupId != null ? !groupId.equals(artifact.groupId) : artifact.groupId != null) return false;
        if (artifactId != null ? !artifactId.equals(artifact.artifactId) : artifact.artifactId != null)
            return false;
        if (classifier != null ? !classifier.equals(artifact.classifier) : artifact.classifier != null)
            return false;
        if (packaging != null ? !packaging.equals(artifact.packaging) : artifact.packaging != null)
            return false;
        return version != null ? version.equals(artifact.version) : artifact.version == null;
    }

    @Override
    public int hashCode() {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + (packaging != null ? packaging.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    public String toJBossModulesString() {
        StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId).append(':').append(version);
        if (classifier != null) {
            sb.append(':').append(classifier);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId);
        int pc = 0;
        if (packaging != null) {
            sb.append(':').append(packaging);
        } else {
            pc++;
        }
        if (classifier != null) {
            if (pc > 0) {
                sb.append(":");
            }
            pc = 0;
            sb.append(':').append(classifier);
        } else {
            pc++;
        }
        if (version != null) {
            for (int i = 0; i < pc; ++i) {
                sb.append(':');
            }
            sb.append(':').append(version);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Artifact o) {
        Comparator<String> comp = Comparator.nullsLast(Comparator.naturalOrder());
        // compare groupIds
        int result = Objects.compare(groupId, o.groupId, comp);
        if (result != 0) {
            return result;
        }
        // groupIds are the same, compare artifactIds
        result = Objects.compare(artifactId, o.artifactId, comp);
        if (result != 0) {
            return result;
        }
        result = Objects.compare(packaging, o.packaging, comp);
        if (result != 0) {
            return result;
        }
        result = Objects.compare(classifier, o.classifier, comp);
        if (result != 0) {
            return result;
        }
        return Objects.compare(version, o.version, comp);

    }
}
