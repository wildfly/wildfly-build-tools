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
