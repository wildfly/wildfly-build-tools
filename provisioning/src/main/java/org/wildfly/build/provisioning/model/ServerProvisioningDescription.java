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

package org.wildfly.build.provisioning.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.common.model.CopyArtifact;

/**
 * Representation of the server provisioning config
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningDescription {

    private final List<FeaturePack> featurePacks = new ArrayList<>();
    private final Map<String, Artifact> artifactRefs = new HashMap<>();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();

    private boolean copyModuleArtifacts;

    private boolean extractSchemas;

    public List<FeaturePack> getFeaturePacks() {
        return featurePacks;
    }

    public boolean isCopyModuleArtifacts() {
        return copyModuleArtifacts;
    }

    public void setCopyModuleArtifacts(boolean copyModuleArtifacts) {
        this.copyModuleArtifacts = copyModuleArtifacts;
    }

    public boolean isExtractSchemas() {
        return extractSchemas;
    }

    public void setExtractSchemas(boolean extractSchemas) {
        this.extractSchemas = extractSchemas;
    }

    public Map<String, Artifact> getArtifactRefs() {
        return artifactRefs;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    /**
     *
     */
    public static class FeaturePack {

        private final String artifact;
        private final ModuleFilters moduleFilters;
        private final ConfigOverride configOverride;
        private final ContentFilters contentFilters;
        private final List<Subsystem> subsystems;

        public FeaturePack(String artifact, ModuleFilters moduleFilters, ConfigOverride configOverride, ContentFilters contentFilters, List<Subsystem> subsystems) {
            this.artifact = artifact;
            this.moduleFilters = moduleFilters;
            this.configOverride = configOverride;
            this.contentFilters = contentFilters;
            this.subsystems = subsystems;
        }

        public String getArtifact() {
            return artifact;
        }

        /**
         *
         * @return the modules filters; if null that translates to include all modules
         */
        public ModuleFilters getModuleFilters() {
            return moduleFilters;
        }

        /**
         *
         * @return the configuration to include; may be null, which translates to include the feature pack configuration without any change
         */
        public ConfigOverride getConfigOverride() {
            return configOverride;
        }

        /**
         *
         * @return the content filters; if null that translates to include all content files
         */
        public ContentFilters getContentFilters() {
            return contentFilters;
        }

        /**
         *
         * @return a list containing the subsystems to include
         */
        public List<Subsystem> getSubsystems() {
            return subsystems;
        }

        /**
         *
         * @return
         */
        public boolean includesContentFiles() {
            if ((configOverride != null || subsystems != null || moduleFilters != null) && contentFilters == null) {
                // if there is any kind of other feature pack filtering, and no content filters are specified, then assume no content files should be included
                return false;
            }
            return true;
        }

        /**
         *
         */
        public static class ModuleFilters {

            private final List<ModuleFilter> filters = new ArrayList<>();
            private final boolean include;

            ModuleFilters(boolean include) {
                this.include = include;
            }

            /**
             *
             * @return a list containing all filters
             */
            public List<ModuleFilter> getFilters() {
                return filters;
            }

            /**
             *
             * @return true if modules not filtered should be included
             */
            public boolean isInclude() {
                return include;
            }
        }

        /**
         *
         */
        public static class ContentFilters {

            private final List<FileFilter> filters = new ArrayList<>();
            private final boolean include;

            ContentFilters(boolean include) {
                this.include = include;
            }

            /**
             *
             * @return a list containing all filters
             */
            public List<FileFilter> getFilters() {
                return filters;
            }

            /**
             *
             * @return true if content files not filtered should be included
             */
            public boolean isInclude() {
                return include;
            }
        }

        /**
         *
         */
        public static class Subsystem {
            private final String module;
            private final boolean transitive;

            public Subsystem(String module, boolean transitive) {
                this.module = module;
                this.transitive = transitive;
            }

            public String getName() {
                return module;
            }

            public boolean isTransitive() {
                return transitive;
            }
        }
    }
}
