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

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
* @author Stuart Douglas
*/
enum Attribute {

    // default unknown attribute
    UNKNOWN(null),

    COPY_MODULE_ARTIFACTS("copy-module-artifacts"),
    EXTRACT_SCHEMAS("extract-schemas"),
    EXTRACT_SCHEMAS_GROUPS("extract-schemas-groups"),
    EXCLUDE_DEPENDENCIES("exclude-dependencies"),
    PATTERN("pattern"),
    INCLUDE("include"),
    TRANSITIVE("transitive"),
    OUTPUT_FILE("output-file"),
    USE_TEMPLATE("use-template"),
    NAME("name"),
    VALUE("value"),
    GROUP_ID("groupId"),
    ARTIFACT_ID("artifactId"),
    CLASSIFIER("classifier"),
    EXTENSION("extension"),
    VERSION("version"),
    ;

    private static final Map<QName, Attribute> attributes;

    static {
        Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
        attributesMap.put(new QName(PATTERN.getLocalName()), PATTERN);
        attributesMap.put(new QName(INCLUDE.getLocalName()), INCLUDE);
        attributesMap.put(new QName(TRANSITIVE.getLocalName()), TRANSITIVE);
        attributesMap.put(new QName(OUTPUT_FILE.getLocalName()), OUTPUT_FILE);
        attributesMap.put(new QName(USE_TEMPLATE.getLocalName()), USE_TEMPLATE);
        attributesMap.put(new QName(NAME.getLocalName()), NAME);
        attributesMap.put(new QName(VALUE.getLocalName()), VALUE);
        attributesMap.put(new QName(COPY_MODULE_ARTIFACTS.getLocalName()), COPY_MODULE_ARTIFACTS);
        attributesMap.put(new QName(EXTRACT_SCHEMAS.getLocalName()), EXTRACT_SCHEMAS);
        attributesMap.put(new QName(EXTRACT_SCHEMAS_GROUPS.getLocalName()), EXTRACT_SCHEMAS_GROUPS);
        attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
        attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
        attributesMap.put(new QName(CLASSIFIER.getLocalName()), CLASSIFIER);
        attributesMap.put(new QName(EXTENSION.getLocalName()), EXTENSION);
        attributesMap.put(new QName(VERSION.getLocalName()), VERSION);
        attributesMap.put(new QName(EXCLUDE_DEPENDENCIES.getLocalName()), EXCLUDE_DEPENDENCIES);


        attributes = attributesMap;
    }

    static Attribute of(QName qName) {
        final Attribute attribute = attributes.get(qName);
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
