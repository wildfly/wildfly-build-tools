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
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.NodeParser;
import org.wildfly.build.util.xml.ParsingUtils;
import org.wildfly.build.util.xml.ProcessingInstructionNode;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */
public class TemplateParser extends NodeParser {

    private static final String SOCKET_BINDINGS_PI = "SOCKET-BINDINGS";
    private static final String EXTENSIONS_PI = "EXTENSIONS";
    private static final String SUBSYSTEMS_PI = "SUBSYSTEMS";
    private static final String INTERFACES_PI = "INTERFACES";

    private final InputStreamSource inputStreamSource;
    private final String rootElementName;
    private ElementNode root;
    private ProcessingInstructionNode extensionPlaceholder;
    private final Map<String, ProcessingInstructionNode> subsystemPlaceHolders = new HashMap<String, ProcessingInstructionNode>();
    private final Map<String, ProcessingInstructionNode> socketBindingsPlaceHolder = new HashMap<String, ProcessingInstructionNode>();
    private ProcessingInstructionNode interfacesPlaceHolder;

    public TemplateParser(InputStreamSource inputStreamSource, String rootElementName) {
        this.inputStreamSource = inputStreamSource;
        this.rootElementName = rootElementName;
    }

    public ElementNode getRootNode() {
        return root;
    }

    public ProcessingInstructionNode getExtensionPlaceHolder() {
        return extensionPlaceholder;
    }

    public Map<String, ProcessingInstructionNode> getSubsystemPlaceholders(){
        return subsystemPlaceHolders;
    }

    public Map<String, ProcessingInstructionNode> getSocketBindingsPlaceHolders() {
        return socketBindingsPlaceHolder;
    }

    public ProcessingInstructionNode getInterfacesPlaceHolders() {
        return interfacesPlaceHolder;
    }

    public void parse() throws IOException, XMLStreamException {
        try (InputStream in = inputStreamSource.getInputStream()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XMLStreamReader reader = factory.createXMLStreamReader(in);

            reader.require(START_DOCUMENT, null, null);
            ParsingUtils.getNextElement(reader, rootElementName, null, false);
            root = super.parseNode(reader, rootElementName);

        }
    }

    @Override
    protected ProcessingInstructionNode parseProcessingInstruction(XMLStreamReader reader, ElementNode parent) throws XMLStreamException {
        ProcessingInstructionNode node = null;
        String pi = reader.getPITarget();
        Map<String, String> data = parseProcessingInstructionData(reader.getPIData());
        if (pi.equals(TemplateParser.EXTENSIONS_PI)) {
            if (!data.isEmpty()) {
                throw new IllegalStateException("<?" + TemplateParser.EXTENSIONS_PI + "?> should not take any data");
            }
            if (extensionPlaceholder != null) {
                throw new IllegalStateException("Can only have one occurrence of <?" + TemplateParser.EXTENSIONS_PI + "?>");
            }
            node = new ProcessingInstructionNode(TemplateParser.EXTENSIONS_PI, null);
            extensionPlaceholder = node;
        } else if (pi.equals(TemplateParser.SUBSYSTEMS_PI)) {
            if (!parent.getName().equals("profile")) {
                throw new IllegalStateException("<?" + TemplateParser.SUBSYSTEMS_PI + "?> must be a child of <profile> " + reader.getLocation());
            }
            if (data.size() == 0 || !data.containsKey("socket-binding-group")) {
                throw new IllegalStateException("Must have 'socket-binding-group' as <?" + TemplateParser.SUBSYSTEMS_PI + "?> data");
            }
            if (data.size() > 1) {
                throw new IllegalStateException("Only 'socket-binding-group' is valid <?" + TemplateParser.SUBSYSTEMS_PI + "?> data");
            }
            String profileName = parent.getAttributeValue("name", "");
            node = new ProcessingInstructionNode(profileName, data);
            subsystemPlaceHolders.put(profileName, node);
        } else if (pi.equals(TemplateParser.SOCKET_BINDINGS_PI)) {
            if (!parent.getName().equals("socket-binding-group")) {
                throw new IllegalStateException("<?" + TemplateParser.SOCKET_BINDINGS_PI + "?> must be a child of <socket-binding-group> " + reader.getLocation());
            }
            if (!data.isEmpty()) {
                throw new IllegalStateException("<?" + TemplateParser.SOCKET_BINDINGS_PI + "?> should not take any data");
            }
            String groupName = parent.getAttributeValue("name", "");
            node = new ProcessingInstructionNode(TemplateParser.SOCKET_BINDINGS_PI, data);
            socketBindingsPlaceHolder.put(groupName, node);
        } else if (pi.equals(TemplateParser.INTERFACES_PI)) {
            if (!parent.getName().equals("interfaces")) {
                throw new IllegalStateException("<?" + TemplateParser.INTERFACES_PI + "?> must be a child of <interfaces> " + reader.getLocation());
            }
            if (!data.isEmpty()) {
                throw new IllegalStateException("<?" + TemplateParser.INTERFACES_PI + "?> should not take any data");
            }
            if (interfacesPlaceHolder!= null) {
                throw new IllegalStateException("Can only have one occurrence of <?" + TemplateParser.INTERFACES_PI + "?>");
            }
            node = new ProcessingInstructionNode(TemplateParser.INTERFACES_PI, data);
            interfacesPlaceHolder = node;
        } else {
            throw new IllegalStateException("Unknown processing instruction <?" + reader.getPITarget() + "?>" + reader.getLocation());
        }
        return node;
    }
}
