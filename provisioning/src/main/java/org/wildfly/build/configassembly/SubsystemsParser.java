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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemsParser {

    static String NAMESPACE = "urn:subsystems-config:1.0";
    private final InputStreamSource inputStreamSource;
    private final BuildPropertyReplacer propertyReplacer;
    Map<String, SubsystemConfig[]> subsystemConfigs = new HashMap<String, SubsystemConfig[]>();

    SubsystemsParser(final InputStreamSource inputStreamSource, BuildPropertyReplacer propertyReplacer) {
        this.inputStreamSource = inputStreamSource;
        this.propertyReplacer = propertyReplacer;
    }

    SubsystemsParser(final InputStreamSource inputStreamSource) {
        this(inputStreamSource, new BuildPropertyReplacer(PropertyResolver.NO_OP));
    }


    Map<String, SubsystemConfig[]> getSubsystemConfigs() {
        return subsystemConfigs;
    }

    void parse() throws IOException, XMLStreamException {
        try (InputStream in = inputStreamSource.getInputStream()) {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            reader.require(START_DOCUMENT, null, null);
            boolean done = false;
            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                case START_ELEMENT:
                    if (!done && reader.getLocalName().equals("config")) {
                        parseSubsystems(reader);
                        done = true;
                    } else {
                        throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_DOCUMENT:
                    return;
                }
            }

        }
    }

    private void parseSubsystems(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
            case START_ELEMENT:
                if (reader.getLocalName().equals("subsystems")) {
                    String name = "";
                    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                        if (!reader.getAttributeLocalName(i).equals("name")) {
                            throw new XMLStreamException("Unknown attribute " + reader.getAttributeLocalName(i), reader.getLocation());
                        } else {
                            name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                        }
                    }
                    if (subsystemConfigs.containsKey(name)) {
                        throw new XMLStreamException("Already have a subsystems named " + name, reader.getLocation());
                    }
                    List<SubsystemConfig> subsystems = new ArrayList<SubsystemConfig>();
                    parseSubsystem(reader, subsystems);
                    this.subsystemConfigs.put(name, subsystems.toArray(new SubsystemConfig[subsystems.size()]));
                } else {
                    throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("config")) {
                    return;
                }
            }
        }
    }

    private void parseSubsystem(XMLStreamReader reader, List<SubsystemConfig> subsystems) throws XMLStreamException {
        reader.next();
        while (true) {
            switch (reader.getEventType()) {
                case START_ELEMENT:
                    if (reader.getLocalName().equals("subsystem")) {
                        String supplement = null;
                        String systemProperty = null;
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            String attr = reader.getAttributeLocalName(i);
                            if (attr.equals("supplement")) {
                                supplement = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                            } else if (attr.equals("include-if-set")) {
                                systemProperty = reader.getAttributeValue(i);
                            } else {
                                throw new XMLStreamException("Unknown attribute " + attr, reader.getLocation());
                            }

                        }
                        if(supplement != null && supplement.isEmpty()) {
                            supplement = null;
                        }
                        String text = reader.getElementText();
                        if (systemProperty != null) {
                            if (System.getProperty(systemProperty) != null) {
                                subsystems.add(new SubsystemConfig(text, supplement));
                            }
                        } else {
                            subsystems.add(new SubsystemConfig(text, supplement));
                        }
                    } else {
                        throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_ELEMENT:
                    if (reader.getLocalName().equals("subsystems")) {
                        return;
                    }
                default:
                    reader.next();
            }
        }
    }
}
