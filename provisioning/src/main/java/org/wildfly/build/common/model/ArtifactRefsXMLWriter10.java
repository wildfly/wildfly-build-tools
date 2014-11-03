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
import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;

import java.util.Map;
import java.util.TreeMap;

import static org.wildfly.build.common.model.ArtifactRefsModelParser10.Attribute;
import static org.wildfly.build.common.model.ArtifactRefsModelParser10.Element;

/**
 *
 * @author Eduardo Martins
 */
public class ArtifactRefsXMLWriter10 {

    public static final ArtifactRefsXMLWriter10 INSTANCE = new ArtifactRefsXMLWriter10();

    private ArtifactRefsXMLWriter10() {
    }

    public void write(Map<String, Artifact> artifactRefs, ElementNode parentElementNode) {
        if (!artifactRefs.isEmpty()) {
            final ElementNode artifactsElementNode = new ElementNode(parentElementNode, ArtifactRefsModelParser10.ELEMENT_LOCAL_NAME);
            final TreeMap<String, Artifact> sortedArtifactRefs = new TreeMap<>(artifactRefs);
            for (Map.Entry<String, Artifact> artifactEntry : sortedArtifactRefs.entrySet()) {
                final String artifactName = artifactEntry.getKey();
                final Artifact artifact = artifactEntry.getValue();
                final ElementNode elementNode = new ElementNode(parentElementNode, Element.ARTIFACT.getLocalName());
                writeArtifact(artifactName, artifact, elementNode);
                artifactsElementNode.addChild(elementNode);
            }
            parentElementNode.addChild(artifactsElementNode);
        }
    }

    protected void writeArtifact(String artifactName, Artifact artifact, ElementNode artifactElementNode) {
        artifactElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(artifactName));
        artifactElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(artifact.getGroupId()));
        artifactElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(artifact.getArtifactId()));
        artifactElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(artifact.getVersion()));
        if (artifact.getClassifier() != null) {
            artifactElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(artifact.getClassifier()));
        }
        if (artifact.getExtension() != null) {
            artifactElementNode.addAttribute(Attribute.EXTENSION.getLocalName(), new AttributeValue(artifact.getExtension()));
        }
    }

}
