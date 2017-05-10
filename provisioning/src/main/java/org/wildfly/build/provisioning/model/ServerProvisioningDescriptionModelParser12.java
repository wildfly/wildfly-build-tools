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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.build.util.PropertyResolver;
import org.wildfly.build.util.xml.ParsingUtils;

/**
 * Parses the distribution build config file, i.e. the config file that is
 * used to create a wildfly distribution.
 *
 * @author Tomaz Cerar
 */
class ServerProvisioningDescriptionModelParser12 extends ServerProvisioningDescriptionModelParser11 {

    public static final String NAMESPACE_1_2 = "urn:wildfly:server-provisioning:1.2";

    ServerProvisioningDescriptionModelParser12(PropertyResolver resolver) {
        super(resolver);
    }

    /*
    we only override this method, for now only change new EXTRACT_SCHEMAS_GROUPS attribute
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final ServerProvisioningDescription result) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case COPY_MODULE_ARTIFACTS:
                    result.setCopyModuleArtifacts(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                case EXTRACT_SCHEMAS:
                    result.setExtractSchemas(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                case EXTRACT_SCHEMAS_GROUPS:
                    result.setExtractSchemasGroups(reader.getAttributeValue(i));
                    break;
                case EXCLUDE_DEPENDENCIES:
                    result.setExcludeDependencies(Boolean.parseBoolean(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getLocalName());

                    switch (element) {
                        case FEATURE_PACKS:
                            parseFeaturePacks(reader, result);
                            break;
                        case VERSION_OVERRIDES:
                            parseVersionOverrides(reader, result);
                            break;
                        case COPY_ARTIFACTS:
                            copyArtifactsModelParser.parseCopyArtifacts(reader, result.getCopyArtifacts());
                            break;
                        default:
                            throw new XMLStreamException(String.format("Unknown element: '%s', elementName: %s, localName: %s", element, reader.getName(), reader.getLocalName()), reader.getLocation());
                            //throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

}
