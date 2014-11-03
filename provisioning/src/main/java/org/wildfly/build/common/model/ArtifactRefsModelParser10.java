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

import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Martins
 */
public class ArtifactRefsModelParser10 {

    public static final String ELEMENT_LOCAL_NAME = "artifact-refs";

    enum Element {

        // default unknown element
        UNKNOWN(null),
        ARTIFACT("artifact"),
        ;

        private static final Map<String, Element> elements;

        static {
            Map<String, Element> elementsMap = new HashMap<>();
            elementsMap.put(Element.ARTIFACT.getLocalName(), Element.ARTIFACT);
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

        UNKNOWN(null),
        NAME("name"),
        // maven artifact attrs
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        EXTENSION("extension"),
        GROUP_ID("groupId"),
        VERSION("version"),
        ;

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(NAME.getLocalName(), NAME);
            attributesMap.put(ARTIFACT_ID.getLocalName(), ARTIFACT_ID);
            attributesMap.put(CLASSIFIER.getLocalName(), CLASSIFIER);
            attributesMap.put(EXTENSION.getLocalName(), EXTENSION);
            attributesMap.put(GROUP_ID.getLocalName(), GROUP_ID);
            attributesMap.put(VERSION.getLocalName(), VERSION);
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

    public ArtifactRefsModelParser10(BuildPropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    public void parseArtifactRefs(final XMLStreamReader reader, final Map<String, Artifact> result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ARTIFACT:
                            parseArtifact(reader, result);
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

    private void parseArtifact(XMLStreamReader reader, final Map<String, Artifact> result) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String artifactId = null;
        String version = null;
        String groupId = null;
        String classifier = null;
        String extension = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.ARTIFACT_ID, Attribute.GROUP_ID, Attribute.VERSION);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case GROUP_ID:
                    groupId = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case ARTIFACT_ID:
                    artifactId = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VERSION:
                    version = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case CLASSIFIER:
                    classifier = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case EXTENSION:
                    extension = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);
        result.put(name, new Artifact(groupId, artifactId, extension, classifier, version));
    }
}
