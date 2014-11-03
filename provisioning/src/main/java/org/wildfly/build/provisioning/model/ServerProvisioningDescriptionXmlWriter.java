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
import org.wildfly.build.common.model.ArtifactRefsXMLWriter10;
import org.wildfly.build.common.model.CopyArtifactsXMLWriter10;
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
 * Writes a {@link ServerProvisioningDescription} as XML.
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionXmlWriter implements XMLElementWriter<ServerProvisioningDescription> {

    public static final ServerProvisioningDescriptionXmlWriter INSTANCE = new ServerProvisioningDescriptionXmlWriter();

    private ServerProvisioningDescriptionXmlWriter() {
    }

    public void writeContent(XMLStreamWriter streamWriter, ServerProvisioningDescription value) throws XMLStreamException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.deparseDocument(this, value, streamWriter);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter, ServerProvisioningDescription value) throws XMLStreamException {
        final ElementNode rootElementNode = new ElementNode(null, Element.SERVER_PROVISIONING.getLocalName(), ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0);
        processFeaturePacks(value.getFeaturePacks(), rootElementNode);
        ArtifactRefsXMLWriter10.INSTANCE.write(value.getArtifactRefs(), rootElementNode);
        CopyArtifactsXMLWriter10.INSTANCE.write(value.getCopyArtifacts(), rootElementNode);
        streamWriter.writeStartDocument();
        rootElementNode.marshall(streamWriter);
        streamWriter.writeEndDocument();
    }

    public void writeContent(ServerProvisioningDescription value, File outputFile) throws XMLStreamException, IOException {
        try (FileOutputStream out = new FileOutputStream(outputFile)){
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            try {
                writeContent(writer, value);
            } finally {
                try {
                    writer.close();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    protected void processFeaturePacks(List<ServerProvisioningDescription.FeaturePack> featurePacks, ElementNode parentElementNode) {
        if (!featurePacks.isEmpty()) {
            final ElementNode featurePacksElementNode = new ElementNode(parentElementNode, Element.FEATURE_PACKS.getLocalName());
            for (ServerProvisioningDescription.FeaturePack featurePack : featurePacks) {
                final ElementNode featurePackElementNode = new ElementNode(featurePacksElementNode, Element.FEATURE_PACK.getLocalName());
                featurePackElementNode.addAttribute(Attribute.ARTIFACT.getLocalName(), new AttributeValue(featurePack.getArtifact()));
                if(!featurePack.getSubsystems().isEmpty()) {
                    final ElementNode subsystemsElementNode = new ElementNode(featurePackElementNode, Element.SUBSYSTEMS.getLocalName());
                    for(ServerProvisioningDescription.FeaturePack.Subsystem subsystem : featurePack.getSubsystems()) {
                        final ElementNode subsystemElementNode = new ElementNode(subsystemsElementNode, Element.SUBSYSTEM.getLocalName());
                        subsystemElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(subsystem.getName()));
                        subsystemElementNode.addAttribute(Attribute.TRANSITIVE.getLocalName(), new AttributeValue(Boolean.toString(subsystem.isTransitive())));
                        subsystemsElementNode.addChild(subsystemElementNode);
                    }
                    featurePackElementNode.addChild(subsystemsElementNode);
                }
                // TODO implement writing of remaining inner elements
                featurePacksElementNode.addChild(featurePackElementNode);
            }
            parentElementNode.addChild(featurePacksElementNode);
        }
    }
}
