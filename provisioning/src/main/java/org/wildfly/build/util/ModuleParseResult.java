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
