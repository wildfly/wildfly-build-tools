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
import org.wildfly.build.pack.model.ModuleIdentifier;

/**
 * @author Stuart Douglas
 */
public class ModuleParseResult {
    final List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
    final List<String> resourceRoots = new ArrayList<>();
    final List<String> artifacts = new ArrayList<>();
    ModuleIdentifier identifier;

    public List<ModuleDependency> getDependencies() {
        return dependencies;
    }

    public List<String> getResourceRoots() {
        return resourceRoots;
    }

    public List<String> getArtifacts() {
        return artifacts;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    public static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
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
}
