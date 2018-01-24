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
import org.wildfly.build.pack.model.Artifact.GACE;

/**
 * @author Eduardo Martins
 */
public class FeaturePackArtifactResolver implements ArtifactResolver {

    private final Map<String, Artifact> artifactMap;

    public FeaturePackArtifactResolver(Collection<Artifact> artifactVersions) {
        this.artifactMap = new HashMap<>();
        for (Artifact artifact : artifactVersions) {
            artifactMap.put(artifact.getGACE().toString(), artifact);
        }
    }

    @Override
    public Artifact getArtifact(String artifactCoords) {
        return artifactMap.get(GACE.canonicalize(artifactCoords));
    }

    @Override
    public Artifact getArtifact(Artifact.GACE GACE) {
        return artifactMap.get(GACE.toString());
    }
}
