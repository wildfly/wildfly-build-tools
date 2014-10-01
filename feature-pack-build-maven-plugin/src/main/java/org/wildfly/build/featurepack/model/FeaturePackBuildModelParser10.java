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
import org.wildfly.build.common.model.ConfigModelParser10;
import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.common.model.FileFilterModelParser10;
import org.wildfly.build.common.model.FilePermissionsModelParser10;
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

    public static final String NAMESPACE_1_0 = "urn:wildfly:feature-pack-build:1.0";

    enum Element {

        // default unknown element
        UNKNOWN(null),

        BUILD("build"),
        DEPENDENCIES("dependencies"),
        ARTIFACT("artifact"),
        CONFIG(ConfigModelParser10.ELEMENT_LOCAL_NAME),
        COPY_ARTIFACTS(CopyArtifactsModelParser10.ELEMENT_LOCAL_NAME),
        FILTER(FileFilterModelParser10.ELEMENT_LOCAL_NAME),
        FILE_PERMISSIONS(FilePermissionsModelParser10.ELEMENT_LOCAL_NAME),
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
            elementsMap.put(new QName(NAMESPACE_1_0, Element.COPY_ARTIFACTS.getLocalName()), Element.COPY_ARTIFACTS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILTER.getLocalName()), Element.FILTER);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FILE_PERMISSIONS.getLocalName()), Element.FILE_PERMISSIONS);
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
        ;

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
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

    private final BuildPropertyReplacer propertyReplacer;
    private final ConfigModelParser10 configModelParser;
    private final CopyArtifactsModelParser10 copyArtifactsModelParser;
    private final FileFilterModelParser10 fileFilterModelParser;
    private final FilePermissionsModelParser10 filePermissionsModelParser;

    FeaturePackBuildModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
        this.configModelParser = new ConfigModelParser10(this.propertyReplacer);
        this.fileFilterModelParser = new FileFilterModelParser10(this.propertyReplacer);
        this.copyArtifactsModelParser = new CopyArtifactsModelParser10(this.propertyReplacer, this.fileFilterModelParser);
        this.filePermissionsModelParser = new FilePermissionsModelParser10(this.propertyReplacer, this.fileFilterModelParser);
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
                            configModelParser.parseConfig(reader, result.getConfig());
                            break;
                        case COPY_ARTIFACTS:
                            copyArtifactsModelParser.parseCopyArtifacts(reader, result.getCopyArtifacts());
                            break;
                        case FILE_PERMISSIONS:
                            filePermissionsModelParser.parseFilePermissions(reader, result.getFilePermissions());
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
                            fileFilterModelParser.parseFilter(reader, result);
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
