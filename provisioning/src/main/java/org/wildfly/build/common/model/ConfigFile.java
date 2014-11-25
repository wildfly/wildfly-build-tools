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

package org.wildfly.build.common.model;

import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.configassembly.SubsystemsParser;
import org.wildfly.build.util.InputStreamSource;
import org.wildfly.build.util.ZipEntryInputStreamSource;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigFile {

    private final Map<String, String> properties;
    private final String template;
    private final String subsystems;
    private final String outputFile;

    public ConfigFile(Map<String, String> properties, String template, String subsystems, String outputFile) {
        this.properties = properties;
        this.template = template;
        this.subsystems = subsystems;
        this.outputFile = outputFile;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getTemplate() {
        return template;
    }

    public String getSubsystems() {
        return subsystems;
    }

    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Retrieves the subsystems configs.
     * @param featurePackFile the feature pack's file containing the subsystem configs
     * @return
     * @throws IOException
     * @throws XMLStreamException
     */
    public Map<String, Map<String, SubsystemConfig>> getSubsystemConfigs(File featurePackFile) throws IOException, XMLStreamException {
        Map<String, Map<String, SubsystemConfig>> subsystems = new HashMap<>();
        try (ZipFile zip = new ZipFile(featurePackFile)) {
            ZipEntry zipEntry = zip.getEntry(getSubsystems());
            if (zipEntry == null) {
                throw new RuntimeException("Feature pack " + featurePackFile + " subsystems file " + getSubsystems() + " not found");
            }
            InputStreamSource inputStreamSource = new ZipEntryInputStreamSource(featurePackFile, zipEntry);
            SubsystemsParser.parse(inputStreamSource, getProperties(), subsystems);
        }
        return subsystems;
    }
}
