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
    public Artifact getArtifact(String coords) {
        String version = (String) properties.get("version." + coords);
        if(version == null) {
            return null;
        }
        String[] parts = coords.split(":");
        if(parts.length == 2) {
            return new Artifact(parts[0], parts[1], null, "jar", version);
        } else {
            return new Artifact(parts[0], parts[1], parts[3], "jar", version);
        }
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
