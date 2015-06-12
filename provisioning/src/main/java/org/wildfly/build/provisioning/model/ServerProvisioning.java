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
        private final Map<String, ConfigFile> hostConfigFiles = new HashMap<>();

        public Map<String, ConfigFile> getStandaloneConfigFiles() {
            return standaloneConfigFiles;
        }

        public Map<String, ConfigFile> getDomainConfigFiles() {
            return domainConfigFiles;
        }

        public ZipFileSubsystemInputStreamSources getInputStreamSources() {
            return inputStreamSources;
        }

        public Map<String, ConfigFile> getHostConfigFiles() {
            return hostConfigFiles;
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
