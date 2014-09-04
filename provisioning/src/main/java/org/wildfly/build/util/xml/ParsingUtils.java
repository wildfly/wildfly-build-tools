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
package org.wildfly.build.util.xml;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.util.Map;
import java.util.Set;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Eduardo Martins
 */
public class ParsingUtils {

    public static String getNextElement(XMLStreamReader reader, String name, Map<String, String> attributes, boolean getElementText) throws XMLStreamException {
        if (!reader.hasNext()) {
            throw new XMLStreamException("Expected more elements", reader.getLocation());
        }
        int type = reader.next();
        while (reader.hasNext() && type != START_ELEMENT) {
            type = reader.next();
        }
        if (reader.getEventType() != START_ELEMENT) {
            throw new XMLStreamException("No <" + name + "> found");
        }
        if (!reader.getLocalName().equals("" + name + "")) {
            throw new XMLStreamException("<" + name + "> expected", reader.getLocation());
        }

        if (attributes != null) {
            for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                String attr = reader.getAttributeLocalName(i);
                if (!attributes.containsKey(attr)) {
                    throw new XMLStreamException("Unexpected attribute " + attr, reader.getLocation());
                }
                attributes.put(attr, reader.getAttributeValue(i));
            }
        }

        return getElementText ? reader.getElementText() : null;
    }

    public static void parseNoContent(final XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }
        throw endOfDocument(reader.getLocation());
    }

    public static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                kind = "attribute";
                break;
            case XMLStreamConstants.CDATA:
                kind = "cdata";
                break;
            case XMLStreamConstants.CHARACTERS:
                kind = "characters";
                break;
            case XMLStreamConstants.COMMENT:
                kind = "comment";
                break;
            case XMLStreamConstants.DTD:
                kind = "dtd";
                break;
            case XMLStreamConstants.END_DOCUMENT:
                kind = "document end";
                break;
            case XMLStreamConstants.END_ELEMENT:
                kind = "element end";
                break;
            case XMLStreamConstants.ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case XMLStreamConstants.NAMESPACE:
                kind = "namespace";
                break;
            case XMLStreamConstants.NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case XMLStreamConstants.SPACE:
                kind = "whitespace";
                break;
            case XMLStreamConstants.START_DOCUMENT:
                kind = "document start";
                break;
            case XMLStreamConstants.START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }

        return new XMLStreamException("unexpected content: " + kind + (reader.hasName() ? reader.getName() : null) +
                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    public static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Unexpected end of document ", location);
    }

    public static XMLStreamException missingAttributes(final Location location, final Set<? extends Enum> requiredAttributes) {
        final StringBuilder b = new StringBuilder();
        for (Object attribute : requiredAttributes) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException("Missing required attributes " + b.toString(), location);
    }

    public static String wildcardToJavaRegexp(String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("expr is null");
        }
        String regex = expr.replaceAll("([(){}\\[\\].+^$])", "\\\\$1"); // escape regex characters
        regex = regex.replaceAll("\\*", ".*"); // replace * with .*
        regex = regex.replaceAll("\\?", "."); // replace ? with .
        return regex;
    }
}
