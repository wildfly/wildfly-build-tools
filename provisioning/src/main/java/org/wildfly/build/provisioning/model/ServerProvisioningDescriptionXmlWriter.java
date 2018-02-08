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

import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.wildfly.build.common.model.ConfigFileOverride;
import org.wildfly.build.common.model.ConfigOverride;
import org.wildfly.build.common.model.CopyArtifactsXMLWriter10;
import org.wildfly.build.common.model.FileFilter;
import org.wildfly.build.common.model.FileFilterXMLWriter10;
import org.wildfly.build.configassembly.SubsystemConfig;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.TextNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Stuart Douglas
 * @author emmartins
 */
public class ServerProvisioningDescriptionXmlWriter implements XMLElementWriter<ServerProvisioningDescription> {

    public static ServerProvisioningDescriptionXmlWriter INSTANCE = new ServerProvisioningDescriptionXmlWriter();


    public void writeContent(XMLStreamWriter streamWriter, ServerProvisioningDescription value) throws XMLStreamException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.deparseDocument(this, value, streamWriter);
    }

    public void writeContent(File file, ServerProvisioningDescription value) throws XMLStreamException, IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            try {
                writeContent(writer, value);
            } finally {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter, ServerProvisioningDescription description) throws XMLStreamException {
        // build node tree
        final ElementNode rootElementNode = new ElementNode(null, Element.SERVER_PROVISIONING.getLocalName(), ServerProvisioningDescriptionModelParser11.NAMESPACE_1_1);
        if (description.isCopyModuleArtifacts()) {
            rootElementNode.addAttribute(Attribute.COPY_MODULE_ARTIFACTS.getLocalName(), new AttributeValue(Boolean.TRUE.toString()));
        }
        if (description.isExtractSchemas()) {
            rootElementNode.addAttribute(Attribute.EXTRACT_SCHEMAS.getLocalName(), new AttributeValue(Boolean.TRUE.toString()));
            rootElementNode.addAttribute(Attribute.EXTRACT_SCHEMAS_GROUPS.getLocalName(), new AttributeValue(description.getExtractSchemasGroupsAsString()));
        }

        processFeaturePacks(description.getFeaturePacks(), rootElementNode);
        processVersionOverrides(description.getVersionOverrides(), rootElementNode);
        CopyArtifactsXMLWriter10.INSTANCE.write(description.getCopyArtifacts(), rootElementNode);
        // write the xml
        streamWriter.writeStartDocument();
        rootElementNode.marshall(streamWriter);
        streamWriter.writeEndDocument();
    }

    protected void processFeaturePacks(List<ServerProvisioningDescription.FeaturePack> featurePacks, ElementNode parentElementNode) {
        if (!featurePacks.isEmpty()) {
            final ElementNode featurePacksElementNode = new ElementNode(parentElementNode, Element.FEATURE_PACKS.getLocalName());
            for (ServerProvisioningDescription.FeaturePack featurePack : featurePacks) {
                processFeaturePack(featurePack, featurePacksElementNode);
            }
            parentElementNode.addChild(featurePacksElementNode);
        }
    }

    protected void processFeaturePack(ServerProvisioningDescription.FeaturePack featurePack, ElementNode parentElementNode) {
        final ElementNode featurePackElementNode = new ElementNode(parentElementNode, Element.FEATURE_PACK.getLocalName());
        featurePackElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(featurePack.getArtifact().getGroupId()));
        featurePackElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(featurePack.getArtifact().getArtifactId()));
        featurePackElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(featurePack.getArtifact().getVersion()));
        if (featurePack.getArtifact().getClassifier() != null) {
            featurePackElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(featurePack.getArtifact().getClassifier()));
        }
        processFeaturePackModuleFilters(featurePack, featurePackElementNode);
        processFeaturePackSubsystems(featurePack, featurePackElementNode);
        processFeaturePackConfigOverride(featurePack, featurePackElementNode);
        processFeaturePackContentFilters(featurePack, featurePackElementNode);
        parentElementNode.addChild(featurePackElementNode);
    }

    protected void processFeaturePackModuleFilters(ServerProvisioningDescription.FeaturePack featurePack, ElementNode featurePackElementNode) {
        final ServerProvisioningDescription.FeaturePack.ModuleFilters moduleFilters = featurePack.getModuleFilters();
        if (moduleFilters != null) {
            final ElementNode moduleFiltersElementNode = new ElementNode(featurePackElementNode, Element.MODULES.getLocalName());
            if (!moduleFilters.isInclude()) {
                moduleFiltersElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(moduleFilters.isInclude())));
            }
            if (!moduleFilters.getFilters().isEmpty()) {
                for (ModuleFilter filter : moduleFilters.getFilters()) {
                    final ElementNode filterElementNode = new ElementNode(moduleFiltersElementNode, Element.FILTER.getLocalName());
                    filterElementNode.addAttribute(Attribute.PATTERN.getLocalName(), new AttributeValue(filter.getPattern()));
                    filterElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(filter.isInclude())));
                    if (!filter.isTransitive()) {
                        filterElementNode.addAttribute(Attribute.TRANSITIVE.getLocalName(), new AttributeValue(Boolean.toString(filter.isTransitive())));
                    }
                    moduleFiltersElementNode.addChild(filterElementNode);
                }
            }
            featurePackElementNode.addChild(moduleFiltersElementNode);
        }
    }

    protected void processFeaturePackSubsystems(ServerProvisioningDescription.FeaturePack featurePack, ElementNode featurePackElementNode) {
        final List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = featurePack.getSubsystems();
        if (subsystems != null && !subsystems.isEmpty()) {
            final ElementNode subsystemsElementNode = new ElementNode(featurePackElementNode, Element.SUBSYSTEMS.getLocalName());
            for (ServerProvisioningDescription.FeaturePack.Subsystem subsystem : featurePack.getSubsystems()) {
                final ElementNode subsystemElementNode = new ElementNode(subsystemsElementNode, Element.SUBSYSTEM.getLocalName());
                subsystemElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(subsystem.getName()));
                if (subsystem.isTransitive()) {
                    subsystemElementNode.addAttribute(Attribute.TRANSITIVE.getLocalName(), new AttributeValue(Boolean.toString(subsystem.isTransitive())));
                }
                subsystemsElementNode.addChild(subsystemElementNode);
            }
            featurePackElementNode.addChild(subsystemsElementNode);
        }
    }

    protected void processFeaturePackConfigOverride(ServerProvisioningDescription.FeaturePack featurePack, ElementNode featurePackElementNode) {
        final ConfigOverride configOverride = featurePack.getConfigOverride();
        if (configOverride != null) {
            final ElementNode configElementNode = new ElementNode(featurePackElementNode, Element.CONFIG.getLocalName());
            processFeaturePackConfigFileOverrides(configOverride.getStandaloneConfigFiles().values(), Element.STANDALONE, configElementNode);
            processFeaturePackConfigFileOverrides(configOverride.getDomainConfigFiles().values(), Element.DOMAIN, configElementNode);
            featurePackElementNode.addChild(configElementNode);
        }
    }

    protected void processFeaturePackConfigFileOverrides(Collection<ConfigFileOverride> configFiles, Element element, ElementNode parentElementNode) {
        for (ConfigFileOverride configFile : configFiles) {
            final ElementNode configFileElementNode = new ElementNode(parentElementNode, element.getLocalName());
            // attrs
            configFileElementNode.addAttribute(Attribute.OUTPUT_FILE.getLocalName(), new AttributeValue(configFile.getOutputFile()));
            if (configFile.isUseTemplate()) {
                configFileElementNode.addAttribute(Attribute.USE_TEMPLATE.getLocalName(), new AttributeValue(Boolean.toString(configFile.isUseTemplate())));
            }
            // properties
            for (Map.Entry<String, String> property : configFile.getProperties().entrySet()) {
                ElementNode propertyElementNode = new ElementNode(configFileElementNode, Element.PROPERTY.getLocalName());
                propertyElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(property.getKey()));
                propertyElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(property.getValue()));
                configFileElementNode.addChild(propertyElementNode);
            }
            // subsystems
            final Map<String, Map<String, SubsystemConfig>> subsystems = configFile.getSubsystems();
            if (subsystems != null) {
                for (Map.Entry<String, Map<String, SubsystemConfig>> entry : subsystems.entrySet()) {
                    // TODO since parsing of this is separated from provisioning xml parser, this should also move into a separated xml writer
                    ElementNode subsystemsElementNode = new ElementNode(configFileElementNode, Element.SUBSYSTEMS.getLocalName());
                    if (!entry.getKey().equals("")) {
                        subsystemsElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(entry.getKey()));
                    }
                    for (SubsystemConfig subsystemConfig : entry.getValue().values()) {
                        ElementNode subsystemElementNode = new ElementNode(subsystemsElementNode, Element.SUBSYSTEM.getLocalName());
                        if (subsystemConfig.getSupplement() != null) {
                            subsystemElementNode.addAttribute("supplement", new AttributeValue(subsystemConfig.getSupplement()));
                        }
                        subsystemElementNode.addChild(new TextNode(subsystemConfig.getSubsystem()));
                        subsystemsElementNode.addChild(subsystemElementNode);
                    }
                    configFileElementNode.addChild(subsystemsElementNode);
                }
            }
            parentElementNode.addChild(configFileElementNode);
        }
    }

    protected void processFeaturePackContentFilters(ServerProvisioningDescription.FeaturePack featurePack, ElementNode featurePackElementNode) {
        final ServerProvisioningDescription.FeaturePack.ContentFilters contentFilters = featurePack.getContentFilters();
        if (contentFilters != null) {
            final ElementNode contentFiltersElementNode = new ElementNode(featurePackElementNode, Element.CONTENTS.getLocalName());
            if (!contentFilters.isInclude()) {
                contentFiltersElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(contentFilters.isInclude())));
            }
            if (!contentFilters.getFilters().isEmpty()) {
                for (FileFilter filter : contentFilters.getFilters()) {
                    FileFilterXMLWriter10.INSTANCE.write(filter, contentFiltersElementNode);
                }
            }
            featurePackElementNode.addChild(contentFiltersElementNode);
        }
    }

    protected void processVersionOverrides(Collection<Artifact> artifacts, ElementNode parentElementNode) {
        if (!artifacts.isEmpty()) {
            final ElementNode versionOverridesElementNode = new ElementNode(parentElementNode, Element.VERSION_OVERRIDES.getLocalName());
            for (Artifact artifact : artifacts) {
                final ElementNode versionOverrideElementNode = new ElementNode(versionOverridesElementNode, Element.VERSION_OVERRIDE.getLocalName());
                versionOverrideElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(artifact.getGroupId()));
                versionOverrideElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(artifact.getArtifactId()));
                versionOverrideElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(artifact.getVersion()));
                if (artifact.getClassifier() != null) {
                    versionOverrideElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(artifact.getClassifier()));
                }
                if (artifact.getPackaging() != null) {
                    versionOverrideElementNode.addAttribute(Attribute.EXTENSION.getLocalName(), new AttributeValue(artifact.getPackaging()));
                }
                versionOverridesElementNode.addChild(versionOverrideElementNode);
            }
            parentElementNode.addChild(versionOverridesElementNode);
        }
    }
}
