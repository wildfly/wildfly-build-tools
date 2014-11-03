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

/**
 * @author Eduardo Martins
 */
public class ModuleArtifactPropertyResolver implements PropertyResolver {

    private final ArtifactResolver artifactResolver;

    public ModuleArtifactPropertyResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public String resolveProperty(String property) {
        Artifact artifact = artifactResolver.getArtifact(property);
        if (artifact != null) {
            final StringBuilder sb = new StringBuilder(artifact.getGroupId()).append(':').append(artifact.getArtifactId()).append(':').append(artifact.getVersion());
            if (artifact.getClassifier() != null) {
                sb.append(':').append(artifact.getClassifier());
            }
            return sb.toString();
        } else {
            return null;
        }
    }
}
