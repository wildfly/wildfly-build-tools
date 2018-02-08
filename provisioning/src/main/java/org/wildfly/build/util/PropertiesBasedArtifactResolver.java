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

import java.util.Properties;

/**
 * properties based resolver than can be used to override specific versions of artifacts on the command line
 *
 * @author Stuart Douglas
 */
public class PropertiesBasedArtifactResolver implements ArtifactResolver {

    private final Properties properties;

    public PropertiesBasedArtifactResolver(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Artifact getArtifact(Artifact artifact) {
        String version = (String) properties.get("version." + artifact.toString());
        if(version == null) {
            return null;
        }
        return new Artifact(artifact, version);
    }
}
