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

import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;

import java.util.List;

import static org.wildfly.build.common.model.CopyArtifactsModelParser10.Attribute;
import static org.wildfly.build.common.model.CopyArtifactsModelParser10.Element;

/**
 * Writes a copy artifact list as XML.
 *
 * @author Eduardo Martins
 */
public class CopyArtifactsXMLWriter10 {

    public static final CopyArtifactsXMLWriter10 INSTANCE = new CopyArtifactsXMLWriter10();

    private CopyArtifactsXMLWriter10() {
    }

    public void write(List<CopyArtifact> copyArtifacts, ElementNode parentElementNode) {
        if (!copyArtifacts.isEmpty()) {
            final ElementNode artifactsElementNode = new ElementNode(parentElementNode, CopyArtifactsModelParser10.ELEMENT_LOCAL_NAME);
            for (CopyArtifact copyArtifact : copyArtifacts) {
                final ElementNode copyArtifactElementNode = new ElementNode(parentElementNode, Element.COPY_ARTIFACT.getLocalName());
                writeCopyArtifact(copyArtifact, copyArtifactElementNode);
                artifactsElementNode.addChild(copyArtifactElementNode);
            }
            parentElementNode.addChild(artifactsElementNode);
        }
    }

    protected void writeCopyArtifact(CopyArtifact copyArtifact, ElementNode copyArtifactElementNode) {
        if (!copyArtifact.getFilters().isEmpty()) {
            for (FileFilter fileFilter : copyArtifact.getFilters()) {
                FileFilterXMLWriter10.INSTANCE.write(fileFilter, copyArtifactElementNode);
            }
        }
        copyArtifactElementNode.addAttribute(Attribute.ARTIFACT.getLocalName(), new AttributeValue(copyArtifact.getArtifact()));
        copyArtifactElementNode.addAttribute(Attribute.TO_LOCATION.getLocalName(), new AttributeValue(copyArtifact.getToLocation()));
        if (copyArtifact.isExtract()) {
            copyArtifactElementNode.addAttribute(Attribute.EXTRACT.getLocalName(), new AttributeValue(Boolean.toString(copyArtifact.isExtract())));
        }
    }

}
