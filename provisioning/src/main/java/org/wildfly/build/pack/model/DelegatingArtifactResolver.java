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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Stuart Douglas
 */
public class DelegatingArtifactResolver implements ArtifactResolver {

    private final List<ArtifactResolver> resolvers = new ArrayList<>();

    public DelegatingArtifactResolver(ArtifactResolver... resolvers) {
        for (ArtifactResolver resolver : resolvers) {
            if (resolver != null) {
                this.resolvers.add(resolver);
            }
        }
    }

    @Override
    public Artifact getArtifact(String artifactName) {
        for(ArtifactResolver resolver : resolvers) {
            Artifact res = resolver.getArtifact(artifactName);
            if(res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public Set<String> getArtifactNames() {
        Set<String> result = new HashSet<>();
        for(ArtifactResolver resolver : resolvers) {
            result.addAll(resolver.getArtifactNames());
        }
        return result;
    }
}
