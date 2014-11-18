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
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.common.model.FileFilterModelParser10;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.configassembly.SubsystemsParser;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
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

    public static final String NAMESPACE_1_0 = "urn:wildfly:server-provisioning:1.0";

    private final BuildPropertyReplacer propertyReplacer;
    private final CopyArtifactsModelParser10 copyArtifactsModelParser;
    private final FileFilterModelParser10 fileFilterModelParser;

    ServerProvisioningDescriptionModelParser10(PropertyResolver resolver) {
        this.propertyReplacer = new BuildPropertyReplacer(resolver);
        this.fileFilterModelParser = new FileFilterModelParser10(propertyReplacer);
        this.copyArtifactsModelParser = new CopyArtifactsModelParser10(this.propertyReplacer, this.fileFilterModelParser);
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
                        case COPY_ARTIFACTS:
                            copyArtifactsModelParser.parseCopyArtifacts(reader, result.getCopyArtifacts());
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
                        case FEATURE_PACK:
                            parseFeaturePack(reader, result);
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

    private void parseFeaturePack(final XMLStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        Artifact artifact = parseArtifact(reader, "zip", false);
        ServerProvisioningDescription.FeaturePack.ModuleFilters moduleFilters = null;
        ConfigOverride config = null;
        ServerProvisioningDescription.FeaturePack.ContentFilters contentFilters = null;
        List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    result.getFeaturePacks().add(new ServerProvisioningDescription.FeaturePack(artifact, moduleFilters, config, contentFilters, subsystems));
                    result.getVersionOverrides().add(artifact);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULES:
                            moduleFilters = parseModules(reader);
                            break;
                        case CONFIG:
                            if (subsystems != null) {
                                throw new XMLStreamException("server provisioning xml specifies a feature pack filtered by config and subsystems");
                            }
                            config = new ConfigOverride();
                            parseConfig(reader, config);
                            break;
                        case CONTENTS:
                            contentFilters = parseContents(reader);
                            break;
                        case SUBSYSTEMS:
                            if (config != null) {
                                throw new XMLStreamException("server provisioning xml specifies a feature pack filtered by config and subsystems");
                            }
                            subsystems = new ArrayList<>();
                            parseSubsystems(reader, subsystems);
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

    private ServerProvisioningDescription.FeaturePack.ContentFilters parseContents(XMLStreamReader reader) throws XMLStreamException {
        boolean include = true;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        ServerProvisioningDescription.FeaturePack.ContentFilters result = new ServerProvisioningDescription.FeaturePack.ContentFilters(include);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return result;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            fileFilterModelParser.parseFilter(reader, result.getFilters());
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

    private ServerProvisioningDescription.FeaturePack.ModuleFilters parseModules(final XMLStreamReader reader) throws XMLStreamException {
        boolean include = true;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case INCLUDE:
                    include = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        ServerProvisioningDescription.FeaturePack.ModuleFilters result = new ServerProvisioningDescription.FeaturePack.ModuleFilters(include);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return result;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FILTER:
                            parseModuleFilter(reader, result.getFilters());
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

    private void parseModuleFilter(final XMLStreamReader reader, List<ModuleFilter> result) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
        boolean transitive = true;
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
                case TRANSITIVE:
                    transitive = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        ParsingUtils.parseNoContent(reader);

        result.add(new ModuleFilter(pattern, include, transitive));
    }

    public void parseConfig(final XMLStreamReader reader, ConfigOverride result) throws XMLStreamException {
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

    private void parseConfigFile(XMLStreamReader reader, Map<String, ConfigFileOverride> result) throws XMLStreamException {
        boolean useTemplate = false;
        String outputFile = null;
        final Set<Attribute> required = EnumSet.of(Attribute.OUTPUT_FILE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case USE_TEMPLATE:
                    useTemplate = Boolean.parseBoolean(reader.getAttributeValue(i));
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
        final Map<String, String> properties = new HashMap<>();
        final BuildPropertyReplacer subystemsParserPropertyReplacer = new BuildPropertyReplacer(new MapPropertyResolver(properties));
        Map<String, Map<String, SubsystemConfig>> subsystems = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    result.put(outputFile, new ConfigFileOverride(properties, useTemplate, subsystems, outputFile));
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PROPERTY:
                            parseProperty(reader, properties);
                            break;
                        case SUBSYSTEMS:
                            if (subsystems == null) {
                                subsystems = new HashMap<>();
                            }
                            SubsystemsParser.parseSubsystems(reader, subystemsParserPropertyReplacer, subsystems);
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

    private void parseSubsystems(final XMLStreamReader reader, List<ServerProvisioningDescription.FeaturePack.Subsystem> result) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case SUBSYSTEM:
                            result.add(parseSubsystem(reader));
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

    private ServerProvisioningDescription.FeaturePack.Subsystem parseSubsystem(final XMLStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        boolean transitive = false;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case TRANSITIVE:
                    transitive = Boolean.parseBoolean(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);

        return new ServerProvisioningDescription.FeaturePack.Subsystem(name, transitive);
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
                            result.getVersionOverrides().add(parseArtifact(reader, null, true));
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

    private Artifact parseArtifact(final XMLStreamReader reader, String defaultExtension, boolean parseNoContent) throws XMLStreamException {
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
        if (parseNoContent) {
            ParsingUtils.parseNoContent(reader);
        }

        return new Artifact(groupId, artifact, classifier, extension, version);
    }
}
