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

import java.util.HashMap;
import java.util.Map;

import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.common.model.FileFilterModelParser10;

/**
 * @author Stuart Douglas
 * @author Tomaz Cerar
 */
enum Element {

    // default unknown element
    UNKNOWN(null),

    SERVER_PROVISIONING("server-provisioning"),
    FEATURE_PACKS("feature-packs"),
    FEATURE_PACK("feature-pack"),
    ARTIFACT("artifact"),
    MODULES("modules"),
    CONFIG("config"),
    STANDALONE("standalone"),
    DOMAIN("domain"),
    PROPERTY("property"),
    SUBSYSTEMS("subsystems"),
    SUBSYSTEM("subsystem"),
    CONTENTS("contents"),
    VERSION_OVERRIDES("version-overrides"),
    VERSION_OVERRIDE("version-override"),
    COPY_ARTIFACTS(CopyArtifactsModelParser10.ELEMENT_LOCAL_NAME),
    FILTER(FileFilterModelParser10.ELEMENT_LOCAL_NAME);

    private static final Map<String, Element> elements;

    static {
        Map<String, Element> elementsMap = new HashMap<>();
        elementsMap.put(SERVER_PROVISIONING.getLocalName(), SERVER_PROVISIONING);
        elementsMap.put(FEATURE_PACKS.getLocalName(), FEATURE_PACKS);
        elementsMap.put(FEATURE_PACK.getLocalName(), FEATURE_PACK);
        elementsMap.put(ARTIFACT.getLocalName(), ARTIFACT);
        elementsMap.put(MODULES.getLocalName(), MODULES);
        elementsMap.put(FILTER.getLocalName(), FILTER);
        elementsMap.put(CONFIG.getLocalName(), CONFIG);
        elementsMap.put(STANDALONE.getLocalName(), STANDALONE);
        elementsMap.put(DOMAIN.getLocalName(), DOMAIN);
        elementsMap.put(PROPERTY.getLocalName(), PROPERTY);
        elementsMap.put(SUBSYSTEMS.getLocalName(), SUBSYSTEMS);
        elementsMap.put(SUBSYSTEM.getLocalName(), SUBSYSTEM);
        elementsMap.put(CONTENTS.getLocalName(), CONTENTS);
        elementsMap.put(VERSION_OVERRIDES.getLocalName(), VERSION_OVERRIDES);
        elementsMap.put(VERSION_OVERRIDE.getLocalName(), VERSION_OVERRIDE);
        elementsMap.put(COPY_ARTIFACTS.getLocalName(), COPY_ARTIFACTS);
        elements = elementsMap;
    }

    static Element of(String name) {
        final Element element = elements.get(name);
        return element == null ? UNKNOWN : element;
    }

    private final String name;

    Element(final String name) {
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
