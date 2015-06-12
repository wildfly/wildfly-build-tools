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

import org.wildfly.build.util.InputStreamSource;
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.Node;
import org.wildfly.build.util.xml.NodeParser;
import org.wildfly.build.util.xml.ParsingUtils;
import org.wildfly.build.util.xml.ProcessingInstructionNode;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemParser extends NodeParser {

    private final String socketBindingNamespace;
    private final InputStreamSource inputStreamSource;
    private final String supplementName;
    private String extensionModule;
    private Node subsystem;
    private final Map<String, ElementNode> socketBindings = new HashMap<String, ElementNode>();
    private final Map<String, ElementNode> outboundSocketBindings = new HashMap<String, ElementNode>();
    private final Map<String, Set<ProcessingInstructionNode>> supplementPlaceholders = new HashMap<String, Set<ProcessingInstructionNode>>();
    private final Map<String, Supplement> supplementReplacements = new HashMap<String, Supplement>();
    private final Map<String, List<AttributeValue>> attributesForReplacement = new HashMap<String, List<AttributeValue>>();
    private final Map<String, ElementNode> interfaces = new HashMap<String, ElementNode>();

    SubsystemParser(String socketBindingNamespace, String supplementName, InputStreamSource inputStreamSource){
        this.socketBindingNamespace = socketBindingNamespace;
        this.supplementName = supplementName;
        this.inputStreamSource = inputStreamSource;
    }

    String getExtensionModule() {
        return extensionModule;
    }

    Node getSubsystem() {
        return subsystem;
    }

    Map<String, ElementNode> getSocketBindings(){
        return socketBindings;
    }

    Map<String, ElementNode> getInterfaces(){
        return interfaces;
    }

    Map<String, ElementNode> getOutboundSocketBindings(){
        return outboundSocketBindings;
    }

    void parse() throws IOException, XMLStreamException {
        try (InputStream in = inputStreamSource.getInputStream()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            reader.require(START_DOCUMENT, null, null);
            Map<String, String> configAttributes = new HashMap<String, String>();
            configAttributes.put("default-supplement", null);
            ParsingUtils.getNextElement(reader, "config", configAttributes, false);
            extensionModule = ParsingUtils.getNextElement(reader, "extension-module", null, true);
            ParsingUtils.getNextElement(reader, "subsystem", null, false);
            subsystem = super.parseNode(reader, "subsystem");

            while (reader.hasNext()) {
                if (reader.next() == START_ELEMENT) {
                    if (reader.getLocalName().equals("subsystem")) {
                        throw new XMLStreamException("Only one subsystem element is allowed", reader.getLocation());
                    } else if (reader.getLocalName().equals("supplement")) {
                        parseSupplement(reader, ((ElementNode)subsystem).getNamespace());
                    } else if (reader.getLocalName().equals("socket-binding")) {
                        ElementNode socketBinding = new NodeParser(socketBindingNamespace).parseNode(reader, "socket-binding");
                        socketBindings.put(socketBinding.getAttributeValue("name"), socketBinding);
                    } else if (reader.getLocalName().equals("outbound-socket-binding")) {
                        ElementNode socketBinding = new NodeParser(socketBindingNamespace).parseNode(reader, "outbound-socket-binding");
                        outboundSocketBindings.put(socketBinding.getAttributeValue("name"), socketBinding);
                    } else if (reader.getLocalName().equals("interface")) {
                        ElementNode iface = new NodeParser(socketBindingNamespace).parseNode(reader, "interface");
                        interfaces.put(iface.getAttributeValue("name"), iface);
                    }
                }
            }

            //Check for the default supplement name if no supplement is set
            String supplementName = this.supplementName;
            if (supplementName == null) {
                supplementName = configAttributes.get("default-supplement");
            }

            if (supplementName != null) {
                Supplement supplement = supplementReplacements.get(supplementName);
                if (supplement == null) {
                    throw new IllegalStateException("No supplement called '" + supplementName + "' could be found to augment the subsystem configuration");
                }
                Map<String, ElementNode> nodeReplacements = supplement.getAllNodeReplacements();
                for (Map.Entry<String, Set<ProcessingInstructionNode>> entry : supplementPlaceholders.entrySet()) {
                    ElementNode replacement = nodeReplacements.get(entry.getKey());
                    if (replacement != null) {
                        for (Iterator<Node> it = replacement.iterateChildren() ; it.hasNext() ; ) {
                            Node node = it.next();
                            for (ProcessingInstructionNode processingInstructionNode : entry.getValue()) {
                                processingInstructionNode.addDelegate(node);
                            }
                        }
                    }
                }

                Map<String, String> attributeReplacements = supplement.getAllAttributeReplacements();
                for (Map.Entry<String, List<AttributeValue>> entry : attributesForReplacement.entrySet()) {
                    String replacement = attributeReplacements.get(entry.getKey());
                    if (replacement == null) {
                        throw new IllegalStateException("No replacement found for " + entry.getKey() + " in supplement " + supplementName);
                    }
                    for (AttributeValue attrValue : entry.getValue()) {
                        attrValue.setValue(replacement);
                    }
                }
            }

        }
    }

    protected void parseSupplement(XMLStreamReader reader, String subsystemNs) throws XMLStreamException {
        String name = null;
        String[] includes = null;
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("name")) {
                name = reader.getAttributeValue(i);
            } else if (attr.equals("includes")){
                String tmp = reader.getAttributeValue(i);
                includes = tmp.split(" ");
            } else {
                throw new XMLStreamException("Invalid attribute " + attr, reader.getLocation());
            }
        }
        if (name == null) {
            throw new XMLStreamException("Missing required attribute 'name'", reader.getLocation());
        }
        if (name.length() == 0) {
            throw new XMLStreamException("Empty name attribute for <supplement>", reader.getLocation());
        }

        Supplement supplement = new Supplement(includes);
        if (supplementReplacements.put(name, supplement) != null) {
            throw new XMLStreamException("Already have a supplement called " + name, reader.getLocation());
        }

        while (reader.hasNext()) {
            reader.next();
            int type = reader.getEventType();
            switch (type) {
            case START_ELEMENT:
                if (reader.getLocalName().equals("replacement")) {
                    parseSupplementReplacement(reader, subsystemNs, supplement);
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("supplement")){
                    return;
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
            }
        }
    }

    protected void parseSupplementReplacement(XMLStreamReader reader, String subsystemNs, Supplement supplement) throws XMLStreamException {
        String placeholder = null;
        String attributeValue = null;
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("placeholder")) {
                placeholder = reader.getAttributeValue(i);
            } else if (attr.equals("attributeValue")) {
                attributeValue = reader.getAttributeValue(i);
            }else {
                throw new XMLStreamException("Invalid attribute " + attr, reader.getLocation());
            }
        }
        if (placeholder == null) {
            throw new XMLStreamException("Missing required attribute 'placeholder'", reader.getLocation());
        }
        if (placeholder.length() == 0) {
            throw new XMLStreamException("Empty placeholder attribute for <replacement>", reader.getLocation());
        }

        if (attributeValue != null) {
            supplement.addAttributeReplacement(placeholder, attributeValue);
        }

        while (reader.hasNext()) {
            int type = reader.getEventType();
            switch (type) {
            case START_ELEMENT:
                ElementNode node = new NodeParser(subsystemNs).parseNode(reader, reader.getLocalName());
                if (attributeValue != null && node.iterateChildren().hasNext()) {
                    throw new XMLStreamException("Can not have nested content when attributeValue is used", reader.getLocation());
                }
                if (supplement.addNodeReplacement(placeholder, node) != null) {
                    throw new XMLStreamException("Already have a replacement called " + placeholder + " in supplement", reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("replacement")){
                    return;
                } else {
                    throw new XMLStreamException("Unknown element " + reader.getLocalName(), reader.getLocation());
                }
            }
        }
    }

    @Override
    protected ProcessingInstructionNode parseProcessingInstruction(XMLStreamReader reader, ElementNode parent) throws XMLStreamException {
        String name = reader.getPITarget();
        ProcessingInstructionNode placeholder = new ProcessingInstructionNode(name, parseProcessingInstructionData(reader.getPIData()));
        Set<ProcessingInstructionNode> processingInstructionNodes;
        if (supplementPlaceholders.containsKey(name)) {
            processingInstructionNodes = supplementPlaceholders.get(name);
        } else {
            processingInstructionNodes = new HashSet<ProcessingInstructionNode>();
        }
        processingInstructionNodes.add(placeholder);
        supplementPlaceholders.put(name, processingInstructionNodes);
        return placeholder;
    }


    protected AttributeValue createAttributeValue(String attributeValue) {
        AttributeValue value = super.createAttributeValue(attributeValue);
        if (attributeValue.startsWith("@@")) {
            List<AttributeValue> attributeValues = attributesForReplacement.get(attributeValue);
            if (attributeValues == null) {
                attributeValues = new ArrayList<AttributeValue>();
                attributesForReplacement.put(attributeValue, attributeValues);
            }
            attributeValues.add(value);
        }
        return value;
    }

    private class Supplement {
        final String[] includes;
        final Map<String, ElementNode> nodeReplacements = new HashMap<String, ElementNode>();
        final Map<String, String> attributeReplacements = new HashMap<String, String>();

        Supplement(String[] includes){
            this.includes = includes;
        }

        ElementNode addNodeReplacement(String placeholder, ElementNode replacement) {
            return nodeReplacements.put(placeholder, replacement);
        }

        String addAttributeReplacement(String placeholder, String replacement) {
            return attributeReplacements.put(placeholder, replacement);
        }

        Map<String, ElementNode> getAllNodeReplacements() {
            Map<String, ElementNode> result = new HashMap<String, ElementNode>();
            getAllNodeReplacements(result);
            return result;
        }

        void getAllNodeReplacements(Map<String, ElementNode> result) {
            if (includes != null && includes.length > 0) {
                for (String include : includes) {
                    Supplement parent = supplementReplacements.get(include);
                    if (parent == null) {
                        throw new IllegalStateException("Can't find included supplement '" + include + "'");
                    }
                    parent.getAllNodeReplacements(result);
                }
            }
            result.putAll(nodeReplacements);
        }

        Map<String, String> getAllAttributeReplacements() {
            Map<String, String> result = new HashMap<String, String>();
            getAllAttributeReplacements(result);
            return result;
        }

        void getAllAttributeReplacements(Map<String, String> result) {
            if (includes != null && includes.length > 0) {
                for (String include : includes) {
                    Supplement parent = supplementReplacements.get(include);
                    if (parent == null) {
                        throw new IllegalStateException("Can't find included supplement '" + include + "'");
                    }
                    parent.getAllAttributeReplacements(result);
                }
            }
            result.putAll(attributeReplacements);
        }
}
}
