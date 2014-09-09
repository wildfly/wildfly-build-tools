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

package org.wildfly.build.featurepack.model;

import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.pack.model.Config;
import org.wildfly.build.pack.model.ConfigFile;
import org.wildfly.build.pack.model.CopyArtifact;
import org.wildfly.build.pack.model.FileFilter;
import org.wildfly.build.pack.model.FilePermission;
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
class FeaturePackBuildModelParser10 implements XMLElementReader<FeaturePackBuild> {


    private final BuildPropertyReplacer propertyReplacer;

    public static final String NAMESPACE_1_0 = "urn:wildfly:feature-pack-build:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        BUILD("build"),
        DEPENDENCIES("dependencies"),
        ARTIFACT("artifact"),
        CONFIG("config"),
        STANDALONE("standalone"),
        DOMAIN("domain"),
        PROPERTY("property"),
        COPY_ARTIFACTS("copy-artifacts"),
        COPY_ARTIFACT("copy-artifact"),
        FILTER("filter"),
        FILE_PERMISSIONS("file-permissions"),
        PERMISSION("permission"),
        MKDIRS("mkdirs"),
        DIR("dir"),
        LINE_ENDINGS("line-endings"),
        WINDOWS("windows"),
        UNIX("unix");

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.BUILD.getLocalName()), Element.BUILD);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.DEPENDENCIES.getLocalName()), Element.DEPENDENCIES);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.ARTIFACT.getLocalName()), Element.ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.CONFIG.getLocalName()), Element.CONFIG);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.STANDALONE.getLocalName()), Element.STANDALONE);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.DOMAIN.getLocalName()), Element.DOMAIN);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PROPERTY.getLocalName()), Element.PROPERTY);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACTS.getLocalName()), Element.COPY_ARTIFACTS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACT.getLocalName()), Element.COPY_ARTIFACT);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILTER.getLocalName()), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILE_PERMISSIONS.getLocalName()), Element.FILE_PERMISSIONS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PERMISSION.getLocalName()), Element.PERMISSION);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.MKDIRS.getLocalName()), Element.MKDIRS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.DIR.getLocalName()), Element.DIR);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.LINE_ENDINGS.getLocalName()), Element.LINE_ENDINGS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.WINDOWS.getLocalName()), Element.WINDOWS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.UNIX.getLocalName()), Element.UNIX);
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

    FeaturePackBuildModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {

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
                        case CONFIG:
                            parseConfig(reader, result.getConfig());
                            break;
                        case COPY_ARTIFACTS:
                            parseCopyArtifacts(reader, result);
                            break;
                        case FILE_PERMISSIONS:
                            parseFilePermissions(reader, result);
                            break;
                        case MKDIRS:
                            parseMkdirs(reader, result);
                            break;
                        case LINE_ENDINGS:
                            parseLineEndings(reader, result);
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

    private void parseDependencies(final XMLStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {
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

    private void parseCopyArtifacts(final XMLStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {
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

    private void parseCopyArtifact(XMLStreamReader reader, FeaturePackBuild result) throws XMLStreamException {
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

        filters.add(new FileFilter(wildcardToJavaRegexp(pattern), include));
    }

    private static String wildcardToJavaRegexp(String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }

    private void parseFilePermissions(final XMLStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {
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

    private void parsePermission(XMLStreamReader reader, FeaturePackBuild result) throws XMLStreamException {
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

    private void parseMkdirs(final XMLStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DIR:
                            result.getMkDirs().add(parseName(reader));
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

    private void parseLineEndings(final XMLStreamReader reader, final FeaturePackBuild result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case WINDOWS:
                            parseLineEnding(reader, result.getWindows());
                            break;
                        case UNIX:
                            parseLineEnding(reader, result.getUnix());
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

    private void parseLineEnding(XMLStreamReader reader, List<FileFilter> result) throws XMLStreamException {
        if(reader.getAttributeCount() != 0) {
            throw ParsingUtils.unexpectedContent(reader);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseFilter(reader, result);
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
