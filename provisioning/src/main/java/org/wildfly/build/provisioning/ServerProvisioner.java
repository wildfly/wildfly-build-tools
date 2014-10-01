/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.provisioning;

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
import java.io.ByteArrayInputStream;
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

    public static void build(ServerProvisioningDescription description, File outputDirectory, ArtifactFileResolver artifactFileResolver, ArtifactResolver versionOverrideArtifactResolver) {
        final ServerProvisioning serverProvisioning = new ServerProvisioning(description);
        final List<String> errors = new ArrayList<>();
        try {
            // create the feature packs
            for (ServerProvisioningDescription.FeaturePack serverProvisioningFeaturePackDescription : description.getFeaturePacks()) {
                final FeaturePack featurePack = FeaturePackFactory.createPack(serverProvisioningFeaturePackDescription.getArtifact(), artifactFileResolver, versionOverrideArtifactResolver);
                serverProvisioning.getFeaturePacks().add(new ServerProvisioningFeaturePack(serverProvisioningFeaturePackDescription, featurePack));
            }
            // create output dir
            FileUtils.deleteRecursive(outputDirectory);
            outputDirectory.mkdirs();
            // create schema output dir if needed
            final File schemaOutputDirectory;
            if (serverProvisioning.getDescription().isExtractSchemas()) {
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
            // process the server config
            processConfig(serverProvisioning, outputDirectory, filesProcessed);
            // process everything else for each feature pack
            for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
                processFeaturePackCopyArtifacts(provisioningFeaturePack.getFeaturePack(), outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
                processProvisioningFeaturePackContents(provisioningFeaturePack, outputDirectory, filesProcessed);
                processFeaturePackFilePermissions(provisioningFeaturePack.getFeaturePack(), outputDirectory);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if(!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Some errors were encountered creating the feature pack\n");
                for(String error : errors) {
                    sb.append(error);
                    sb.append("\n");
                }
                throw new RuntimeException(sb.toString());
            }
        }
    }

    private static void processCopyArtifacts(List<CopyArtifact> copyArtifacts, ArtifactResolver artifactResolver, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException {
        for (CopyArtifact copyArtifact : copyArtifacts) {
            if (!filesProcessed.add(copyArtifact.getToLocation())) {
                continue;
            }
            File target = new File(outputDirectory, copyArtifact.getToLocation());
            if (!target.getParentFile().isDirectory()) {
                if (!target.getParentFile().mkdirs()) {
                    throw new IOException("Could not create directory " + target.getParentFile());
                }
            }
            Artifact artifact = artifactResolver.getArtifact(copyArtifact.getArtifact());
            if (artifact == null) {
                throw new RuntimeException("Could not resolve artifact " + copyArtifact.getArtifact() + " to copy");
            }
            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
            if (artifactFile == null) {
                throw new RuntimeException("Could not resolve file for artifact " + copyArtifact.getArtifact() + " to copy");
            }
            if (copyArtifact.isExtract()) {
                extractArtifact(artifactFile, target, copyArtifact);
            } else {
                FileUtils.copyFile(artifactFile, target);
            }

            if (schemaOutputDirectory != null) {
                // extract schemas, if any
                FileUtils.extractSchemas(artifactFile, schemaOutputDirectory);
            }
        }
    }

    private static void processModules(ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException, XMLStreamException {
        getLog().debug("Processing provisioning modules");
        // 1. gather the modules for each feature pack
        final Map<FeaturePack, List<FeaturePack.Module>> featurePackModulesMap = new HashMap<>();
        Set<ModuleIdentifier> moduleIdentifiers = new HashSet<>();
        for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
            getLog().debug("Gathering modules for provisioning feature pack " + provisioningFeaturePack.getFeaturePack().getFeaturePackFile());
            for (FeaturePack.Module module : provisioningFeaturePack.getModules(artifactFileResolver).values()) {
                final ModuleIdentifier moduleIdentifier = module.getIdentifier();
                if (moduleIdentifiers.add(moduleIdentifier)) {
                    getLog().debug("Adding " + moduleIdentifier + " from feature pack " + module.getFeaturePack().getFeaturePackFile() + " to provisioning module set.");
                    List<FeaturePack.Module> featurePackModules = featurePackModulesMap.get(module.getFeaturePack());
                    if (featurePackModules == null) {
                        featurePackModules = new ArrayList<>();
                        featurePackModulesMap.put(module.getFeaturePack(), featurePackModules);
                    }
                    featurePackModules.add(module);
                } else {
                    getLog().debug("Not adding " + moduleIdentifier + " from feature pack " + module.getFeaturePack().getFeaturePackFile()+". A module with such identifier is already in the provisioning module set.");
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

    private static void processFeaturePackModules(FeaturePack featurePack, List<FeaturePack.Module> includedModules, ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException {
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
                // read module xml to string for content update
                String moduleXmlContents = FileUtils.readFile(targetFile);
                // parse the module xml
                ModuleParseResult result = module.getModuleParseResult();
                // process module artifacts
                for (ModuleParseResult.ArtifactName artifactName : result.getArtifacts()) {
                    String artifactCoords = artifactName.getArtifactCoords();
                    String options = artifactName.getOptions();
                    boolean jandex = false;
                    if (options != null) {
                        jandex = options.contains("jandex"); //todo: eventually we may need options to have a proper query string type syntax
                        moduleXmlContents = moduleXmlContents.replace(artifactName.toString(), artifactCoords); //todo: all these replace calls are a bit yuck, we may need proper solution if this gets more complex
                    }
                    Artifact artifact = featurePack.getArtifactResolver().getArtifact(artifactCoords);
                    if (artifact == null) {
                        throw new RuntimeException("Could not resolve module resource artifact " + artifactName + " for feature pack " + featurePack.getFeaturePackFile());
                    }
                    try {
                        // process the module artifact
                        File artifactFile = artifactFileResolver.getArtifactFile(artifact);
                        // add all subsystem templates
                        serverProvisioning.getConfig().getInputStreamSources().addAllSubsystemFileSourcesFromZipFile(artifactFile);
                        // extract schemas if needed
                        if (schemaOutputDirectory != null) {
                            FileUtils.extractSchemas(artifactFile, schemaOutputDirectory);
                        }
                        if (jandex) {
                            String baseName = artifactFile.getName().substring(0, artifactFile.getName().lastIndexOf("."));
                            String extension = artifactFile.getName().substring(artifactFile.getName().lastIndexOf("."));
                            File target = new File(targetFile.getParent(), baseName + "-jandex" + extension);
                            JandexIndexer.createIndex(artifactFile, new FileOutputStream(target));
                            moduleXmlContents = moduleXmlContents.replaceAll("(\\s*)<artifact\\s+name=\"\\$\\{" + artifactCoords + "\\}\"\\s*/>", "$1<artifact name=\"\\${" + artifactCoords + "}\" />$1<resource-root path=\"" + target.getName() + "\"/>");
                        }
                        if (!thinServer) {
                            // copy the artifact
                            String artifactFileName = artifactFile.getName();
                            FileUtils.copyFile(artifactFile, new File(targetFile.getParent(), artifactFileName));
                            // update module xml content
                            moduleXmlContents = moduleXmlContents.replaceAll("<artifact\\s+name=\"\\$\\{" + artifactCoords + "\\}\"\\s*/>", "<resource-root path=\"" + artifactFileName + "\"/>");
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException("Could not extract resources from " + artifactName, t);
                    }
                }
                if (thinServer) {
                    // replace artifact coords properties with the ones expected by jboss-modules
                    moduleXmlContents = buildPropertyReplacer.replaceProperties(moduleXmlContents);
                }
                // write updated module xml content
                FileUtils.copyFile(new ByteArrayInputStream(moduleXmlContents.getBytes("UTF-8")), targetFile);

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

    private static void processConfig(ServerProvisioning serverProvisioning, File outputDirectory, Set<String> filesProcessed) throws IOException, XMLStreamException {
        getLog().debug("Processing provisioning config");
        ServerProvisioning.Config provisioningConfig = serverProvisioning.getConfig();
        // 1. collect and merge each feature pack configs
        for (ServerProvisioningFeaturePack provisioningFeaturePack : serverProvisioning.getFeaturePacks()) {
            processFeaturePackConfig(provisioningFeaturePack, provisioningConfig);
        }
        // 2. assemble the merged configs
        for (ServerProvisioning.ConfigFile provisioningConfigFile : provisioningConfig.getDomainConfigFiles().values()) {
            getLog().debug("Assembling config file "+provisioningConfigFile.getOutputFile());
            filesProcessed.add(provisioningConfigFile.getOutputFile());
            new ConfigurationAssembler(provisioningConfig.getInputStreamSources(),
                    provisioningConfigFile.getTemplateInputStreamSource(),
                    "domain",
                    provisioningConfigFile.getSubsystems(),
                    new File(outputDirectory, provisioningConfigFile.getOutputFile()))
                    .assemble();
        }
        for (ServerProvisioning.ConfigFile provisioningConfigFile : provisioningConfig.getStandaloneConfigFiles().values()) {
            getLog().debug("Assembling config file "+provisioningConfigFile.getOutputFile());
            filesProcessed.add(provisioningConfigFile.getOutputFile());
            new ConfigurationAssembler(provisioningConfig.getInputStreamSources(),
                    provisioningConfigFile.getTemplateInputStreamSource(),
                    "server",
                    provisioningConfigFile.getSubsystems(),
                    new File(outputDirectory, provisioningConfigFile.getOutputFile()))
                    .assemble();
        }
    }

    private static void processFeaturePackConfig(ServerProvisioningFeaturePack provisioningFeaturePack, ServerProvisioning.Config provisioningConfig) throws IOException, XMLStreamException {
        FeaturePack featurePack = provisioningFeaturePack.getFeaturePack();
        getLog().debug("Processing provisioning feature pack "+featurePack.getFeaturePackFile()+" configs");
        try (ZipFile zipFile = new ZipFile(featurePack.getFeaturePackFile())) {
            for (ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile : provisioningFeaturePack.getDomainConfigFiles()) {
                processFeaturePackConfigFile(serverProvisioningFeaturePackConfigFile, zipFile, provisioningFeaturePack, provisioningConfig.getDomainConfigFiles());
            }
            for (ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile : provisioningFeaturePack.getStandaloneConfigFiles()) {
                processFeaturePackConfigFile(serverProvisioningFeaturePackConfigFile, zipFile, provisioningFeaturePack, provisioningConfig.getStandaloneConfigFiles());
            }
        }
    }

    private static void processFeaturePackConfigFile(ServerProvisioningFeaturePack.ConfigFile serverProvisioningFeaturePackConfigFile, ZipFile zipFile, ServerProvisioningFeaturePack provisioningFeaturePack, Map<String, ServerProvisioning.ConfigFile> provisioningConfigFiles) throws IOException, XMLStreamException {
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
            String subsystemsName = subsystemsEntry.getKey();
            Map<String, SubsystemConfig> subsystemConfigMap = subsystemsEntry.getValue();
            Map<String, SubsystemConfig> provisioningSubsystems = provisioningConfigFile.getSubsystems().get(subsystemsName);
            if (provisioningSubsystems == null) {
                // do not exist yet, create it
                provisioningSubsystems = new HashMap<>();
                provisioningConfigFile.getSubsystems().put(subsystemsName, provisioningSubsystems);
            }
            // add the 'new' subsystem configs and related input stream sources
            for (Map.Entry<String, SubsystemConfig> subsystemConfigMapEntry : subsystemConfigMap.entrySet()) {
                String subsystemFile = subsystemConfigMapEntry.getKey();
                SubsystemConfig subsystemConfig = subsystemConfigMapEntry.getValue();
                getLog().debug("Adding subsystem config "+subsystemFile+" to provisioning config file "+provisioningConfigFile.getOutputFile());
                // put subsystem config
                provisioningSubsystems.put(subsystemFile, subsystemConfig);
            }
        }
    }

    private static void processFeaturePackCopyArtifacts(FeaturePack featurePack, File outputDirectory, Set<String> filesProcessed, ArtifactFileResolver artifactFileResolver, File schemaOutputDirectory) throws IOException {
        processCopyArtifacts(featurePack.getDescription().getCopyArtifacts(), featurePack.getArtifactResolver(), outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processCopyArtifacts(dependency.getDescription().getCopyArtifacts(), dependency.getArtifactResolver(), outputDirectory, filesProcessed, artifactFileResolver, schemaOutputDirectory);
        }
    }

    private static void processProvisioningFeaturePackContents(ServerProvisioningFeaturePack provisioningFeaturePack, File outputDirectory, Set<String> filesProcessed) throws IOException {
        processFeaturePackContents(provisioningFeaturePack.getFeaturePack(), provisioningFeaturePack.getDescription().getContentFilters(), outputDirectory, filesProcessed);
    }

    private static void processFeaturePackContents(FeaturePack featurePack, List<FileFilter> contentFilters, File outputDirectory, Set<String> filesProcessed) throws IOException {
        final int fileNameWithoutContentsStart = Locations.CONTENT.length() + 1;
        try (JarFile jar = new JarFile(featurePack.getFeaturePackFile())) {
            for (String contentFile : featurePack.getContentFiles()) {
                final String outputFile = contentFile.substring(fileNameWithoutContentsStart);
                boolean include = true;
                if (contentFilters != null) {
                    for (FileFilter contentFilter : contentFilters) {
                        if (contentFilter.matches(outputFile) && !contentFilter.isInclude()) {
                            include = false;
                            break;
                        }
                    }
                }
                if (!include) {
                    getLog().debug("Skipping feature pack " + featurePack.getFeaturePackFile() + " filtered content file " + outputFile);
                    continue;
                }
                if (!filesProcessed.add(outputFile)) {
                    getLog().debug("Skipping already processed feature pack " + featurePack.getFeaturePackFile() + " content file "+outputFile);
                    continue;
                }
                getLog().debug("Adding feature pack " + featurePack.getFeaturePackFile() + " content file "+outputFile);
                FileUtils.extractFile(jar, contentFile, new java.io.File(outputDirectory, outputFile));
            }
        }
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processFeaturePackContents(dependency, contentFilters, outputDirectory, filesProcessed);
        }
    }

    private static void processFeaturePackFilePermissions(FeaturePack featurePack, File outputDirectory) throws IOException {
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
        for (FeaturePack dependency : featurePack.getDependencies()) {
            processFeaturePackFilePermissions(dependency, outputDirectory);
        }
    }

    private static void extractArtifact(File file, File target, CopyArtifact copy) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (copy.includeFile(entry.getName())) {
                    if (entry.isDirectory()) {
                        new File(target, entry.getName()).mkdirs();
                    } else {
                        try (InputStream in = zip.getInputStream(entry)) {
                            FileUtils.copyFile(in, new File(target, entry.getName()));
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
