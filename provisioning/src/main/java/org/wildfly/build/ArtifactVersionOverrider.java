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
package org.wildfly.build;

import org.wildfly.build.pack.model.Artifact;

import java.util.Map;

/**
 * The artifact version overrider is a "build" facility, which is used to override the versions of specific artifacts included in XML descriptors and feature packs.
 * @author Eduardo Martins
 */
public interface ArtifactVersionOverrider {

    /**
     * Overrides versions of all artifacts in the specified resolver, which the overrider has a different version.
     * @param artifactResolver
     */
    void override(ArtifactResolver artifactResolver);

    /**
     * Overrides the provided artifact's version, if the overrider has a different version.
     * @param artifactName
     * @param artifact
     */
    void override(String artifactName, Artifact artifact);

    /**
     * Retrieves a map containing all artifacts the instance has overridden.
     * @return
     */
    Map<String, Artifact> getOverriddenArtifacts();
}
