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

import org.wildfly.build.logger.ProvisioningLogger;

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
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("null or empty version");
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

    public static Artifact parse(String description) {
        String[] parts = description.split(":");
        switch (parts.length) {
        case 3:
            return new Artifact(parts[0], parts[1], null, "jar", parts[2]);
        case 5:
            return new Artifact(parts[0], parts[1], parts[3], parts[2], parts[4]);
        default:
            throw ProvisioningLogger.ROOT_LOGGER.cannotParseArtifact(description);
        }
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(GACE.groupId).append(':').append(GACE.artifactId);
        if (GACE.extension != null || GACE.classifier != null) {
            sb.append(':');
            if (GACE.extension != null) {
                sb.append(GACE.extension);
            }
            sb.append(':');
            if (GACE.classifier != null) {
                sb.append(GACE.classifier);
            }
        }
        sb.append(':').append(version);
        return sb.toString();
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

        public static String canonicalize(String description) {
            // TODO this can probably be optimized because in most cases the description already is canonical
            return parse(description).toString();
        }

        public static GACE parse(String description) {
            String[] parts = description.split(":");
            switch (parts.length) {
            case 2:
                return new GACE(parts[0], parts[1], null, null);
            case 3:
                return new GACE(parts[0], parts[1], null, parts[2]);
            case 4:
                return new GACE(parts[0], parts[1], parts[3], parts[2]);
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
            if (extension != null) {
                sb.append(':').append(extension);
                if (classifier != null) {
                    sb.append(':').append(classifier);
                }
            } else {
                /* extension == null */
                if (classifier != null) {
                    sb.append("::").append(classifier);
                }
            }
            return sb.toString();
        }
    }
}
