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
package org.wildfly.build.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.wildfly.build.pack.model.ModuleIdentifier;
import org.wildfly.build.util.ModuleParseResult.ResourceRoot;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;

/**
 *
 * @author Thomas.Diesler@jboss.com
 * @author Stuart Douglas
 * @author Eduardo Martins
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @since 06-Sep-2012
 */
public class ModuleParser {

    private final BuildPropertyReplacer propertyReplacer;

    public ModuleParser(PropertyResolver propertyResolver) {
        this.propertyReplacer = new BuildPropertyReplacer(propertyResolver);
    }

    public ModuleParseResult parse(Path inputFile)
            throws IOException, ParsingException {
        return parse(new BufferedInputStream(new FileInputStream(inputFile.toFile())));
    }

    public ModuleParseResult parse(final InputStream in) throws IOException, ParsingException {
        Builder builder = new Builder(false);
        final Document document;
        try (InputStream in1 = in) {
            document = builder.build(in1);
        }
        ModuleParseResult result = new ModuleParseResult(document);
        final Element rootElement = document.getRootElement();
        if (rootElement.getLocalName().equals("module-alias")) {
            parseModuleAlias(rootElement, result);
        } else if (rootElement.getLocalName().equals("module")) {
            parseModule(rootElement, result);
        }
        return result;
    }

    private void parseModule(Element element, ModuleParseResult result) {
        String name = element.getAttributeValue("name");
        String slot = getOptionalAttributeValue(element, "slot", "main");
        result.identifier = new ModuleIdentifier(name, slot);
        final Attribute versionAttribute = element.getAttribute("version");
        if (versionAttribute != null) {
            result.versionArtifactName = parseOptionalArtifactName(versionAttribute.getValue(), versionAttribute);
        }
        final Element dependencies = element.getFirstChildElement("dependencies", element.getNamespaceURI());
        if (dependencies != null) parseDependencies(dependencies, result);
        final Element resources = element.getFirstChildElement("resources", element.getNamespaceURI());
        if (resources != null) parseResources(resources, result);
    }

    private String getOptionalAttributeValue(Element element, String name, String defVal) {
        final String value = element.getAttributeValue(name);
        return value == null ? defVal : value;
    }

    private void parseModuleAlias(Element element, ModuleParseResult result) {
        final String targetName = getOptionalAttributeValue(element, "target-name", "");
        final String targetSlot = getOptionalAttributeValue(element, "target-slot", "main");
        final String name = element.getAttributeValue("name");
        final String slot = getOptionalAttributeValue(element, "slot", "main");
        ModuleIdentifier moduleId = new ModuleIdentifier(targetName, targetSlot);
        result.identifier = new ModuleIdentifier(name, slot);
        result.dependencies.add(new ModuleParseResult.ModuleDependency(moduleId, false));
    }

    private void parseDependencies(Element element, ModuleParseResult result) {
        final Elements modules = element.getChildElements("module", element.getNamespaceURI());
        final int size = modules.size();
        for (int i = 0; i < size; i ++) {
            final Element moduleElement = modules.get(i);
            String name = getOptionalAttributeValue(moduleElement, "name", "");
            String slot = getOptionalAttributeValue(moduleElement, "slot", "main");
            boolean optional = Boolean.parseBoolean(getOptionalAttributeValue(moduleElement, "optional", "false"));
            ModuleIdentifier moduleId = new ModuleIdentifier(name, slot);
            result.dependencies.add(new ModuleParseResult.ModuleDependency(moduleId, optional));
        }
    }

    private void parseResources(Element element, ModuleParseResult result) {
        final Elements children = element.getChildElements();
        final int size = children.size();
        for (int i = 0; i < size; i ++) {
            final Element child = children.get(i);
            switch (child.getLocalName()) {
                case "resource-root": {
                    final Attribute attribute = child.getAttribute("path");
                    if (attribute != null) {
                        result.resourceRoots.add(new ResourceRoot(propertyReplacer.replaceProperties(attribute.getValue()), attribute));
                    }
                    break;
                }
                case "artifact": {
                    final Attribute attribute = child.getAttribute("name");
                    if (attribute != null) {
                        final String nameStr = attribute.getValue();
                        result.artifacts.add(parseArtifactName(nameStr, attribute));
                        break;
                    }
                }
            }
        }
    }

    private ModuleParseResult.ArtifactName parseArtifactName(String artifactName, final Attribute attribute) {
        final ModuleParseResult.ArtifactName name = parseOptionalArtifactName(artifactName, attribute);
        if (name == null) {
            return new ModuleParseResult.ArtifactName(propertyReplacer.replaceProperties(artifactName), null, attribute);
        }
        return name;
    }

    private static ModuleParseResult.ArtifactName parseOptionalArtifactName(String artifactName, final Attribute attribute) {
        if (artifactName.startsWith("${") && artifactName.endsWith("}")) {
            String ct = artifactName.substring(2, artifactName.length() - 1);
            String options = null;
            String artifactCoords = ct;
            if (ct.contains("?")) {
                String[] split = ct.split("\\?");
                options = split[1];
                artifactCoords = split[0];
            }
            return new ModuleParseResult.ArtifactName(artifactCoords, options, attribute);
        } else {
            return null;
        }
    }
}
