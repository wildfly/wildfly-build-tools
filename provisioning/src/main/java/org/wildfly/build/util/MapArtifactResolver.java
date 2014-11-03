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

package org.wildfly.build.util;

import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.Artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An artifact resolver based on a 'artifact name -> artifact' map.
 * @author Eduardo Martins
 */
public class MapArtifactResolver implements ArtifactResolver {

    protected final Map<String, Artifact> artifactMap;

    public MapArtifactResolver() {
        this(new HashMap<String, Artifact>());
    }

    public MapArtifactResolver(Map<String, Artifact> artifactMap) {
        this.artifactMap = artifactMap;
    }

    @Override
    public Artifact getArtifact(String artifactName) {
        return artifactMap.get(artifactName);
    }

    @Override
    public Set<String> getArtifactNames() {
        return Collections.unmodifiableSet(artifactMap.keySet());
    }
}
