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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ModuleParser;

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
    private final List<String> modulesFiles;
    private final List<String> contentFiles;
    private final List<FeaturePack> dependencies;
    private final ArtifactResolver artifactResolver;

    public FeaturePack(File featurePackFile, Artifact featurePackArtifact, FeaturePackDescription description, List<FeaturePack> dependencies, ArtifactResolver artifactResolver, List<String> configurationFiles, List<String> modulesFiles, List<String> contentFiles) {
        this.featurePackFile = featurePackFile;
        this.featurePackArtifact = featurePackArtifact;
        this.description = description;
        this.dependencies = dependencies;
        this.artifactResolver = artifactResolver;
        this.configurationFiles = Collections.unmodifiableList(configurationFiles);
        this.modulesFiles = Collections.unmodifiableList(modulesFiles);
        this.contentFiles = Collections.unmodifiableList(contentFiles);
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

    public List<String> getModulesFiles() {
        return modulesFiles;
    }

    public List<String> getContentFiles() {
        return contentFiles;
    }

    private Map<ModuleIdentifier, ModuleParseResult> modules;

    private static final String MODULE_XML_ENTRY_NAME_SUFIX = "/module.xml";

    public synchronized Map<ModuleIdentifier, ModuleParseResult> getModules() {
        if (modules == null) {
            modules = new HashMap<>();
            try (JarFile jar = new JarFile(featurePackFile)) {
                for (String moduleFile : modulesFiles) {
                    if (moduleFile.endsWith(MODULE_XML_ENTRY_NAME_SUFIX)) {
                        ZipEntry entry = jar.getEntry(moduleFile);
                        ModuleParseResult moduleParseResult = ModuleParser.parse(jar.getInputStream(entry));
                        modules.put(moduleParseResult.getIdentifier(), moduleParseResult);
                    }
                }
            } catch (Throwable e) {
                throw new RuntimeException("Failed to parse feature pack modules from " + featurePackFile, e);
            }
            modules = Collections.unmodifiableMap(modules);
        }
        return modules;
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
}
