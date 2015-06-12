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

import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Martins
 */
public class ConfigModelParser11 {

    public static final String ELEMENT_LOCAL_NAME = "config";

    enum Element {

        // default unknown element
        UNKNOWN(null),
        STANDALONE("standalone"),
        DOMAIN("domain"),
        PROPERTY("property"),
        HOST("host"),
        ;

        private static final Map<String, Element> elements;

        static {
            Map<String, Element> elementsMap = new HashMap<>();
            elementsMap.put(Element.STANDALONE.getLocalName(), Element.STANDALONE);
            elementsMap.put(Element.DOMAIN.getLocalName(), Element.DOMAIN);
            elementsMap.put(Element.PROPERTY.getLocalName(), Element.PROPERTY);
            elementsMap.put(Element.HOST.getLocalName(), Element.HOST);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            final Element element = elements.get(qName.getLocalPart());
            return element == null ? UNKNOWN : element;
        }

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    enum Attribute {

        // default unknown attribute
        UNKNOWN(null),
        TEMPLATE("template"),
        SUBSYSTEMS("subsystems"),
        OUTPUT_FILE("output-file"),
        NAME("name"),
        VALUE("value"),
        ;

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(TEMPLATE.getLocalName(), TEMPLATE);
            attributesMap.put(SUBSYSTEMS.getLocalName(), SUBSYSTEMS);
            attributesMap.put(OUTPUT_FILE.getLocalName(), OUTPUT_FILE);
            attributesMap.put(NAME.getLocalName(), NAME);
            attributesMap.put(VALUE.getLocalName(), VALUE);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName.getLocalPart());
            return attribute == null ? UNKNOWN : attribute;
        }

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    private final BuildPropertyReplacer propertyReplacer;

    public ConfigModelParser11(BuildPropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    public void parseConfig(final XMLStreamReader reader, final Config result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case STANDALONE:
                            parseConfigFile(reader, result.getStandaloneConfigFiles());
                            break;
                        case DOMAIN:
                            parseConfigFile(reader, result.getDomainConfigFiles());
                            break;
                        case HOST:
                            parseConfigFile(reader, result.getHostConfigFiles());
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private void parseConfigFile(XMLStreamReader reader, List<ConfigFile> result) throws XMLStreamException {
        final Map<String, String> properties = new HashMap<>();
        String template = null;
        String subsystems = null;
        String outputFile = null;
        final Set<Attribute> required = EnumSet.of(Attribute.TEMPLATE, Attribute.SUBSYSTEMS, Attribute.OUTPUT_FILE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case TEMPLATE:
                    template = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case SUBSYSTEMS:
                    subsystems = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case OUTPUT_FILE:
                    outputFile = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        final ConfigFile configFile = new ConfigFile(properties, template, subsystems, outputFile);
        result.add(configFile);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PROPERTY:
                            parseProperty(reader, properties);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private void parseProperty(XMLStreamReader reader, Map<String, String> result) throws XMLStreamException {
        String name = null;
        String value = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VALUE:
                    value = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);
        result.put(name, value);
    }

}
