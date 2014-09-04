/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
