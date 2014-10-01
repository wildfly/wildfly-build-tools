/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.wildfly.build.util.BuildPropertyReplacer;
import org.wildfly.build.util.xml.ParsingUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Martins
 */
public class FileFilterModelParser10 {

    public static final String ELEMENT_LOCAL_NAME = "filter";

    enum Attribute {

        // default unknown attribute
        UNKNOWN(null),
        PATTERN("pattern"),
        INCLUDE("include");

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(PATTERN.getLocalName(), PATTERN);
            attributesMap.put(INCLUDE.getLocalName(), INCLUDE);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName.getLocalPart());
            return attribute == null ? UNKNOWN : attribute;
        }

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    private final BuildPropertyReplacer propertyReplacer;

    public FileFilterModelParser10(BuildPropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    public void parseFilter(XMLStreamReader reader, List<FileFilter> filters) throws XMLStreamException {
        String pattern = null;
        boolean include = false;
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
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        ParsingUtils.parseNoContent(reader);

        filters.add(new FileFilter(pattern, include));
    }
}
