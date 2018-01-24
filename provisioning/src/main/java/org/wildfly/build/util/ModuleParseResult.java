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

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Document;

import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.ModuleIdentifier;

/**
 * @author Stuart Douglas
 */
public class ModuleParseResult {
    final List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
    final List<ResourceRoot> resourceRoots = new ArrayList<>();
    final List<ArtifactName> artifacts = new ArrayList<>();
    final Document document;
    ModuleIdentifier identifier;
    ArtifactName versionArtifactName;

    public ModuleParseResult(final Document document) {
        this.document = document;
    }

    public List<ModuleDependency> getDependencies() {
        return dependencies;
    }

    public List<ResourceRoot> getResourceRoots() {
        return resourceRoots;
    }

    public List<ArtifactName> getArtifacts() {
        return artifacts;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public Document getDocument() {
        return document;
    }

    public ArtifactName getVersionArtifactName() {
        return versionArtifactName;
    }

    public static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        public ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
            this.moduleId = moduleId;
            this.optional = optional;
        }

        public ModuleIdentifier getModuleId() {
            return moduleId;
        }

        public boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return "[" + moduleId + (optional ? ",optional=true" : "") + "]";
        }
    }

    public static class ArtifactName {

        private final String artifactCoords;
        private final String options;
        private final Attribute attribute;

        public ArtifactName(String artifactCoords, String options, final Attribute attribute) {
            this.artifactCoords = artifactCoords;
            this.options = options;
            this.attribute = attribute;
        }

        public String getArtifactCoords() {
            return artifactCoords;
        }

        public String getOptions() {
            return options;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(artifactCoords);
            if (options != null) {
                sb.append('?').append(options);
            }
            return sb.toString();
        }

        public boolean hasVersion() {
            String[] parts = artifactCoords.split(":");
            if(parts.length > 2) {
                String version = parts[2];
                if(version != null && !version.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        public Artifact getArtifact() {
            if(!hasVersion()) {
                throw new IllegalStateException("can only be called when version is hard coded");
            }
            String[] parts = getArtifactCoords().split(":");
            if(parts.length == 3) {
                return new Artifact(parts[0], parts[1], null, "jar", parts[2]);
            } else {
                return new Artifact(parts[0], parts[1], parts[3], "jar", parts[2]);
            }
        }
    }

    public static class ResourceRoot {

        private final String path;
        private final Attribute attribute;

        public ResourceRoot(String path, final Attribute attribute) {
            this.path = path;
            this.attribute = attribute;
        }

        public String getPath() {
            return path;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
