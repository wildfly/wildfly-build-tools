package org.wildfly.build.provisioning.model;

import java.util.ArrayList;
import java.util.List;

import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.common.model.CopyArtifact;

/**
 * Representation of the server provisioning config
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningDescription {

    private final List<FeaturePack> featurePacks = new ArrayList<>();
    private final List<Artifact> versionOverrides = new ArrayList<>();
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

    public List<Artifact> getVersionOverrides() {
        return versionOverrides;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    /**
     *
     */
    public static class FeaturePack {

        private final Artifact artifact;

        private final List<ModuleFilter> moduleFilters;

        private final ConfigOverride configOverride;

        private final List<FileFilter> contentFilters;

        FeaturePack(Artifact artifact, List<ModuleFilter> moduleFilters, ConfigOverride configOverride, List<FileFilter> contentFilters) {
            this.artifact = artifact;
            this.moduleFilters = moduleFilters;
            this.configOverride = configOverride;
            this.contentFilters = contentFilters;
        }

        public Artifact getArtifact() {
            return artifact;
        }

        /**
         *
         * @return a list containing all modules filters; if null or empty that translates to include all modules
         */
        public List<ModuleFilter> getModuleFilters() {
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
         * @return a list containing all content filters; if null or empty that translates to include all content files
         */
        public List<FileFilter> getContentFilters() {
            return contentFilters;
        }
    }
}
