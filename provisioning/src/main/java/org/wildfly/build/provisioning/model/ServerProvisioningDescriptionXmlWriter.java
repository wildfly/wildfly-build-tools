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
import org.wildfly.build.common.model.CopyArtifactsXMLWriter10;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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
        final ElementNode rootElementNode = new ElementNode(null, Element.SERVER_PROVISIONING.getLocalName(), ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0);
        if (description.isCopyModuleArtifacts()) {
            rootElementNode.addAttribute(Attribute.COPY_MODULE_ARTIFACTS.getLocalName(), new AttributeValue(Boolean.TRUE.toString()));
        }
        if (description.isExtractSchemas()) {
            rootElementNode.addAttribute(Attribute.EXTRACT_SCHEMAS.getLocalName(), new AttributeValue(Boolean.TRUE.toString()));
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
                final ElementNode featurePackElementNode = new ElementNode(featurePacksElementNode, Element.FEATURE_PACK.getLocalName());
                featurePackElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(featurePack.getArtifact().getGACE().getGroupId()));
                featurePackElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(featurePack.getArtifact().getGACE().getArtifactId()));
                featurePackElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(featurePack.getArtifact().getVersion()));
                if(featurePack.getArtifact().getGACE().getClassifier() != null) {
                    featurePackElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(featurePack.getArtifact().getGACE().getClassifier()));
                }
                final List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = featurePack.getSubsystems();
                if(subsystems != null && !subsystems.isEmpty()) {
                    final ElementNode subsystemsElementNode = new ElementNode(featurePackElementNode, Element.SUBSYSTEMS.getLocalName());
                    for(ServerProvisioningDescription.FeaturePack.Subsystem subsystem : featurePack.getSubsystems()) {
                        final ElementNode subsystemElementNode = new ElementNode(featurePackElementNode, Element.SUBSYSTEM.getLocalName());
                        subsystemElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(subsystem.getName()));
                        subsystemElementNode.addAttribute(Attribute.TRANSITIVE.getLocalName(), new AttributeValue(Boolean.toString(subsystem.isTransitive())));
                        subsystemsElementNode.addChild(subsystemElementNode);
                    }
                    featurePackElementNode.addChild(subsystemsElementNode);
                }
                // TODO implement writing of inner elements
                featurePacksElementNode.addChild(featurePackElementNode);
            }
            parentElementNode.addChild(featurePacksElementNode);
        }
    }

    protected void processVersionOverrides(List<Artifact> artifacts, ElementNode parentElementNode) {
        if (!artifacts.isEmpty()) {
            final ElementNode versionOverridesElementNode = new ElementNode(parentElementNode, Element.VERSION_OVERRIDES.getLocalName());
            for (Artifact artifact : artifacts) {
                final ElementNode versionOverrideElementNode = new ElementNode(versionOverridesElementNode, Element.VERSION_OVERRIDE.getLocalName());
                versionOverrideElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(artifact.getGACE().getGroupId()));
                versionOverrideElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(artifact.getGACE().getArtifactId()));
                versionOverrideElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(artifact.getVersion()));
                if(artifact.getGACE().getClassifier() != null) {
                    versionOverrideElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(artifact.getGACE().getClassifier()));
                }
                if(artifact.getGACE().getExtension() != null) {
                    versionOverrideElementNode.addAttribute(Attribute.EXTENSION.getLocalName(), new AttributeValue(artifact.getGACE().getExtension()));
                }
                versionOverridesElementNode.addChild(versionOverrideElementNode);
            }
            parentElementNode.addChild(versionOverridesElementNode);
        }
    }
}
