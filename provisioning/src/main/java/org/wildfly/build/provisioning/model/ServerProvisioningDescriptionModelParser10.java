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

package org.wildfly.build.provisioning.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.PropertyResolver;
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
 * Parses the distribution build config file, i.e. the config file that is
 * used to create a wildfly distribution.
 *
 * @author Eduardo Martins
 */
class ServerProvisioningDescriptionModelParser10 implements XMLElementReader<ServerProvisioningDescription> {


    private final BuildPropertyReplacer propertyReplacer;

    public static final String NAMESPACE_1_0 = "urn:wildfly:server-provisioning:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        SERVER_PROVISIONING("server-provisioning"),
        FEATURE_PACKS("feature-packs"),
        ARTIFACT("artifact"),
        VERSION_OVERRIDES("version-overrides"),
        VERSION_OVERRIDE("version-override"),
        ;


        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.SERVER_PROVISIONING.getLocalName()), Element.SERVER_PROVISIONING);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACKS.getLocalName()), Element.FEATURE_PACKS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.ARTIFACT.getLocalName()), Element.ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.VERSION_OVERRIDES.getLocalName()), Element.VERSION_OVERRIDES);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.VERSION_OVERRIDE.getLocalName()), Element.VERSION_OVERRIDE);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
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

        COPY_MODULE_ARTIFACTS("copy-module-artifacts"),
        EXTRACT_SCHEMAS("extract-schemas"),
        NAME("name"),
        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        EXTENSION("extension"),
        VERSION("version")
        ;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
            attributesMap.put(new QName(COPY_MODULE_ARTIFACTS.getLocalName()), COPY_MODULE_ARTIFACTS);
            attributesMap.put(new QName(EXTRACT_SCHEMAS.getLocalName()), EXTRACT_SCHEMAS);
            attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
            attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
            attributesMap.put(new QName(CLASSIFIER.getLocalName()), CLASSIFIER);
            attributesMap.put(new QName(EXTENSION.getLocalName()), EXTENSION);
            attributesMap.put(new QName(VERSION.getLocalName()), VERSION);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
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

    ServerProvisioningDescriptionModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        final Set<Attribute> required = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case COPY_MODULE_ARTIFACTS:
                    result.setCopyModuleArtifacts(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                case EXTRACT_SCHEMAS:
                    result.setExtractSchemas(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());

                    switch (element) {
                        case FEATURE_PACKS:
                            parseFeaturePacks(reader, result);
                            break;
                        case VERSION_OVERRIDES:
                            parseVersionOverrides(reader, result);
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

    private void parseFeaturePacks(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ARTIFACT:
                            parseArtifact(reader, result.getFeaturePacks(), "zip"); //feature packs default to zip
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


    private void parseVersionOverrides(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case VERSION_OVERRIDE:
                            parseArtifact(reader, result.getVersionOverrides(), null);
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


    private void parseArtifact(final XMLStreamReader reader, List<Artifact> overrides, String defaultExtension) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String artifact = null;
        String version = null;
        String groupId = null;
        String classifier = null;
        String extension = defaultExtension;
        final Set<Attribute> required = EnumSet.of(Attribute.ARTIFACT_ID, Attribute.VERSION, Attribute.GROUP_ID);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case GROUP_ID:
                    groupId = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case ARTIFACT_ID:
                    artifact = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
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

        overrides.add(new Artifact(groupId, artifact, classifier, extension, version));
    }
}
