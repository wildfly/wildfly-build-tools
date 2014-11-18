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

import org.wildfly.build.common.model.CopyArtifactsModelParser10;
import org.wildfly.build.common.model.FileFilterModelParser10;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
* @author Stuart Douglas
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
    FILTER(FileFilterModelParser10.ELEMENT_LOCAL_NAME),
    ;

    private static final Map<QName, Element> elements;

    static {
        Map<QName, Element> elementsMap = new HashMap<QName, Element>();
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, SERVER_PROVISIONING.getLocalName()), SERVER_PROVISIONING);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, FEATURE_PACKS.getLocalName()), FEATURE_PACKS);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, FEATURE_PACK.getLocalName()), FEATURE_PACK);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, ARTIFACT.getLocalName()), ARTIFACT);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, MODULES.getLocalName()), MODULES);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, FILTER.getLocalName()), FILTER);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, CONFIG.getLocalName()), CONFIG);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, STANDALONE.getLocalName()), STANDALONE);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, DOMAIN.getLocalName()), DOMAIN);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, PROPERTY.getLocalName()), PROPERTY);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, SUBSYSTEMS.getLocalName()), SUBSYSTEMS);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, SUBSYSTEM.getLocalName()), SUBSYSTEM);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, CONTENTS.getLocalName()), CONTENTS);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, VERSION_OVERRIDES.getLocalName()), VERSION_OVERRIDES);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, VERSION_OVERRIDE.getLocalName()), VERSION_OVERRIDE);
        elementsMap.put(new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, COPY_ARTIFACTS.getLocalName()), COPY_ARTIFACTS);
        elements = elementsMap;
    }

    static Element of(QName qName) {
        QName name;
        if (qName.getNamespaceURI().equals("")) {
            name = new QName(ServerProvisioningDescriptionModelParser10.NAMESPACE_1_0, qName.getLocalPart());
        } else {
            name = qName;
        }
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
