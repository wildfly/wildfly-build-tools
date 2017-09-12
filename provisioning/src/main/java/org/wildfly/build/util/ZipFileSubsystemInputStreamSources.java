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

package org.wildfly.build.util;

import org.wildfly.build.ArtifactFileResolver;
import org.wildfly.build.configassembly.SubsystemInputStreamSources;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.FeaturePack;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class ZipFileSubsystemInputStreamSources implements SubsystemInputStreamSources {

    private final Map<String, ZipEntryInputStreamSource> inputStreamSourceMap = new HashMap<>();

    /**
     * Creates a zip entry inputstream source and maps it to the specified filename.
     * @param subsystemFileName
     * @param zipFile
     * @param zipEntry
     */
    public void addSubsystemFileSource(String subsystemFileName, File zipFile, ZipArchiveEntry zipEntry) {
       inputStreamSourceMap.put(subsystemFileName, new ZipEntryInputStreamSource(zipFile, zipEntry));
    }

    /**
     * Adds all subsystem input stream sources from the specified factory. Note that only absent sources will be added.
     * @param other
     */
    public void addAllSubsystemFileSources(ZipFileSubsystemInputStreamSources other) {
        for (Map.Entry<String, ZipEntryInputStreamSource> entry : other.inputStreamSourceMap.entrySet()) {
            if (!this.inputStreamSourceMap.containsKey(entry.getKey())) {
                this.inputStreamSourceMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds all file sources in the specified zip file.
     * @param file
     * @throws IOException
     */
    public void addAllSubsystemFileSourcesFromZipFile(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            // extract subsystem template and schema, if present
            if (zip.getEntry("subsystem-templates") != null) {
                Enumeration<ZipArchiveEntry> entries = zip.getEntries();
                while (entries.hasMoreElements()) {
                    ZipArchiveEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.startsWith("subsystem-templates/")) {
                            addSubsystemFileSource(entryName.substring("subsystem-templates/".length()), file, entry);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds the file source for the specified subsystem, from the specified zip file.
     * @param subsystem
     * @param file
     * @return true if such subsystem file source was found and added; false otherwise
     * @throws IOException
     */
    public boolean addSubsystemFileSourceFromZipFile(String subsystem, File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String entryName = "subsystem-templates/"+subsystem;
            ZipArchiveEntry entry = zip.getEntry(entryName);
            if (entry != null) {
                addSubsystemFileSource(subsystem, file, entry);
                return true;
            }
        }
        return false;
    }

    /**
     * Adds all file sources in the specified module's artifacts.
     * @param module
     * @param artifactFileResolver
     * @param transitive
     * @throws IOException
     */
    public void addAllSubsystemFileSourcesFromModule(FeaturePack.Module module, ArtifactFileResolver artifactFileResolver, boolean transitive) throws IOException {
        // the subsystem templates are included in module artifacts files
        for (ModuleParseResult.ArtifactName artifactName : module.getModuleParseResult().getArtifacts()) {
            // resolve the artifact
            Artifact artifact = module.getFeaturePack().getArtifactResolver().getArtifact(artifactName.getArtifactCoords());
            if (artifact == null) {
                throw new RuntimeException("Could not resolve module resource artifact " + artifactName.getArtifactCoords() + " for feature pack " + module.getFeaturePack().getFeaturePackFile());
            }
            // resolve the artifact file
            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
            // get the subsystem templates
            addAllSubsystemFileSourcesFromZipFile(artifactFile);
        }
        if (transitive) {
            for (FeaturePack.Module dependency : module.getDependencies().values()) {
                addAllSubsystemFileSourcesFromModule(dependency, artifactFileResolver, false);
            }
        }
    }

    /**
     * Adds the file source for the specified subsystem, from the specified module's artifacts.
     * @param subsystem
     * @param module
     * @param artifactFileResolver
     * @return true if such subsystem file source was found and added; false otherwise
     * @throws IOException
     */
    public boolean addSubsystemFileSourceFromModule(String subsystem, FeaturePack.Module module, ArtifactFileResolver artifactFileResolver) throws IOException {
        // the subsystem templates are included in module artifacts files
        for (ModuleParseResult.ArtifactName artifactName : module.getModuleParseResult().getArtifacts()) {
            // resolve the artifact
            Artifact artifact = module.getFeaturePack().getArtifactResolver().getArtifact(artifactName.getArtifactCoords());
            if (artifact == null) {
                throw new RuntimeException("Could not resolve module resource artifact " + artifactName.getArtifactCoords() + " for feature pack " + module.getFeaturePack().getFeaturePackFile());
            }
            // resolve the artifact file
            File artifactFile = artifactFileResolver.getArtifactFile(artifact);
            // get the subsystem template from the artifact file
            if (addSubsystemFileSourceFromZipFile(subsystem, artifactFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        return inputStreamSourceMap.get(subsystemFileName);
    }

    @Override
    public String toString() {
        return "zip subsystem parser factory files: "+ inputStreamSourceMap.keySet();
    }

}
