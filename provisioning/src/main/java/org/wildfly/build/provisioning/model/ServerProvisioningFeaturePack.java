package org.wildfly.build.provisioning.model;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.util.ZipFileSubsystemInputStreamSources;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Martins
 */
public class ServerProvisioningFeaturePack {

    /**
     * the feature pack description, in the context of server provisioning
     */
    private final ServerProvisioningDescription.FeaturePack description;

    /**
     * the feature pack
     */
    private final FeaturePack featurePack;

    /**
     * the standalone config files
     */
    private final List<ConfigFile> standaloneConfigFiles;

    /**
     * the domain config files
     */
    private final List<ConfigFile> domainConfigFiles;

    /**
     * the host config files
     */
    private final List<ConfigFile> hostConfigFiles;

    /**
     * the artifact file resolver
     */
    private final ArtifactFileResolver artifactFileResolver;

    /**
     *
     * @param description
     * @param featurePack
     * @param artifactFileResolver
     * @throws IOException
     * @throws XMLStreamException
     */
    public ServerProvisioningFeaturePack(ServerProvisioningDescription.FeaturePack description, FeaturePack featurePack, ArtifactFileResolver artifactFileResolver) throws IOException, XMLStreamException {
        this.description = description;
        this.featurePack = featurePack;
        this.artifactFileResolver = artifactFileResolver;
        ConfigOverride configOverride = getConfigOverride(featurePack, description, artifactFileResolver);
        this.standaloneConfigFiles = createStandaloneConfigFiles(featurePack, configOverride);
        this.domainConfigFiles = createDomainConfigFiles(featurePack, configOverride);
        this.hostConfigFiles = createHostConfigFiles(featurePack, configOverride);
    }

    /**
     * Retrieves the feature pack's provisioning description.
     * @return
     */
    public ServerProvisioningDescription.FeaturePack getDescription() {
        return description;
    }

    /**
     * Retrieves the feature pack
     * @return
     */
    public FeaturePack getFeaturePack() {
        return featurePack;
    }

    /**
     * Retrieves the collection of modules to include from the feature pack, considering the server provisioning description module filtering:
     *
     * 1) module filtering --> modules accepted by filters only
     * 2) no module filtering + config/subsystems filtering --> only the specified config's subsystems, and dependencies
     * 3) no module filtering + no config filtering --> all modules
     *
     * @return
     */
    public Map<ModuleIdentifier, FeaturePack.Module> getModules(ArtifactFileResolver artifactFileResolver) throws IOException, XMLStreamException {
        ServerProvisioningDescription.FeaturePack.ModuleFilters moduleFilters = description.getModuleFilters();
        final Map<ModuleIdentifier, FeaturePack.Module> includedModules = new HashMap<>();
        if (moduleFilters == null) {
            // no module filtering, include all modules if no config override selecting specific subsystems, otherwise include only modules needed by subsystems
            Set<String> subsystems = null;
            if (description.getConfigOverride() != null || description.getSubsystems() != null) {
                subsystems = new HashSet<>();
                for (ConfigFile configFile : getStandaloneConfigFiles()) {
                    for (Map<String, SubsystemConfig> subsystemConfigMap : configFile.getSubsystems().values()) {
                        for (String subsystem : subsystemConfigMap.keySet()) {
                            subsystems.add(subsystem);
                        }
                    }
                }
                for (ConfigFile configFile : getDomainConfigFiles()) {
                    for (Map<String, SubsystemConfig> subsystemConfigMap : configFile.getSubsystems().values()) {
                        for (String subsystem : subsystemConfigMap.keySet()) {
                            subsystems.add(subsystem);
                        }
                    }
                }
                for (ConfigFile configFile : getHostConfigFiles()) {
                    for (Map<String, SubsystemConfig> subsystemConfigMap : configFile.getSubsystems().values()) {
                        for (String subsystem : subsystemConfigMap.keySet()) {
                            subsystems.add(subsystem);
                        }
                    }
                }
            }
            if (subsystems == null) {
                // and no subsystems filtered, include all modules
                includedModules.putAll(featurePack.getFeaturePackAndDependenciesModules());
            } else {
                // subsystems filtered, include subsystems module's and all transitive dependencies
                for (String subsystem : subsystems) {
                    FeaturePack.Module module = featurePack.getSubsystemModule(subsystem, artifactFileResolver);
                    if (module == null) {
                        throw new RuntimeException("Subsystem "+subsystem+" module not found in feature pack "+featurePack.getFeaturePackFile()+ " and dependencies");
                    }
                    includedModules.put(module.getModuleParseResult().getIdentifier(), module);
                    includedModules.putAll(module.getDependencies());
                }
            }
        } else {
            // modules filtered
            for (FeaturePack.Module module : featurePack.getFeaturePackModules().values()) {
                boolean include = moduleFilters.isInclude();
                // by default module's dependencies are included
                boolean transitive = true;
                for (ModuleFilter moduleFilter : moduleFilters.getFilters()) {
                    String moduleFile = module.getModuleFile().substring(Locations.MODULES.length() + 1);
                    if (moduleFilter.matches(moduleFile)) {
                        if (moduleFilter.isInclude()) {
                            if (!moduleFilter.isTransitive()) {
                                transitive = false;
                            }
                        } else {
                            include = false;
                        }
                        break;
                    }
                }
                if (include) {
                    includedModules.put(module.getIdentifier(), module);
                    if (transitive) {
                        includedModules.putAll(module.getDependencies());
                    }
                }
            }
        }
        return includedModules;
    }

    /**
     *
     * @return the list of domain {@link org.wildfly.build.provisioning.model.ServerProvisioningFeaturePack.ConfigFile}
     */
    public List<ConfigFile> getDomainConfigFiles() {
        return domainConfigFiles;
    }

    /**
     *
     * @return the list of standalone {@link org.wildfly.build.provisioning.model.ServerProvisioningFeaturePack.ConfigFile}
     */
    public List<ConfigFile> getStandaloneConfigFiles() {
        return standaloneConfigFiles;
    }

    public List<ConfigFile> getHostConfigFiles() {
        return hostConfigFiles;
    }

    /**
     * Retrieves the {@link org.wildfly.build.common.model.ConfigOverride} to use when provisioning the feature pack.
     *
     * If the description includes high level subsystems filtering, instead of config override, this method will create a {@link org.wildfly.build.common.model.ConfigOverride}, containing a {@link org.wildfly.build.common.model.ConfigFileOverride} for each feature pack {@link org.wildfly.build.common.model.ConfigFile}, containing only the required subsystem configs.
     * @param featurePack
     * @param featurePackProvisioningDescription
     * @param artifactFileResolver
     * @return
     * @throws IOException
     * @throws XMLStreamException
     */
    private static ConfigOverride getConfigOverride(FeaturePack featurePack, ServerProvisioningDescription.FeaturePack featurePackProvisioningDescription, ArtifactFileResolver artifactFileResolver) throws IOException, XMLStreamException {
        ConfigOverride configOverride = featurePackProvisioningDescription.getConfigOverride();
        if (configOverride == null) {
            // no config override in description
            List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = featurePackProvisioningDescription.getSubsystems();
            if (subsystems != null) {
                // subsystems defined in description, transform into config override
                configOverride = new ConfigOverride();
                // 1. collect all subsystem templates from each subsystem module
                ZipFileSubsystemInputStreamSources subsystemInputStreamSources = new ZipFileSubsystemInputStreamSources();
                for (ServerProvisioningDescription.FeaturePack.Subsystem subsystem : subsystems) {
                    final String subsystemName = subsystem.getName().endsWith(".xml") ? subsystem.getName() : subsystem.getName() + ".xml";
                    FeaturePack.Module module = featurePack.getSubsystemModule(subsystemName, artifactFileResolver);
                    if (module == null) {
                        throw new RuntimeException("Subsystem " + subsystemName + " module not found in feature pack " + featurePack.getFeaturePackFile() + " and dependencies");
                    }
                    if (subsystem.isTransitive()) {
                        subsystemInputStreamSources.addAllSubsystemFileSourcesFromModule(module, artifactFileResolver, true);
                    } else {
                        subsystemInputStreamSources.addSubsystemFileSourceFromModule(subsystemName, module, artifactFileResolver);
                    }
                }
                // 2. create config file override for each feature pack config file
                createConfigFileOverridesFromSubsystems(featurePack.getFeaturePackFile(), featurePack.getDescription().getConfig().getStandaloneConfigFiles(), subsystemInputStreamSources, configOverride.getStandaloneConfigFiles());
                createConfigFileOverridesFromSubsystems(featurePack.getFeaturePackFile(), featurePack.getDescription().getConfig().getDomainConfigFiles(), subsystemInputStreamSources, configOverride.getDomainConfigFiles());
                createConfigFileOverridesFromSubsystems(featurePack.getFeaturePackFile(), featurePack.getDescription().getConfig().getHostConfigFiles(), subsystemInputStreamSources, configOverride.getHostConfigFiles());
            }
        }
        return configOverride;
    }

    /**
     * Creates a {@link org.wildfly.build.common.model.ConfigFileOverride} for each {@link org.wildfly.build.common.model.ConfigFile} provided, including only the subsystems in the specified {@link org.wildfly.build.util.ZipFileSubsystemInputStreamSources}.
     * @param featurePackFile
     * @param configFiles
     * @param subsystemInputStreamSources
     * @param configFileOverrides
     * @throws IOException
     * @throws XMLStreamException
     */
    private static void createConfigFileOverridesFromSubsystems(File featurePackFile, List<org.wildfly.build.common.model.ConfigFile> configFiles, ZipFileSubsystemInputStreamSources subsystemInputStreamSources, Map<String, ConfigFileOverride> configFileOverrides) throws IOException, XMLStreamException {
        for (org.wildfly.build.common.model.ConfigFile configFile : configFiles) {
            // parse subsystems
            Map<String, Map<String, SubsystemConfig>> subsystems = configFile.getSubsystemConfigs(featurePackFile);
            // remove the subsystems which templates were not found in the subsystem modules
            Iterator<Map<String, SubsystemConfig>> subsystemsIterator = subsystems.values().iterator();
            while (subsystemsIterator.hasNext()) {
                Map<String, SubsystemConfig> subsystemConfigs = subsystemsIterator.next();
                Iterator<String> subsystemConfigsIterator = subsystemConfigs.keySet().iterator();
                while (subsystemConfigsIterator.hasNext()) {
                    String subsystemConfig = subsystemConfigsIterator.next();
                    if (subsystemInputStreamSources.getInputStreamSource(subsystemConfig) == null) {
                        subsystemConfigsIterator.remove();
                    }
                }
                if (subsystemConfigs.isEmpty()) {
                    subsystemsIterator.remove();
                }
            }
            if (subsystems.isEmpty()) {
                // skip config files without subsystems
                continue;
            }
            configFileOverrides.put(configFile.getOutputFile(), new ConfigFileOverride(configFile.getProperties(), false, subsystems, configFile.getOutputFile()));
        }
    }

    /**
     * Creates the provisioning standalone config files.
     * @param featurePack
     * @param configOverride
     * @return
     */
    private static List<ConfigFile> createStandaloneConfigFiles(FeaturePack featurePack, ConfigOverride configOverride) {
        final List<org.wildfly.build.common.model.ConfigFile> configFiles = featurePack.getDescription().getConfig().getStandaloneConfigFiles();
        final Map<String, ConfigFileOverride> configFileOverrides = configOverride != null ? configOverride.getStandaloneConfigFiles() : null;
        return createConfigFiles(featurePack.getFeaturePackFile(), configFiles, configOverride, configFileOverrides);
    }


    /**
     * Creates the provisioning standalone config files.
     * @param featurePack
     * @param configOverride
     * @return
     */
    private static List<ConfigFile> createHostConfigFiles(FeaturePack featurePack, ConfigOverride configOverride) {
        final List<org.wildfly.build.common.model.ConfigFile> configFiles = featurePack.getDescription().getConfig().getHostConfigFiles();
        final Map<String, ConfigFileOverride> configFileOverrides = configOverride != null ? configOverride.getHostConfigFiles() : null;
        return createConfigFiles(featurePack.getFeaturePackFile(), configFiles, configOverride, configFileOverrides);
    }

    /**
     * Creates the provisioning domain config files.
     * @param featurePack
     * @param configOverride
     * @return
     */
    private static List<ConfigFile> createDomainConfigFiles(FeaturePack featurePack, ConfigOverride configOverride) {
        final List<org.wildfly.build.common.model.ConfigFile> configFiles = featurePack.getDescription().getConfig().getDomainConfigFiles();
        final Map<String, ConfigFileOverride> configFileOverrides = configOverride != null ? configOverride.getDomainConfigFiles() : null;
        return createConfigFiles(featurePack.getFeaturePackFile(), configFiles, configOverride, configFileOverrides);
    }

    /**
     * Creates a provisioning config file for each {@link org.wildfly.build.common.model.ConfigFile} provided.
     * @param featurePackFile
     * @param configFiles
     * @param configOverride
     * @param configFileOverrides
     * @return
     */
    private static List<ConfigFile> createConfigFiles(File featurePackFile, List<org.wildfly.build.common.model.ConfigFile> configFiles, ConfigOverride configOverride, Map<String, ConfigFileOverride> configFileOverrides) {
        final List<ConfigFile> result = new ArrayList<>();
        if (configOverride != null) {
            if (configFileOverrides != null && !configFileOverrides.isEmpty()) {
                for (org.wildfly.build.common.model.ConfigFile featurePackConfigFile : configFiles) {
                    ConfigFileOverride configFileOverride = configFileOverrides.get(featurePackConfigFile.getOutputFile());
                    if (configFileOverride != null) {
                        result.add(new ConfigFile(featurePackFile, featurePackConfigFile, configFileOverride));
                    }
                }
            }
        } else {
            for (org.wildfly.build.common.model.ConfigFile featurePackConfigFile : configFiles) {
                result.add(new ConfigFile(featurePackFile, featurePackConfigFile, null));
            }
        }
        return result;
    }

    /**
     * The provisioning config file
     */
    public static class ConfigFile {

        private final File featurePackFile;
        private final org.wildfly.build.common.model.ConfigFile featurePackConfigFile;
        private final ConfigFileOverride configFileOverride;
        private Map<String, Map<String, SubsystemConfig>> subsystems;

        public synchronized Map<String, Map<String, SubsystemConfig>> getSubsystems() throws IOException, XMLStreamException {
            if (subsystems == null) {
                if (configFileOverride == null || configFileOverride.getSubsystems() == null) {
                    // parse the feature pack's config subsystems file and include all
                    subsystems = featurePackConfigFile.getSubsystemConfigs(featurePackFile);
                } else {
                    subsystems = configFileOverride.getSubsystems();
                }
            }
            return subsystems;
        }

        public ConfigFile(File featurePackFile, org.wildfly.build.common.model.ConfigFile featurePackConfigFile, ConfigFileOverride configFileOverride) {
            this.featurePackFile = featurePackFile;
            this.featurePackConfigFile = featurePackConfigFile;
            this.configFileOverride = configFileOverride;
        }

        public org.wildfly.build.common.model.ConfigFile getFeaturePackConfigFile() {
            return featurePackConfigFile;
        }

        public ConfigFileOverride getConfigFileOverride() {
            return configFileOverride;
        }

    }
}