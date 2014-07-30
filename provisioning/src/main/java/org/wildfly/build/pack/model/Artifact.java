package org.wildfly.build.pack.model;

/**
 * @author Eduardo Martins
 */
public class Artifact implements Comparable<Artifact> {

    private final GACE GACE;

    private String version;

    public Artifact(String groupId, String artifactId, String classifier, String extension, String version) {
        this(new GACE(groupId, artifactId, classifier, extension), version);
    }

    public Artifact(GACE GACE, String version) {
        if (GACE == null) {
            throw new IllegalArgumentException("null gac");
        }
        this.GACE = GACE;
        if (version == null) {
            throw new IllegalArgumentException("null version");
        }
        this.version = version;
    }

    public GACE getGACE() {
        return GACE;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artifact artifact = (Artifact) o;

        if (!GACE.equals(artifact.GACE)) return false;
        if (!version.equals(artifact.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = GACE.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public int compareTo(Artifact o) {
        int result = GACE.compareTo(o.GACE);
        if (result == 0) {
            result = version.compareTo(o.version);
        }
        return result;
    }

    public static class GACE implements Comparable<GACE> {

        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String extension;

        public GACE(String groupId, String artifactId, String classifier, String extension) {
            if (groupId == null) {
                throw new IllegalArgumentException("null groupId");
            }
            this.groupId = groupId;
            if (artifactId == null) {
                throw new IllegalArgumentException("null artifactId");
            }
            this.artifactId = artifactId;
            if (classifier != null && !classifier.isEmpty()) {
                this.classifier = classifier;
            } else {
                this.classifier = null;
            }
            if (extension != null && !extension.equals("jar")) {
                this.extension = extension;
            } else {
                this.extension = null;
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

        public String getExtension() {
            return extension;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GACE GACE = (GACE) o;

            if (!groupId.equals(GACE.groupId)) return false;
            if (!artifactId.equals(GACE.artifactId)) return false;
            if (classifier != null ? !classifier.equals(GACE.classifier) : GACE.classifier != null) return false;
            if (extension != null ? !extension.equals(GACE.extension) : GACE.extension != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = groupId.hashCode();
            result = 31 * result + artifactId.hashCode();
            result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
            result = 31 * result + (extension != null ? extension.hashCode() : 0);
            return result;
        }

        @Override
        public int compareTo(GACE o) {
            // compare groupIds
            int result = groupId.compareTo(o.groupId);
            if (result == 0) {
                // groupIds are the same, compare artifactIds
                result = artifactId.compareTo(o.artifactId);
                if (result == 0) {
                    // artifactIds are the same, compare classifiers
                    if (classifier != null) {
                        if (o.classifier == null) {
                            result = 1;
                        } else {
                            result = classifier.compareTo(o.classifier);
                        }
                    } else {
                        if (o.classifier != null) {
                            result = -1;
                        }
                    }
                    if (result == 0) {
                        // classifiers are the same, compare extensions
                        if (extension != null) {
                            if (o.extension == null) {
                                result = 1;
                            } else {
                                result = extension.compareTo(o.extension);
                            }
                        } else {
                            if (o.extension != null) {
                                result = -1;
                            }
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId);
            if (classifier != null) {
                sb.append("::").append(classifier);
            }
            return sb.toString();
        }
    }
}
