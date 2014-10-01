package org.wildfly.build.provisioning.model;

import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.util.InputStreamSource;
import org.wildfly.build.util.ZipFileSubsystemInputStreamSources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Eduardo Martins
 */
public class ServerProvisioning {

    private final ServerProvisioningDescription description;

    /**
     * the server's feature packs
     */
    private final List<ServerProvisioningFeaturePack> featurePacks = new ArrayList<>();

    /**
     * the provisioning config
     */
    private final Config config = new Config();

    /**
     *
     * @param description
     */
    public ServerProvisioning(ServerProvisioningDescription description) {
        this.description = description;
    }

    /**
     *
     * @return
     */
    public ServerProvisioningDescription getDescription() {
        return description;
    }

    /**
     *
     * @return
     */
    public List<ServerProvisioningFeaturePack> getFeaturePacks() {
        return featurePacks;
    }

    /**
     * Retrieves the provisioning config
     * @return
     */
    public Config getConfig() {
        return config;
    }

    /**
     *
     */
    public static class Config {

        private final ZipFileSubsystemInputStreamSources inputStreamSources = new ZipFileSubsystemInputStreamSources();
        private final Map<String, ConfigFile> standaloneConfigFiles = new HashMap<>();
        private final Map<String, ConfigFile> domainConfigFiles = new HashMap<>();

        public Map<String, ConfigFile> getStandaloneConfigFiles() {
            return standaloneConfigFiles;
        }

        public Map<String, ConfigFile> getDomainConfigFiles() {
            return domainConfigFiles;
        }

        public ZipFileSubsystemInputStreamSources getInputStreamSources() {
            return inputStreamSources;
        }
    }

    /**
     *
     */
    public static class ConfigFile {

        private InputStreamSource templateInputStreamSource;
        private final Map<String, Map<String, SubsystemConfig>> subsystems;
        private final String outputFile;

        public ConfigFile(String outputFile) {
            this.subsystems = new HashMap<>();
            this.outputFile = outputFile;
        }

        public InputStreamSource getTemplateInputStreamSource() {
            return templateInputStreamSource;
        }

        public void setTemplateInputStreamSource(InputStreamSource templateInputStreamSource) {
            this.templateInputStreamSource = templateInputStreamSource;
        }

        public Map<String, Map<String, SubsystemConfig>> getSubsystems() {
            return subsystems;
        }

        public String getOutputFile() {
            return outputFile;
        }

    }
}
