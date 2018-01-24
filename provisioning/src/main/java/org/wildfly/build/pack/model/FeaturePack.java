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

package org.wildfly.build.pack.model;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.common.model.ConfigFile;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ModuleParser;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.ZipFileSubsystemInputStreamSources;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Represents a Wildfly feature pack. This is used by both the build and provisioning tools,
 * to represent the contents of a zipped up feature pack.
 *
 * This class is immutable.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class FeaturePack {

    private final File featurePackFile;
    private final Artifact featurePackArtifact;
    private final FeaturePackDescription description;
    private final List<String> configurationFiles;
    private final SortedSet<String> modulesFiles;
    private final List<String> contentFiles;
    private final List<FeaturePack> dependencies;
    private final ModuleParser moduleParser;
    private final ArtifactResolver artifactResolver;

    public FeaturePack(File featurePackFile, Artifact featurePackArtifact, FeaturePackDescription description, List<FeaturePack> dependencies, PropertyResolver propertyResolver, ArtifactResolver artifactResolver, List<String> configurationFiles, List<String> modulesFiles, List<String> contentFiles) {
        this.featurePackFile = featurePackFile;
        this.featurePackArtifact = featurePackArtifact;
        this.description = description;
        this.dependencies = dependencies;
        this.artifactResolver = artifactResolver;
        this.configurationFiles = Collections.unmodifiableList(configurationFiles);
        this.modulesFiles = Collections.unmodifiableSortedSet(new TreeSet<String>(modulesFiles));
        this.contentFiles = Collections.unmodifiableList(contentFiles);
        this.moduleParser = new ModuleParser(propertyResolver);
    }

    public FeaturePackDescription getDescription() {
        return description;
    }

    public File getFeaturePackFile() {
        return featurePackFile;
    }

    public Artifact getArtifact() {
        return featurePackArtifact;
    }

    public List<FeaturePack> getDependencies() {
        return dependencies;
    }

    public ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    public List<String> getConfigurationFiles() {
        return configurationFiles;
    }

    public SortedSet<String> getModulesFiles() {
        return modulesFiles;
    }

    public List<String> getContentFiles() {
        return contentFiles;
    }

    private Map<ModuleIdentifier, Module> featurePackModules;

    private Map<ModuleIdentifier, Module> featurePackAndDependenciesModules;

    private static final String MODULE_XML_ENTRY_NAME_SUFIX = "/module.xml";

    public synchronized Map<ModuleIdentifier, Module> getFeaturePackModules() {
        if (featurePackModules == null) {
            featurePackModules = new HashMap<>();
            try (JarFile jar = new JarFile(featurePackFile)) {
                // collect modules from entries named */module.xml
                for (String moduleFile : modulesFiles) {
                    if (moduleFile.endsWith(MODULE_XML_ENTRY_NAME_SUFIX)) {
                        ZipEntry entry = jar.getEntry(moduleFile);
                        // parse the module file
                        ModuleParseResult moduleParseResult = moduleParser.parse(jar.getInputStream(entry));
                        featurePackModules.put(moduleParseResult.getIdentifier(), new Module(this, moduleFile, moduleParseResult));
                    }
                }
            } catch (Throwable e) {
                throw new RuntimeException("Failed to parse feature pack modules from " + featurePackFile, e);
            }
            featurePackModules = Collections.unmodifiableMap(featurePackModules);
        }
        return featurePackModules;
    }

    public synchronized Map<ModuleIdentifier, Module> getFeaturePackAndDependenciesModules() {
        if (featurePackAndDependenciesModules == null) {
            featurePackAndDependenciesModules = new HashMap<>(getFeaturePackModules());
            for (FeaturePack dependency : dependencies) {
                for (Map.Entry<ModuleIdentifier, Module> entry : dependency.getFeaturePackAndDependenciesModules().entrySet()) {
                    if (!featurePackAndDependenciesModules.containsKey(entry.getKey())) {
                        featurePackAndDependenciesModules.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            featurePackAndDependenciesModules = Collections.unmodifiableMap(featurePackAndDependenciesModules);
        }
        return featurePackAndDependenciesModules;
    }

    public Module getSubsystemModule(String subsystem, ArtifactFileResolver artifactFileResolver) throws IOException {
        ZipFileSubsystemInputStreamSources inputStreamSources = new ZipFileSubsystemInputStreamSources();
        for(Module module : getFeaturePackAndDependenciesModules().values()) {
            if (inputStreamSources.addSubsystemFileSourceFromModule(subsystem, module, artifactFileResolver)) {
                // module has the subsystem config file
                return module;
            }
        }
        return null;
    }

    /**
     * Retrieves all subsystems included in the feature pack config files.
     * @return
     * @throws IOException
     * @throws XMLStreamException
     */
    public Set<String> getSubsystems() throws IOException, XMLStreamException {
        final Set<String> result = new HashSet<>();
        for (ConfigFile configFile : description.getConfig().getDomainConfigFiles()) {
            for (Map<String, SubsystemConfig> subsystems : configFile.getSubsystemConfigs(featurePackFile).values()) {
                result.addAll(subsystems.keySet());
            }
        }
        for (ConfigFile configFile : description.getConfig().getStandaloneConfigFiles()) {
            for (Map<String, SubsystemConfig> subsystems : configFile.getSubsystemConfigs(featurePackFile).values()) {
                result.addAll(subsystems.keySet());
            }
        }
        for (ConfigFile configFile : description.getConfig().getHostConfigFiles()) {
            for (Map<String, SubsystemConfig> subsystems : configFile.getSubsystemConfigs(featurePackFile).values()) {
                result.addAll(subsystems.keySet());
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeaturePack that = (FeaturePack) o;

        if (!featurePackFile.equals(that.featurePackFile)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return featurePackFile.hashCode();
    }

    public static class Module {

        private final FeaturePack featurePack;
        private final String moduleFile;
        private final ModuleParseResult moduleParseResult;

        private Module(FeaturePack featurePack, String moduleFile, ModuleParseResult moduleParseResult) {
            this.featurePack = featurePack;
            this.moduleFile = moduleFile;
            this.moduleParseResult = moduleParseResult;
        }

        public FeaturePack getFeaturePack() {
            return featurePack;
        }

        public String getModuleFile() {
            return moduleFile;
        }

        /**
         * Retrieves all files in module dir except the module dir and module xml file.
         * @return
         */
        public List<String> getModuleDirFiles() {
            final String moduleDir = moduleFile.substring(0, moduleFile.length() - MODULE_XML_ENTRY_NAME_SUFIX.length() + 1);
            List<String> moduleDirFiles = new ArrayList<>();
            for (String s : featurePack.modulesFiles.tailSet(moduleDir)) {
                if (s.startsWith(moduleDir)) {
                    if (s.length() > moduleDir.length() && !s.equals(moduleFile)) {
                        moduleDirFiles.add(s);
                    }
                } else {
                    break;
                }
            }
            return moduleDirFiles;
        }

        public ModuleParseResult getModuleParseResult() {
            return moduleParseResult;
        }

        public ModuleIdentifier getIdentifier() {
            return moduleParseResult.getIdentifier();
        }

        /**
         * Retrieves the full set of modules which the module directly and indirectly depends.
         * @return
         */
        public Map<ModuleIdentifier, Module> getDependencies() {
            Map<ModuleIdentifier, Module> featurePackAndDependenciesModules = featurePack.getFeaturePackAndDependenciesModules();
            Map<ModuleIdentifier, Module> result = new HashMap<>();
            Deque<ModuleParseResult.ModuleDependency> remaining = new ArrayDeque<>(moduleParseResult.getDependencies());
            while (!remaining.isEmpty()) {
                ModuleParseResult.ModuleDependency moduleDependency = remaining.pop();
                ModuleIdentifier moduleIdentifier = moduleDependency.getModuleId();
                Module module = featurePackAndDependenciesModules.get(moduleIdentifier);
                if (module == null) {
                    if (!moduleDependency.isOptional()) {
                        throw new IllegalStateException("Module " + moduleIdentifier + " not found in feature pack " + featurePack + " and dependencies");
                    }
                } else {
                    if (!result.containsKey(moduleIdentifier)) {
                        result.put(moduleIdentifier, module);
                        remaining.addAll(module.getModuleParseResult().getDependencies());
                    }
                }
            }
            return result;
        }
    }
}
