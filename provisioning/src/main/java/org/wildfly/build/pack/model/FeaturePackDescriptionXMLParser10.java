/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.pack.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
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
 * Parses the feature pack build config file (i.e. the config file that is
 * used to create a feature pack, not the config file inside the feature pack).
 *
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
class FeaturePackDescriptionXMLParser10 implements XMLElementReader<FeaturePackDescription> {

    private final BuildPropertyReplacer propertyReplacer;

    public static final String NAMESPACE_1_0 = "urn:wildfly:feature-pack:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        FEATURE_PACK("feature-pack"),
        DEPENDENCIES("dependencies"),
        ARTIFACT("artifact"),
        ARTIFACT_VERSIONS("artifact-versions"),
        CONFIG("config"),
        STANDALONE("standalone"),
        DOMAIN("domain"),
        PROPERTY("property"),
        COPY_ARTIFACTS("copy-artifacts"),
        COPY_ARTIFACT("copy-artifact"),
        FILTER("filter"),
        FILE_PERMISSIONS("file-permissions"),
        PERMISSION("permission");

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACK.getLocalName()), Element.FEATURE_PACK);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.DEPENDENCIES.getLocalName()), Element.DEPENDENCIES);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.ARTIFACT.getLocalName()), Element.ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.ARTIFACT_VERSIONS.getLocalName()), Element.ARTIFACT_VERSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.CONFIG.getLocalName()), Element.CONFIG);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.STANDALONE.getLocalName()), Element.STANDALONE);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.DOMAIN.getLocalName()), Element.DOMAIN);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PROPERTY.getLocalName()), Element.PROPERTY);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACTS.getLocalName()), Element.COPY_ARTIFACTS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACT.getLocalName()), Element.COPY_ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILTER.getLocalName()), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILE_PERMISSIONS.getLocalName()), Element.FILE_PERMISSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PERMISSION.getLocalName()), Element.PERMISSION);
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
        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        EXTENSION("extension"),
        VERSION("version"),
        NAME("name"),
        VALUE("value"),
        TEMPLATE("template"),
        SUBSYSTEMS("subsystems"),
        OUTPUT_FILE("output-file"),
        ARTIFACT("artifact"),
        TO_LOCATION("to-location"),
        EXTRACT("extract"),
        PATTERN("pattern"),
        INCLUDE("include");

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
            attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
            attributesMap.put(new QName(CLASSIFIER.getLocalName()), CLASSIFIER);
            attributesMap.put(new QName(EXTENSION.getLocalName()), EXTENSION);
            attributesMap.put(new QName(VERSION.getLocalName()), VERSION);
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
            attributesMap.put(new QName(TEMPLATE.getLocalName()), TEMPLATE);
            attributesMap.put(new QName(SUBSYSTEMS.getLocalName()), SUBSYSTEMS);
            attributesMap.put(new QName(OUTPUT_FILE.getLocalName()), OUTPUT_FILE);
            attributesMap.put(new QName(VALUE.getLocalName()), VALUE);
            attributesMap.put(new QName(ARTIFACT.getLocalName()), ARTIFACT);
            attributesMap.put(new QName(TO_LOCATION.getLocalName()), TO_LOCATION);
            attributesMap.put(new QName(EXTRACT.getLocalName()), EXTRACT);
            attributesMap.put(new QName(PATTERN.getLocalName()), PATTERN);
            attributesMap.put(new QName(INCLUDE.getLocalName()), INCLUDE);
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

    FeaturePackDescriptionXMLParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final FeaturePackDescription result) throws XMLStreamException {

        final Set<Attribute> required = EnumSet.noneOf(Attribute.class);
        final int count = reader.getAttributeCount();

        for (int i = 0; i < count; i++) {
                    throw ParsingUtils.unexpectedContent(reader);
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
                        case DEPENDENCIES:
                            parseDependencies(reader, result);
                            break;
                        case ARTIFACT_VERSIONS:
                            parseArtifactVersions(reader, result);
                            break;
                        case CONFIG:
                            parseConfig(reader, result.getConfig());
                            break;
                        case COPY_ARTIFACTS:
                            parseCopyArtifacts(reader, result);
                            break;
                        case FILE_PERMISSIONS:
                            parseFilePermissions(reader, result);
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

    private void parseDependencies(final XMLStreamReader reader, final FeaturePackDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ARTIFACT:
                            result.getDependencies().add(parseName(reader));
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

    private String parseName(final XMLStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);
        return propertyReplacer.replaceProperties(name);
    }

    private void parseArtifactVersions(final XMLStreamReader reader, final FeaturePackDescription result) throws XMLStreamException {
        final Set<Artifact> artifactVersions = result.getArtifactVersions();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ARTIFACT:
                            artifactVersions.add(parseArtifact(reader));
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

    private Artifact parseArtifact(final XMLStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String groupId = null;
        String artifactId = null;
        String version = null;
        String classifier = null;
        String extension = null;
        final Set<Attribute> required = EnumSet.of(Attribute.GROUP_ID, Attribute.ARTIFACT_ID, Attribute.VERSION);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
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
        return new Artifact(groupId, artifactId, classifier, extension, version);
    }

    private void parseConfig(final XMLStreamReader reader, final Config result) throws XMLStreamException {
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


    private void parseCopyArtifacts(final XMLStreamReader reader, final FeaturePackDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case COPY_ARTIFACT:
                            parseCopyArtifact(reader, result);
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

    private void parseCopyArtifact(XMLStreamReader reader, FeaturePackDescription result) throws XMLStreamException {
        String artifact = null;
        String location = null;
        boolean extract = false;
        final Set<Attribute> required = EnumSet.of(Attribute.ARTIFACT, Attribute.TO_LOCATION);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case ARTIFACT:
                    artifact = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case TO_LOCATION:
                    location = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case EXTRACT:
                    extract = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        CopyArtifact copyArtifact = new CopyArtifact(artifact, location, extract);
        result.getCopyArtifacts().add(copyArtifact);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, copyArtifact.getFilters());
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

    private void parseFilter(XMLStreamReader reader, List<FileFilter> filters) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
        final Set<Attribute> required = EnumSet.of(Attribute.PATTERN, Attribute.INCLUDE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATTERN:
                    pattern = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        ParsingUtils.parseNoContent(reader);

        filters.add(new FileFilter(pattern, include));
    }

    private void parseFilePermissions(final XMLStreamReader reader, final FeaturePackDescription result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PERMISSION:
                            parsePermission(reader, result);
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

    private void parsePermission(XMLStreamReader reader, FeaturePackDescription result) throws XMLStreamException {
        String permission = null;
        final Set<Attribute> required = EnumSet.of(Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case VALUE:
                    permission = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        FilePermission filePermission = new FilePermission(permission);
        result.getFilePermissions().add(filePermission);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, filePermission.getFilters());
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

}
