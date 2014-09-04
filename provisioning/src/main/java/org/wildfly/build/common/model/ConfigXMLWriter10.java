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

import java.util.Map;

import static org.wildfly.build.common.model.ConfigModelParser10.Attribute;
import static org.wildfly.build.common.model.ConfigModelParser10.Element;

/**
 * Writes a {@link org.wildfly.build.common.model.Config} as XML.
 *
 * @author Eduardo Martins
 */
public class ConfigXMLWriter10 {

    public static final ConfigXMLWriter10 INSTANCE = new ConfigXMLWriter10();

    private ConfigXMLWriter10() {
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
