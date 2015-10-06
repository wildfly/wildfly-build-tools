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
package org.wildfly.build.configassembly;

import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.FormattingXMLStreamWriter;
import org.wildfly.build.util.xml.TextNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Generate subsystems definition from a given spec
 *
 * @author Thomas.Diesler@jboss.com
 * @since 06-Sep-2012
 */
public class GenerateSubsystemsDefinition {

    private final List<SubsystemConfig> configs;
    private final String[] profiles;
    private final String filePrefix;
    private final File outputFile;

    /**
     * arg[0] - subsystems definition spec (e.g logging:osgi,osgi:eager,deployment-scanner)
     * arg[1] - subsytem profiles (e.g. default,ha,full,full-ha)
     * arg[2] - subsystem path prefix (e.g. configuration/subsystems)
     * arg[4] - the output file (e.g. domain-subsystems.xml)
     */
    public static void main(String[] args) throws Exception{
        if (args == null)
            throw new IllegalArgumentException("Null args");
        if (args.length < 4)
            throw new IllegalArgumentException("Invalid args: " + Arrays.asList(args));

        // spec := subsystem:supplement
        // definitions := definitions,spec

        int index = 0;
        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No configured subsystems");
        }
        String definitions = args[index++];

        String[] profiles = new String[]{""};
        if (args[index] != null && !args[index].isEmpty()) {
            profiles = args[index].split(",");
        }
        index++;

        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No file prefix");
        }
        String filePrefix = args[index++];
        if (!filePrefix.endsWith("/"))
            filePrefix += "/";

        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No output file");
        }
        File outputFile = new File(args[index++]);

        List<SubsystemConfig> configs = new ArrayList<SubsystemConfig>();
        for (String spec : definitions.split(",")) {
            String[] split = spec.split(":");
            String subsystem = split[0];
            String supplement = split.length > 1 ? split[1] : null;
            configs.add(new SubsystemConfig(subsystem, supplement));
        }
        new GenerateSubsystemsDefinition(configs, profiles, filePrefix, outputFile).process();
    }

    private GenerateSubsystemsDefinition(List<SubsystemConfig> configs, String[] profiles, String filePrefix, File outputFile) {
        this.configs = configs;
        this.profiles = profiles;
        this.filePrefix = filePrefix;
        this.outputFile = outputFile;
    }

    private void process() throws XMLStreamException, IOException {

        ElementNode config = new ElementNode(null, "config", SubsystemsParser.NAMESPACE);
        for (String profile : profiles) {
            ElementNode subsystems = new ElementNode(config, "subsystems");
            if (!profile.isEmpty()) {
                subsystems.addAttribute("name", new AttributeValue(profile));
            }
            config.addChild(subsystems);

            for (SubsystemConfig sub : configs) {
                ElementNode subsystem = new ElementNode(config, "subsystem");
                if (sub.getSupplement() != null) {
                    subsystem.addAttribute("supplement", new AttributeValue(sub.getSupplement()));
                }
                subsystem.addChild(new TextNode(filePrefix + sub.getSubsystem() + ".xml"));
                subsystems.addChild(subsystem);
            }
        }

        try (Writer writer = new FileWriter(outputFile)) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlwriter = new FormattingXMLStreamWriter(factory.createXMLStreamWriter(writer));
            config.marshall(xmlwriter);
        }
    }
}
