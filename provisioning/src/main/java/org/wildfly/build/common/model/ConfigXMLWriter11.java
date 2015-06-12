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

import java.util.Map;

import static org.wildfly.build.common.model.ConfigModelParser10.Attribute;
import static org.wildfly.build.common.model.ConfigModelParser10.Element;

/**
 * Writes a {@link org.wildfly.build.common.model.Config} as XML.
 *
 * @author Eduardo Martins
 */
public class ConfigXMLWriter11 {

    public static final ConfigXMLWriter11 INSTANCE = new ConfigXMLWriter11();

    private ConfigXMLWriter11() {
    }

    public void write(Config config, ElementNode parentElementNode) {
        if (!config.getStandaloneConfigFiles().isEmpty() || !config.getDomainConfigFiles().isEmpty()) {
            final ElementNode configElementNode = new ElementNode(parentElementNode, ConfigModelParser10.ELEMENT_LOCAL_NAME);
            for (ConfigFile configFile : config.getStandaloneConfigFiles()) {
                final ElementNode standaloneElementNode = new ElementNode(parentElementNode, Element.STANDALONE.getLocalName());
                writeConfigFile(configFile, standaloneElementNode);
                configElementNode.addChild(standaloneElementNode);
            }
            for (ConfigFile configFile : config.getDomainConfigFiles()) {
                final ElementNode domainElementNode = new ElementNode(parentElementNode, Element.DOMAIN.getLocalName());
                writeConfigFile(configFile, domainElementNode);
                configElementNode.addChild(domainElementNode);
            }
            for (ConfigFile configFile : config.getHostConfigFiles()) {
                final ElementNode hostElementNode = new ElementNode(parentElementNode, Element.HOST.getLocalName());
                writeConfigFile(configFile, hostElementNode);
                configElementNode.addChild(hostElementNode);
            }
            parentElementNode.addChild(configElementNode);
        }
    }

    protected void writeConfigFile(ConfigFile configFile, ElementNode configElementNode) {
        for (Map.Entry<String, String> property : configFile.getProperties().entrySet()) {
            ElementNode propertyElementNode = new ElementNode(configElementNode, Element.PROPERTY.getLocalName());
            propertyElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(property.getKey()));
            propertyElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(property.getValue()));
            configElementNode.addChild(propertyElementNode);
        }
        configElementNode.addAttribute(Attribute.TEMPLATE.getLocalName(), new AttributeValue(configFile.getTemplate()));
        configElementNode.addAttribute(Attribute.SUBSYSTEMS.getLocalName(), new AttributeValue(configFile.getSubsystems()));
        configElementNode.addAttribute(Attribute.OUTPUT_FILE.getLocalName(), new AttributeValue(configFile.getOutputFile()));
    }

}
