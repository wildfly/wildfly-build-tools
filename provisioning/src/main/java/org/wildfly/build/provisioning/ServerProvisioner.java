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

package org.wildfly.build.provisioning;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import org.jboss.logging.Logger;
import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.common.model.ConfigFile;
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.common.model.FilePermission;
import org.wildfly.build.configassembly.ConfigurationAssembler;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.FeaturePackFactory;
import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.provisioning.model.ServerProvisioning;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningFeaturePack;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.FileUtils;
import org.wildfly.build.util.ModuleArtifactPropertyResolver;
import org.wildfly.build.util.ModuleParseResult;
import org.wildfly.build.util.ZipEntryInputStreamSource;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Task that builds a server from a set of features packs declared in the pack.
 *
 * @author Eduardo Martins
 */
public class ServerProvisioner {

    private static final Logger logger = Logger.getLogger(ServerProvisioner.class);

    private static final String SUBSYSTEM_SCHEMA_TARGET_DIRECTORY = "docs" + File.separator + "schema";

    private static final boolean OS_WINDOWS = System.getProperty("os.name").contains("indows");

    private final ServerProvisioningDescription description;

    private final File outputDirectory;

    private final ArtifactFileResolver artifactFileResolver;

    private final ArtifactResolver versionOverrideArtifactResolver;

    private final boolean overlay;

    public ServerProvisioner(ServerProvisioningDescription description, File outputDirectory, boolean overlay, ArtifactFileResolver artifactFileResolver, ArtifactResolver versionOverrideArtifactResolver) {
        this.description = description;
        this.outputDirectory = outputDirectory;
        this.overlay = overlay;
        this.artifactFileResolver = artifactFileResolver;
        this.versionOverrideArtifactResolver = versionOverrideArtifactResolver;
    }

    public void build() {
        final ServerProvisioning serverProvisioning = new ServerProvisioning(description);
        final List<String> errors = new ArrayList<>();
        try {
            // create the feature packs
            for (ServerProvisioningDescription.FeaturePack serverProvisioningFeaturePackDescription : description.getFeaturePacks()) {
                final FeaturePack featurePack = FeaturePackFactory.createPack(serverProvisioningFeaturePackDescription.getArtifact(), artifactFileResolver, versionOverrideArtifactResolver);
                serverProvisioning.getFeaturePacks().add(new ServerProvisioningFeaturePack(serverProvisioningFeaturePackDescription, featurePack, artifactFileResolver));
            }
            // create output dir
            FileUtils.deleteRecursive(outputDirectory);
            outputDirectory.mkdirs();
            // create schema output dir if needed
            final File schemaOutputDirectory;
            if (description.isExtractSchemas()) {
                schemaOutputDirectory = new File(outputDirectory, SUBSYSTEM_SCHEMA_TARGET_DIRECTORY);
                if (!schemaOutputDirectory.exists()) {
                    schemaOutputDirectory.mkdirs();
                }
            } else {
                schemaOutputDirectory = null;
            }
            final Set<String> filesProcessed = new HashSet<>();
            // process server provisioning copy-artifacts
            processCopyArtifacts(serverProvisioning.getDescription().getCopyArtifacts(), versionOverrideArtifactResolver, outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
            // process modules (needs to be done for all feature packs before any config is processed, due to subsystem template gathering)
            processModules(serverProvisioning, outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);

            // process everything else for each feature pack
            for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
                if ( ! overlay ) {
                    processSubsystemConfigInFeaturePack(provisioningFeaturePack, serverProvisioning, artifactFileResolver);
                }
                processFeaturePackCopyArtifacts(provisioningFeaturePack.getFeaturePack(), outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory, overlay || description.isExcludeDependencies());
                processProvisioningFeaturePackContents(provisioningFeaturePack, outputDirectory, filesProcessed, overlay || description.isExcludeDependencies());
                processFeaturePackFilePermissions(provisioningFeaturePack.getFeaturePack(), outputDirectory, overlay || description.isExcludeDependencies());
            }
            // process the server config
            if ( ! overlay ) {
                processConfig(serverProvisioning, outputDirectory, filesProcessed);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Some errors were encountered creating the feature pack\n");
                for (String error : errors) {
                    sb.append(error);
                    sb.append("\n");
                }
                throw new RuntimeException(sb.toString());
            }
        }

    }

    private void processSubsystemConfigInFeaturePack(ServerProvisioningFeaturePack provisioningFeaturePack, ServerProvisioning serverProvisioning, ArtifactFileResolver artifactFileResolver) throws IOException {

        File artifactFile = artifactFileResolver.getArtifactFile(provisioningFeaturePack.getFeaturePack().getArtifact());
        // features packs themselves can contain a 'subsystem-templates' directory. Templates in the feature pack override ones from modules
        serverProvisioning.getConfig().getInputStreamSources().addAllSubsystemFileSourcesFromZipFile(artifactFile);
    }

    public static void build(ServerProvisioningDescription description, File outputDirectory, boolean overlay, ArtifactFileResolver artifactFileResolver, ArtifactResolver versionOverrideArtifactResolver) {
        ServerProvisioner provisioner = new ServerProvisioner(description, outputDirectory, overlay, artifactFileResolver, versionOverrideArtifactResolver);
        provisioner.build();
    }

    private void processCopyArtifacts(List<CopyArtifact> copyArtifacts, ArtifactResolver artifactResolver, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException {
        Set<String> filesProcessedThisPack = new HashSet<>();
        for (CopyArtifact copyArtifact : copyArtifacts) {

            //first resolve the artifact
            Artifact artifact = copyArtifact.getArtifact(artifactResolver);
            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
            if (artifactFile == null) {
                throw new RuntimeException("Could not resolve file for artifact " + copyArtifact.getArtifact() + " to copy");
            }

            String location = copyArtifact.getToLocation();
            if (location.endsWith("/")) {
                //if the to location ends with a / then it is a directory
                //so we need to append the artifact name
                location += artifactFile.getName();
            }

            if (filesProcessed.contains(location)) {
                continue;
            }
            filesProcessedThisPack.add(location);
            File target = new File(outputDirectory, location);
            if (!target.getParentFile().isDirectory()) {
                if (!target.getParentFile().mkdirs()) {
                    throw new IOException("Could not create directory " + target.getParentFile());
                }
            }
            if (copyArtifact.isExtract()) {
                extractArtifact(artifactFile, target, copyArtifact);
            } else {
                FileUtils.copyFile(artifactFile, target);
            }

            extractSchema(schemaOutputDirectory, artifact, artifactFile);
        }
        filesProcessed.addAll(filesProcessedThisPack);
    }

    private void extractSchema(File schemaOutputDirectory, Artifact artifact, File artifactFile) throws IOException {
        if (description.isExtractSchemas() && schemaOutputDirectory != null) {
            String groupId = artifact.getGroupId();
            // extract schemas, if any
            if (description.getExtractSchemasGroups().contains(groupId)) {
                logger.debugf("extracting schemas for artifact: '%s'", artifact);
                FileUtils.extractSchemas(artifactFile, schemaOutputDirectory);
            }
        }
    }


    private void processModules(ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException, XMLStreamException {
        // 1. gather the modules for each feature pack
        final Map<FeaturePack, List<FeaturePack.Module>> featurePackModulesMap = new HashMap<>();
        Set<ModuleIdentifier> moduleIdentifiers = new HashSet<>();
        for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
            getLog().debugf("Gathering modules for provisioning feature pack %s", provisioningFeaturePack.getFeaturePack().getFeaturePackFile());
            for (FeaturePack.Module module : provisioningFeaturePack.getModules(artifactFileResolver, overlay || serverProvisioning.getDescription().isExcludeDependencies()).values()) {
                final ModuleIdentifier moduleIdentifier = module.getIdentifier();
                if (moduleIdentifiers.add(moduleIdentifier)) {
                    getLog().debugf("Adding module %s from feature pack %s", moduleIdentifier, module.getFeaturePack().getFeaturePackFile());
                    List<FeaturePack.Module> featurePackModules = featurePackModulesMap.get(module.getFeaturePack());
                    if (featurePackModules == null) {
                        featurePackModules = new ArrayList<>();
                        featurePackModulesMap.put(module.getFeaturePack(), featurePackModules);
                    }
                    featurePackModules.add(module);
                } else {
                    getLog().debugf("Skipping %s from feature pack %s. A module with such identifier is already in the provisioning module set.", moduleIdentifier, module.getFeaturePack().getFeaturePackFile());
                }
            }
            //we always need to resolve all subsystem templates, regardless of the value of exclude-dependencies
            for (FeaturePack.Module module : provisioningFeaturePack.getModules(artifactFileResolver, false).values()) {
                for (ModuleParseResult.ArtifactName artifactName : module.getModuleParseResult().getArtifacts()) {
                    String options = artifactName.getOptions();
                    Artifact artifact;
                    if(artifactName.hasVersion()) {
                        artifact = artifactName.getArtifact();
                    } else {
                        artifact = module.getFeaturePack().getArtifactResolver().getArtifact(artifactName.getArtifact());
                    }
                    if (artifact == null) {
                        throw new RuntimeException("Could not resolve module resource artifact " + artifactName + " for feature pack " + module.getFeaturePack().getFeaturePackFile());
                    }
                    File artifactFile = artifactFileResolver.getArtifactFile(artifact);
                    // add all subsystem templates
                    serverProvisioning.getConfig().getInputStreamSources().addAllSubsystemFileSourcesFromZipFile(artifactFile);
                }
            }
        }
        // 2. provision each feature pack modules
        for (Map.Entry<FeaturePack, List<FeaturePack.Module>> mapEntry : featurePackModulesMap.entrySet()) {
            FeaturePack featurePack = mapEntry.getKey();
            List<FeaturePack.Module> includedModules = mapEntry.getValue();
            processFeaturePackModules(featurePack, includedModules, serverProvisioning, outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
        }
    }

    private void processFeaturePackModules(FeaturePack featurePack, List<FeaturePack.Module> includedModules, ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException {
        final boolean thinServer = !serverProvisioning.getDescription().isCopyModuleArtifacts();
        // create the module's artifact property replacer
        final BuildPropertyReplacer buildPropertyReplacer = thinServer ? new BuildPropertyReplacer(new ModuleArtifactPropertyResolver(featurePack.getArtifactResolver())) : null;
        // process each module file
        try (JarFile jar = new JarFile(featurePack.getFeaturePackFile())) {
            for (FeaturePack.Module module : includedModules) {
                // process the module file
                final String jarEntryName = module.getModuleFile();
                filesProcessed.add(jarEntryName);
                File targetFile = new File(outputDirectory, jarEntryName);
                // ensure parent dirs exist
                targetFile.getParentFile().mkdirs();
                // extract the module file
                FileUtils.extractFile(jar, jarEntryName, targetFile);
                // parse the module xml
                ModuleParseResult result = module.getModuleParseResult();
                // process module artifacts
                for (ModuleParseResult.ArtifactName artifactName : result.getArtifacts()) {
                    String options = artifactName.getOptions();
                    boolean jandex = false;
                    if (options != null) {
                        jandex = options.contains("jandex"); //todo: eventually we may need options to have a proper query string type syntax
                    }
                    Artifact artifact;
                    if(artifactName.hasVersion()) {
                        artifact = artifactName.getArtifact();
                    } else {
                        artifact = featurePack.getArtifactResolver().getArtifact(artifactName.getArtifact());
                    }
                    if (artifact == null) {
                        throw new RuntimeException("Could not resolve module resource artifact " + artifactName + " for feature pack " + featurePack.getFeaturePackFile());
                    }
                    try {
                        if (thinServer) {
                            // replace artifact coords properties with the ones expected by jboss-modules
                            String orig = artifactName.getAttribute().getValue();
                            if(orig.contains("?")) {
                                orig = orig.substring(0, orig.indexOf("?")) + "}";
                            }
                            if(!artifactName.hasVersion()) {
                                String repl = buildPropertyReplacer.replaceProperties(orig);
                                if (! repl.equals(orig)) {
                                    artifactName.getAttribute().setValue(repl);
                                }
                            } else {
                                artifactName.getAttribute().setValue(artifactName.getJBossModulesArtifactString());
                            }
                            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
                            // extract schemas if needed
                            extractSchema(schemaOutputDirectory, artifact, artifactFile);
                        } else {
                            // process the module artifact
                            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
                            // extract schemas if needed
                            extractSchema(schemaOutputDirectory, artifact, artifactFile);
                            String location;
                            if (jandex) {
                                String baseName = artifactFile.getName().substring(0, artifactFile.getName().lastIndexOf("."));
                                String extension = artifactFile.getName().substring(artifactFile.getName().lastIndexOf("."));
                                File target = new File(targetFile.getParent(), baseName + "-jandex" + extension);
                                JandexIndexer.createIndex(artifactFile, new FileOutputStream(target));
                                location = target.getName();
                            } else {
                                location = artifactFile.getName();
                                // copy the artifact
                                FileUtils.copyFile(artifactFile, new File(targetFile.getParent(), location));
                            }
                            // update module xml content
                            final Attribute attribute = artifactName.getAttribute();
                            final Element artifactNode = (Element) attribute.getParent();
                            artifactNode.setLocalName("resource-root");
                            attribute.setLocalName("path");
                            attribute.setValue(location);
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException("Could not extract resources from " + artifactName, t);
                    }
                }
                // update the version, if there is one
                final ModuleParseResult.ArtifactName versionArtifactName = result.getVersionArtifactName();
                if (versionArtifactName != null) {
                    Artifact artifact = featurePack.getArtifactResolver().getArtifact(versionArtifactName.getArtifact());
                    if (artifact == null) {
                        throw new RuntimeException("Could not resolve module resource artifact " + versionArtifactName + " for feature pack " + featurePack.getFeaturePackFile());
                    }
                    // set the resolved version
                    versionArtifactName.getAttribute().setValue(artifact.getVersion());
                }
                // write updated module xml content
                final Document document = result.getDocument();
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    new Serializer(out).write(document);
                }

                // extract all other files in the module dir
                for (String moduleDirFile : module.getModuleDirFiles()) {
                    filesProcessed.add(moduleDirFile);
                    FileUtils.extractFile(jar, moduleDirFile, new File(outputDirectory, moduleDirFile));
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to process feature pack " + featurePack.getFeaturePackFile() + " modules", e);
        }
    }

    private void processConfig(ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed) throws IOException, XMLStreamException {
        ServerProvisioning.Config provisioningConfig = serverProvisioning.getConfig();
        // 1. collect and merge each feature pack configs
        for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
            processFeaturePackConfig(provisioningFeaturePack, provisioningConfig);
        }
        // 2. assemble the merged configs
        for (ServerProvisioning.ConfigFile provisioningConfigFile : provisioningConfig.getDomainConfigFiles().values()) {
            if (provisioningConfigFile.getTemplateInputStreamSource() == null) {
                getLog().debugf("Skipping assembly of config file %s, template not set.", provisioningConfigFile.getOutputFile());
                continue;
            }
            getLog().debugf("Assembling config file %s", provisioningConfigFile.getOutputFile());
            filesProcessed.add(provisioningConfigFile.getOutputFile());
            new ConfigurationAssembler(provisioningConfig.getInputStreamSources(),
                                       provisioningConfigFile.getTemplateInputStreamSource(),
                                       "domain",
                                       provisioningConfigFile.getSubsystems(),
                                       new File(outputDirectory, provisioningConfigFile.getOutputFile()))
                    .assemble();
        }
        for (ServerProvisioning.ConfigFile provisioningConfigFile : provisioningConfig.getStandaloneConfigFiles().values()) {
            if (provisioningConfigFile.getTemplateInputStreamSource() == null) {
                getLog().debugf("Skipping assembly of config file %s, template not set.", provisioningConfigFile.getOutputFile());
                continue;
            }
            getLog().debugf("Assembling config file %s", provisioningConfigFile.getOutputFile());
            filesProcessed.add(provisioningConfigFile.getOutputFile());
            new ConfigurationAssembler(provisioningConfig.getInputStreamSources(),
                                       provisioningConfigFile.getTemplateInputStreamSource(),
                                       "server",
                                       provisioningConfigFile.getSubsystems(),
                                       new File(outputDirectory, provisioningConfigFile.getOutputFile()))
                    .assemble();
        }
        for (ServerProvisioning.ConfigFile provisioningConfigFile : provisioningConfig.getHostConfigFiles().values()) {
            if (provisioningConfigFile.getTemplateInputStreamSource() == null) {
                getLog().debugf("Skipping assembly of config file %s, template not set.", provisioningConfigFile.getOutputFile());
                continue;
            }
            getLog().debugf("Assembling config file %s", provisioningConfigFile.getOutputFile());
            filesProcessed.add(provisioningConfigFile.getOutputFile());
            new ConfigurationAssembler(provisioningConfig.getInputStreamSources(),
                                       provisioningConfigFile.getTemplateInputStreamSource(),
                                       "host",
                                       provisioningConfigFile.getSubsystems(),
                                       new File(outputDirectory, provisioningConfigFile.getOutputFile()))
                    .assemble();
        }
    }

    private void processFeaturePackConfig(ServerProvisioningFeaturePack provisioningFeaturePack, ServerProvisioning.Config provisioningConfig) throws IOException, XMLStreamException {
        FeaturePack featurePack = provisioningFeaturePack.getFeaturePack();
        getLog().debug("Processing provisioning feature pack " + featurePack.getFeaturePackFile() + " configs");
        try (ZipFile zipFile = new ZipFile(featurePack.getFeaturePackFile())) {
            for (ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile : provisioningFeaturePack.getDomainConfigFiles()) {
                processFeaturePackConfigFile(serverProvisioningFeaturePackConfigFile, zipFile, provisioningFeaturePack, provisioningConfig.getDomainConfigFiles());
            }
            for (ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile : provisioningFeaturePack.getStandaloneConfigFiles()) {
                processFeaturePackConfigFile(serverProvisioningFeaturePackConfigFile, zipFile, provisioningFeaturePack, provisioningConfig.getStandaloneConfigFiles());
            }
            for (ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile : provisioningFeaturePack.getHostConfigFiles()) {
                processFeaturePackConfigFile(serverProvisioningFeaturePackConfigFile, zipFile, provisioningFeaturePack, provisioningConfig.getHostConfigFiles());
            }
        }
    }

    private void processFeaturePackConfigFile(ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile, ZipFile zipFile, ServerProvisioningFeaturePack provisioningFeaturePack, Map<String, ServerProvisioning.ConfigFile> provisioningConfigFiles) throws IOException, XMLStreamException {
        ConfigFile configFile = serverProvisioningFeaturePackConfigFile.getFeaturePackConfigFile();
        // get provisioning config file for the output file being processed
        ServerProvisioning.ConfigFile provisioningConfigFile = provisioningConfigFiles.get(configFile.getOutputFile());
        if (provisioningConfigFile == null) {
            // the provisioning config file does not exists yet, create one
            provisioningConfigFile = new ServerProvisioning.ConfigFile(configFile.getOutputFile());
            provisioningConfigFiles.put(configFile.getOutputFile(), provisioningConfigFile);
        }
        ConfigFileOverride configFileOverride = serverProvisioningFeaturePackConfigFile.getConfigFileOverride();
        // process template
        if (configFileOverride == null || configFileOverride.isUseTemplate()) {
            // template file from this config file to be used
            // get the template's file zip entry
            ZipEntry templateFileZipEntry = zipFile.getEntry(configFile.getTemplate());
            if (templateFileZipEntry == null) {
                throw new RuntimeException("Feature pack " + provisioningFeaturePack.getFeaturePack().getFeaturePackFile() + " template file " + configFile.getTemplate() + " not found");
            }
            // set the input stream source
            provisioningConfigFile.setTemplateInputStreamSource(new ZipEntryInputStreamSource(provisioningFeaturePack.getFeaturePack().getFeaturePackFile(), templateFileZipEntry));
        }
        // get this config file subsystems
        Map<String, Map<String, SubsystemConfig>> subsystems = serverProvisioningFeaturePackConfigFile.getSubsystems();
        // merge the subsystems in the provisioning config file
        for (Map.Entry<String, Map<String, SubsystemConfig>> subsystemsEntry : subsystems.entrySet()) {
            // get the subsystems in the provisioning config file
            String profileName = subsystemsEntry.getKey();
            Map<String, SubsystemConfig> subsystemConfigMap = subsystemsEntry.getValue();
            Map<String, SubsystemConfig> provisioningSubsystems = provisioningConfigFile.getSubsystems().get(profileName);
            if (provisioningSubsystems == null) {
                // do not exist yet, create it
                provisioningSubsystems = new LinkedHashMap<>();
                provisioningConfigFile.getSubsystems().put(profileName, provisioningSubsystems);
            }
            // add the 'new' subsystem configs and related input stream sources
            for (Map.Entry<String, SubsystemConfig> subsystemConfigMapEntry : subsystemConfigMap.entrySet()) {
                String subsystemFile = subsystemConfigMapEntry.getKey();
                SubsystemConfig subsystemConfig = subsystemConfigMapEntry.getValue();
                getLog().debugf("Adding subsystem config %s to provisioning config file %s", subsystemFile, provisioningConfigFile.getOutputFile());
                // put subsystem config
                provisioningSubsystems.put(subsystemFile, subsystemConfig);
            }
        }
    }

    private void processFeaturePackCopyArtifacts(FeaturePack featurePack, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory, boolean excludeDependencies) throws IOException {
        processCopyArtifacts(featurePack.getDescription().getCopyArtifacts(), featurePack.getArtifactResolver(), outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
        if (!excludeDependencies) {
            for (FeaturePack dependency : featurePack.getDependencies()) {
                processFeaturePackCopyArtifacts(dependency, outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory, excludeDependencies);
            }
        }
    }

    private void processProvisioningFeaturePackContents(ServerProvisioningFeaturePack provisioningFeaturePack, File outputDirectory, Set<String> filesProcessed, boolean excludeDependencies) throws IOException {
        if (provisioningFeaturePack.getDescription().includesContentFiles()) {
            processFeaturePackContents(provisioningFeaturePack.getFeaturePack(), provisioningFeaturePack.getDescription().getContentFilters(), outputDirectory, filesProcessed, excludeDependencies);
        }
    }

    private void processFeaturePackContents(FeaturePack featurePack, ServerProvisioningDescription.FeaturePack.ContentFilters contentFilters, File outputDirectory, Set<String> filesProcessed, boolean excludeDependencies) throws IOException {
        final int fileNameWithoutContentsStart = Locations.CONTENT.length() + 1;
        try (JarFile jar = new JarFile(featurePack.getFeaturePackFile())) {
            for (String contentFile : featurePack.getContentFiles()) {
                final String outputFile = contentFile.substring(fileNameWithoutContentsStart);
                boolean include = true;
                if (contentFilters != null) {
                    include = contentFilters.isInclude();
                    for (FileFilter contentFilter : contentFilters.getFilters()) {
                        if (contentFilter.matches(outputFile) && !contentFilter.isInclude()) {
                            include = false;
                            break;
                        }
                    }
                }
                if (!include) {
                    getLog().debugf("Skipping feature pack %s filtered content file %s", featurePack.getFeaturePackFile(), outputFile);
                    continue;
                }
                if (!filesProcessed.add(outputFile)) {
                    getLog().debugf("Skipping already processed feature pack %s content file %s", featurePack.getFeaturePackFile(), outputFile);
                    continue;
                }
                getLog().debugf("Adding feature pack %s content file %s", featurePack.getFeaturePackFile(), outputFile);
                FileUtils.extractFile(jar, contentFile, new java.io.File(outputDirectory, outputFile));
            }
        }
        if (!excludeDependencies) {
            for (FeaturePack dependency : featurePack.getDependencies()) {
                processFeaturePackContents(dependency, contentFilters, outputDirectory, filesProcessed, excludeDependencies);
            }
        }
    }

    private void processFeaturePackFilePermissions(FeaturePack featurePack, File outputDirectory, boolean excludeDependencies) throws IOException {
        final Path baseDir = Paths.get(outputDirectory.getAbsolutePath());
        final List<FilePermission> filePermissions = featurePack.getDescription().getFilePermissions();
        Files.walkFileTree(baseDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String relative = baseDir.relativize(dir).toString();
                if (!OS_WINDOWS) {
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(dir, perm.getPermission());
                            continue;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = baseDir.relativize(file).toString();
                if (!OS_WINDOWS) {
                    for (FilePermission perm : filePermissions) {
                        if (perm.includeFile(relative)) {
                            Files.setPosixFilePermissions(file, perm.getPermission());
                            continue;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (!excludeDependencies) {
            for (FeaturePack dependency : featurePack.getDependencies()) {
                processFeaturePackFilePermissions(dependency, outputDirectory, excludeDependencies);
            }
        }
    }

    private void extractArtifact(File file, File target, CopyArtifact copy) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (copy.includeFile(entry.getName())) {
                    if (entry.isDirectory()) {
                        new File(target, copy.relocatedPath(entry.getName())).mkdirs();
                    } else {
                        try (InputStream in = zip.getInputStream(entry)) {
                            FileUtils.copyFile(in, new File(target, copy.relocatedPath(entry.getName())));
                        }
                    }
                }
            }
        }
    }

    static Logger getLog() {
        return logger;
    }

}
