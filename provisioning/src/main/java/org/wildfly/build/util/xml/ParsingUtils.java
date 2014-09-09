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
}
