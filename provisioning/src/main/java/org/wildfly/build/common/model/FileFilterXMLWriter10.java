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

import static org.wildfly.build.common.model.FileFilterModelParser10.Attribute;

/**
 * Writes a file filter as XML element.
 *
 * @author Eduardo Martins
 */
public class FileFilterXMLWriter10 {

    public static final FileFilterXMLWriter10 INSTANCE = new FileFilterXMLWriter10();

    private FileFilterXMLWriter10() {
    }

    public void write(FileFilter fileFilter, ElementNode parentElementNode) {
        final ElementNode fileFilterElementNode = new ElementNode(parentElementNode, FileFilterModelParser10.ELEMENT_LOCAL_NAME);
        fileFilterElementNode.addAttribute(Attribute.PATTERN.getLocalName(), new AttributeValue(fileFilter.getPattern()));
        fileFilterElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(fileFilter.isInclude())));
        parentElementNode.addChild(fileFilterElementNode);
    }

}
