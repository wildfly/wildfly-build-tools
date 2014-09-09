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

import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.InputStreamSource;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.FormattingXMLStreamWriter;
import org.wildfly.build.util.xml.Node;
import org.wildfly.build.util.xml.ProcessingInstructionNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigurationAssembler {

    private final SubsystemInputStreamSources subsystemInputStreamSources;
    private final InputStreamSource templateInputStreamSource;
    private final String templateRootElementName;
    private final InputStreamSource subsystemsInputStreamSource;
    private final File outputFile;
    private final PropertyResolver properties;

    public ConfigurationAssembler(SubsystemInputStreamSources subsystemInputStreamSources, InputStreamSource templateInputStreamSource, String templateRootElementName, InputStreamSource subsystemsInputStreamSource, File outputFile, PropertyResolver properties) {
        this.properties = properties;
        this.subsystemInputStreamSources = subsystemInputStreamSources;
        this.templateInputStreamSource = templateInputStreamSource;
        this.templateRootElementName = templateRootElementName;
        this.subsystemsInputStreamSource = subsystemsInputStreamSource;
        this.outputFile = outputFile.getAbsoluteFile();
    }

    public void assemble() throws IOException, XMLStreamException {
        TemplateParser templateParser = new TemplateParser(templateInputStreamSource, templateRootElementName);
        templateParser.parse();
        SubsystemsParser subsystemsParser = new SubsystemsParser(subsystemsInputStreamSource, new BuildPropertyReplacer(properties));
        subsystemsParser.parse();

        populateTemplate(templateParser, subsystemsParser);

        if (outputFile.exists()) {
            outputFile.delete();
        }
        if (!outputFile.getParentFile().exists()) {
            if (!outputFile.getParentFile().mkdirs()) {
                throw new IllegalStateException("Could not create " + outputFile.getParentFile());
            }
        }
        FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(new BufferedWriter(new FileWriter(outputFile))));
        try {
            writer.writeStartDocument();
            templateParser.getRootNode().marshall(writer);
            writer.writeEndDocument();
        } finally {
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
    }

    private void populateTemplate(TemplateParser templateParser, SubsystemsParser subsystemsParser) throws IOException, XMLStreamException{
        final Set<String> extensions = new TreeSet<String>();
        final Map<String, Map<String, ElementNode>> socketBindingsByGroup = new HashMap<String, Map<String, ElementNode>>();
        final Map<String, Map<String, ElementNode>> outboundSocketBindingsByGroup = new HashMap<String, Map<String, ElementNode>>();
        for (Map.Entry<String, ProcessingInstructionNode> subsystemEntry : templateParser.getSubsystemPlaceholders().entrySet()) {
            final String subsystemName = subsystemEntry.getKey();
            final String groupName = subsystemEntry.getValue().getDataValue("socket-binding-group", "");

            final SubsystemConfig[] subsystems = subsystemsParser.getSubsystemConfigs().get(subsystemName);
            if (subsystems == null) {
                throw new IllegalStateException("Could not find a subsystems configuration called '" + subsystemEntry.getKey() + "' in " + subsystemsInputStreamSource);
            }
            final Map<String, ElementNode> socketBindings = new TreeMap<String, ElementNode>();
            if (socketBindingsByGroup.put(groupName, socketBindings) != null) {
                throw new IllegalStateException("Group '" + groupName + "' already exists");
            }
            final Map<String, ElementNode> outboundSocketBindings = new TreeMap<String, ElementNode>();
            outboundSocketBindingsByGroup.put(groupName, outboundSocketBindings);

            for (SubsystemConfig subsystem : subsystems) {
                final InputStreamSource inputStreamSource = subsystemInputStreamSources.getInputStreamSource(subsystem.getSubsystem());
                if (inputStreamSource == null) {
                    throw new IllegalStateException("Could not resolve '" + subsystem);
                }
                final SubsystemParser subsystemParser = new SubsystemParser(templateParser.getRootNode().getNamespace(), subsystem.getSupplement(), inputStreamSource);
                subsystemParser.parse();
                subsystemEntry.getValue().addDelegate(subsystemParser.getSubsystem());
                extensions.add(subsystemParser.getExtensionModule());
                for (Map.Entry<String, ElementNode> entry : subsystemParser.getSocketBindings().entrySet()) {
                    if (socketBindings.containsKey(entry.getKey())) {
                        throw new IllegalStateException("SocketBinding '" + entry + "' already exists");
                    }
                    socketBindings.put(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, ElementNode> entry : subsystemParser.getOutboundSocketBindings().entrySet()) {
                    if (outboundSocketBindings.containsKey(entry.getKey())) {
                        throw new IllegalStateException("Outbound SocketBinding '" + entry + "' already exists");
                    }
                    outboundSocketBindings.put(entry.getKey(), entry.getValue());
                }
            }
        }

        if (extensions.size() > 0) {
            final ProcessingInstructionNode extensionNode = templateParser.getExtensionPlaceHolder();
            for (String extension : extensions) {
                final ElementNode extensionElement = new ElementNode(null, "extension", templateParser.getRootNode().getNamespace());
                extensionElement.addAttribute("module", new AttributeValue(extension));
                extensionNode.addDelegate(extensionElement);
            }
        } else {
            //Delete the extensions element if there are no extensions
            for (Iterator<Node> it = templateParser.getRootNode().iterateChildren() ; it.hasNext() ; ) {
                Node node = it.next();
                if (node instanceof ElementNode) {
                    ElementNode elementNode = (ElementNode)node;
                    if (elementNode.getName().equals("extensions") || elementNode.getName().equals("profile")) {
                        it.remove();
                    }
                }
            }
        }
        if (socketBindingsByGroup.size() > 0 && outboundSocketBindingsByGroup.size() > 0) {
            for (Map.Entry<String, ProcessingInstructionNode> entry : templateParser.getSocketBindingsPlaceHolders().entrySet()) {
                Map<String, ElementNode> socketBindings = socketBindingsByGroup.get(entry.getKey());
                if (socketBindings == null) {
                    throw new IllegalArgumentException("No socket bindings for group " + entry.getKey());
                }
                if (socketBindings.size() > 0) {
                    for (ElementNode binding : socketBindings.values()) {
                        entry.getValue().addDelegate(binding);
                    }
                }
            }
            for (Map.Entry<String, ProcessingInstructionNode> entry : templateParser.getSocketBindingsPlaceHolders().entrySet()) {
                Map<String, ElementNode> outboundSocketBindings = outboundSocketBindingsByGroup.get(entry.getKey());
                if (outboundSocketBindings == null) {
                    throw new IllegalArgumentException("No outbound socket bindings for group " + entry.getKey());
                }
                if (outboundSocketBindings.size() > 0) {
                    for (ElementNode binding : outboundSocketBindings.values()) {
                        entry.getValue().addDelegate(binding);
                    }
                }
            }
        }
    }
}
