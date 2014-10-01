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

import static org.wildfly.build.common.model.FilePermissionsModelParser10.Attribute;
import static org.wildfly.build.common.model.FilePermissionsModelParser10.Element;

/**
 * Writes a list of file permissions as XML.
 *
 * @author Eduardo Martins
 */
public class FilePermissionsXMLWriter10 {

    public static final FilePermissionsXMLWriter10 INSTANCE = new FilePermissionsXMLWriter10();

    private FilePermissionsXMLWriter10() {
    }

    public void write(List<FilePermission> filePermissions, ElementNode parentElementNode) {
        if (!filePermissions.isEmpty()) {
            final ElementNode filePermissionsElementNode = new ElementNode(parentElementNode, FilePermissionsModelParser10.ELEMENT_LOCAL_NAME);
            for (FilePermission filePermission : filePermissions) {
                writeFilePermission(filePermission, filePermissionsElementNode);
            }
            parentElementNode.addChild(filePermissionsElementNode);
        }
    }

    protected void writeFilePermission(FilePermission filePermission, ElementNode filePermissionsElementNode) {
        final ElementNode filePermissionElementNode = new ElementNode(filePermissionsElementNode, Element.PERMISSION.getLocalName());
        if (!filePermission.getFilters().isEmpty()) {
            for (FileFilter fileFilter : filePermission.getFilters()) {
                FileFilterXMLWriter10.INSTANCE.write(fileFilter, filePermissionElementNode);
            }
        }
        filePermissionElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(filePermission.getValue()));
        filePermissionsElementNode.addChild(filePermissionElementNode);
    }

}
