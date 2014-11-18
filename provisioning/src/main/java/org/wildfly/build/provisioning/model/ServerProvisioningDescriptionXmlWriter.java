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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Stuart Douglas
 */
public class ServerProvisioningDescriptionXmlWriter implements XMLElementWriter<ServerProvisioningDescription> {

    public static ServerProvisioningDescriptionXmlWriter INSTANCE = new ServerProvisioningDescriptionXmlWriter();


    public void writeContent(XMLStreamWriter streamWriter, ServerProvisioningDescription value) throws XMLStreamException {
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.deparseDocument(this, value, streamWriter);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter, ServerProvisioningDescription value) throws XMLStreamException {
        streamWriter.writeStartDocument();
        streamWriter.writeStartElement(Element.SERVER_PROVISIONING.getLocalName());
        streamWriter.writeDefaultNamespace(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0);
        streamWriter.writeStartElement(Element.FEATURE_PACKS.getLocalName());
        for(ServerProvisioningDescription.FeaturePack pack : value.getFeaturePacks()) {
            writeFeaturePack(pack, streamWriter);
        }
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
        streamWriter.writeEndDocument();
    }

    private void writeFeaturePack(ServerProvisioningDescription.FeaturePack pack, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(Element.FEATURE_PACK.getLocalName());
        streamWriter.writeAttribute(Attribute.GROUP_ID.getLocalName(), pack.getArtifact().getGACE().getGroupId());
        streamWriter.writeAttribute(Attribute.ARTIFACT_ID.getLocalName(), pack.getArtifact().getGACE().getArtifactId());
        streamWriter.writeAttribute(Attribute.VERSION.getLocalName(), pack.getArtifact().getVersion());
        if(pack.getArtifact().getGACE().getExtension() != null) {
            streamWriter.writeAttribute(Attribute.EXTENSION.getLocalName(), pack.getArtifact().getGACE().getExtension());
        }
        if(pack.getArtifact().getGACE().getClassifier() != null) {
            streamWriter.writeAttribute(Attribute.CLASSIFIER.getLocalName(), pack.getArtifact().getGACE().getClassifier());
        }
    }
}
