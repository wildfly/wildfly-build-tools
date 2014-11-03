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

import org.wildfly.build.ArtifactResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eduardo Martins
 */
public class Artifact {

    private static final Pattern fromStringPattern = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");

    private String groupId;
    private String artifactId;
    private String classifier;
    private String extension;
    private String version;

    public Artifact(String groupId, String artifactId, String version) {
        this(groupId, artifactId, null, null, version);
    }

    public Artifact(String groupId, String artifactId, String extension, String classifier, String version) {
        setGroupId(groupId);
        setArtifactId(artifactId);
        setVersion(version);
        setClassifier(classifier);
        setExtension(extension);
    }

    public String getGroupId() {
        return groupId;
    }

    public Artifact setGroupId(String groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("null groupId");
        }
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Artifact setArtifactId(String artifactId) {
        if (artifactId == null) {
            throw new IllegalArgumentException("null artifactId");
        }
        this.artifactId = artifactId;
        return this;
    }

    public String getClassifier() {
        return classifier;
    }

    public Artifact setClassifier(String classifier) {
        if (classifier != null && !classifier.isEmpty()) {
            this.classifier = classifier;
        } else {
            this.classifier = null;
        }
        return this;
    }

    public String getExtension() {
        return extension;
    }

    public Artifact setExtension(String extension) {
        if (extension != null && !extension.equals("jar")) {
            this.extension = extension;
        } else {
            this.extension = null;
        }
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Artifact setVersion(String version) {
        if (version == null) {
            throw new IllegalArgumentException("null version");
        }
        this.version = version;
        return this;
    }

    @Override
    public String toString() {
        return new StringBuilder(getDefaultArtifactRef()).append(':').append(version).toString();
    }

    public String getDefaultArtifactRef() {
        final StringBuilder sb = new StringBuilder(groupId).append(':').append(artifactId);
        if (extension != null && !extension.isEmpty()) {
            sb.append(':').append(extension);
        }
        if (classifier != null && !classifier.isEmpty()) {
            sb.append(':').append(classifier);
        }
        return sb.toString();
    }

    /**
     * Creates a new artifact with the specified artifact name. If not specified in the artifact name, the
     * artifact's extension defaults to {@code jar} and classifier to an empty string.
     *
     * @param artifactName The artifactName coordinates in the format
     *            {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}, must not be {@code null}.
     * @return the artifact that results from parsing the specified name; null if the artifact name format is invalid.
     */
    public static Artifact fromString(String artifactName) {
        Matcher m = fromStringPattern.matcher(artifactName);
        if (!m.matches()) {
            return null;
        }
        String groupId = m.group(1);
        String artifactId = m.group(2);
        String extension = m.group(4);
        String classifier = m.group(6);
        String version = m.group(7);
        return new Artifact(groupId, artifactId, extension, classifier, version);
    }

    /**
     * Tries to resolve the artifact using the specified resolver, if that fails this method will try to build an artifact from the specified name.
     * @param artifactName
     * @param artifactResolver
     * @return
     */
    public static Artifact resolve(String artifactName, ArtifactResolver artifactResolver) {
        Artifact resolvedArtifact = artifactResolver.getArtifact(artifactName);
        if (resolvedArtifact == null) {
            // not found in resolver, try build from provided name
            final Artifact fromStringArtifact = Artifact.fromString(artifactName);
            if (fromStringArtifact != null) {
                // the artifact can be built from the provided name, retry resolver, this time using the standard artifact ref name
                resolvedArtifact = artifactResolver.getArtifact(fromStringArtifact.getDefaultArtifactRef());
                if (resolvedArtifact == null) {
                    // still not found in resolver, return the artifact built from string
                    resolvedArtifact = fromStringArtifact;
                }
            }
        }
        return resolvedArtifact;
    }

}
