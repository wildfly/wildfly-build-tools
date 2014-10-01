package org.wildfly.build.provisioning.model;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.configassembly.SubsystemsParser;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.util.InputStreamSource;
import org.wildfly.build.util.ZipEntryInputStreamSource;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
     *
     * @param description
     * @param featurePack
     */
    public ServerProvisioningFeaturePack(ServerProvisioningDescription.FeaturePack description, FeaturePack featurePack) {
        this.description = description;
        this.featurePack = featurePack;
        this.standaloneConfigFiles = initStandaloneConfigFiles(featurePack, description.getConfigOverride());
        this.domainConfigFiles = initDomainConfigFiles(featurePack, description.getConfigOverride());
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
     * 1) no module filtering + no config filtering --> all modules
     * 2) module filtering --> modules accepted by filters only, but note that by default a module not matched by any filter will be included, same for its dependencies
     * 3) no module filtering + config filtering --> only the specified config's subsystems, and dependencies
     *
     * @return
     */
    public Map<ModuleIdentifier, FeaturePack.Module> getModules(ArtifactFileResolver artifactFileResolver) throws IOException, XMLStreamException {
        List<ModuleFilter> moduleFilters = description.getModuleFilters();
        final Map<ModuleIdentifier, FeaturePack.Module> includedModules = new HashMap<>();
        if (moduleFilters == null || moduleFilters.isEmpty()) {
            // no module filtering
            Set<String> subsystems = null;
            if (description.getConfigOverride() != null) {
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
                // by default a module and its dependencies are included
                boolean include = true;
                boolean transitive = true;
                for (ModuleFilter moduleFilter : moduleFilters) {
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

    public List<ConfigFile> getDomainConfigFiles() {
        return domainConfigFiles;
    }

    public List<ConfigFile> getStandaloneConfigFiles() {
        return standaloneConfigFiles;
    }

    private static List<ConfigFile> initStandaloneConfigFiles(FeaturePack featurePack, ConfigOverride configOverride) {
        final List<org.wildfly.build.common.model.ConfigFile> configFiles = featurePack.getDescription().getConfig().getStandaloneConfigFiles();
        final Map<String, ConfigFileOverride> configFileOverrides = configOverride != null ? configOverride.getStandaloneConfigFiles() : null;
        return getConfigFiles(featurePack.getFeaturePackFile(), configFiles, configOverride, configFileOverrides);
    }

    private static List<ConfigFile> initDomainConfigFiles(FeaturePack featurePack, ConfigOverride configOverride) {
        final List<org.wildfly.build.common.model.ConfigFile> configFiles = featurePack.getDescription().getConfig().getDomainConfigFiles();
        final Map<String, ConfigFileOverride> configFileOverrides = configOverride != null ? configOverride.getDomainConfigFiles() : null;
        return getConfigFiles(featurePack.getFeaturePackFile(), configFiles, configOverride, configFileOverrides);
    }

    private static List<ConfigFile> getConfigFiles(File featurePackFile, List<org.wildfly.build.common.model.ConfigFile> configFiles, ConfigOverride configOverride, Map<String, ConfigFileOverride> configFileOverrides) {
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

    public static class ConfigFile {

        private final File featurePackFile;
        private final org.wildfly.build.common.model.ConfigFile featurePackConfigFile;
        private final ConfigFileOverride configFileOverride;
        private Map<String, Map<String, SubsystemConfig>> subsystems;

        public synchronized Map<String, Map<String, SubsystemConfig>> getSubsystems() throws IOException, XMLStreamException {
            if (subsystems == null) {
                if (configFileOverride == null || configFileOverride.getSubsystems() == null) {
                    // parse the feature pack's config subsystems file and include all
                    subsystems = new HashMap<>();
                    try (ZipFile zip = new ZipFile(featurePackFile)) {
                        ZipEntry zipEntry = zip.getEntry(featurePackConfigFile.getSubsystems());
                        if (zipEntry == null) {
                            throw new RuntimeException("Feature pack " + featurePackFile + " subsystems file " + featurePackConfigFile.getSubsystems() + " not found");
                        }
                        InputStreamSource inputStreamSource = new ZipEntryInputStreamSource(featurePackFile, zipEntry);
                        SubsystemsParser.parse(inputStreamSource, featurePackConfigFile.getProperties(), subsystems);
                    }
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