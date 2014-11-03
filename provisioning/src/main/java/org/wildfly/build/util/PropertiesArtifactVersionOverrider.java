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
import org.wildfly.build.ArtifactVersionOverrider;
import org.wildfly.build.pack.model.Artifact;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A {@link org.wildfly.build.ArtifactVersionOverrider} built on top of {@link java.util.Properties}.
 *
 * Each property defines an artifact version override, and its name should be the artifact name prefixed with "version.". The property value is the override version.
 * Properties which name does not follows the expected format are ignored.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class PropertiesArtifactVersionOverrider implements ArtifactVersionOverrider {

    public static final String OVERRIDE_PROPERTY_NAME_PREFIX = "version.";

    private final Map<String, Artifact> overriddenArtifacts = new HashMap<>();
    private final Map<String, String> overrideVersions = new HashMap<>();

    public PropertiesArtifactVersionOverrider(Properties overrideProperties) {
        if (overrideProperties != null) {
            for (String propertyName : overrideProperties.stringPropertyNames()) {
                if (propertyName.startsWith(OVERRIDE_PROPERTY_NAME_PREFIX)) {
                    final String artifactName = propertyName.substring(OVERRIDE_PROPERTY_NAME_PREFIX.length());
                    final String artifactVersion = overrideProperties.getProperty(propertyName);
                    overrideVersions.put(artifactName, artifactVersion);
                }
            }
        }
    }

    @Override
    public void override(ArtifactResolver artifactResolver) {
        for (String artifactName : artifactResolver.getArtifactNames()) {
            override(artifactName, artifactResolver.getArtifact(artifactName));
        }
    }

    @Override
    public void override(String artifactName, Artifact artifact) {
        final String overrideVersion = overrideVersions.get(artifactName);
        if (overrideVersion != null && !overrideVersion.equals(artifact.getVersion())) {
            artifact.setVersion(overrideVersion);
            overriddenArtifacts.put(artifactName, artifact);
        }
    }

    @Override
    public Map<String, Artifact> getOverriddenArtifacts() {
        return Collections.unmodifiableMap(overriddenArtifacts);
    }
}