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
package org.wildfly.build.pack.model;

import org.wildfly.build.util.xml.AttributeValue;
import org.wildfly.build.util.xml.ElementNode;
import org.wildfly.build.util.xml.FormattingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wildfly.build.pack.model.FeaturePackDescriptionXMLParser10.Attribute;
import static org.wildfly.build.pack.model.FeaturePackDescriptionXMLParser10.Element;

/**
 * Writes a feature pack description as XML.
 *
 * @author Eduardo Martins
 */
public class FeaturePackDescriptionXMLWriter10 {

    public static final FeaturePackDescriptionXMLWriter10 INSTANCE = new FeaturePackDescriptionXMLWriter10();

    private FeaturePackDescriptionXMLWriter10() {
    }

    public void write(FeaturePackDescription featurePackDescription, File outputFile) throws XMLStreamException, IOException {
        final ElementNode featurePackElementNode = new ElementNode(null, Element.FEATURE_PACK.getLocalName(), FeaturePackDescriptionXMLParser10.NAMESPACE_1_0);
        processDependencies(featurePackDescription.getDependencies(), featurePackElementNode);
        processArtifactVersions(featurePackDescription.getArtifactVersions(), featurePackElementNode);
        processConfig(featurePackDescription.getConfig(), featurePackElementNode);
        processCopyArtifacts(featurePackDescription.getCopyArtifacts(), featurePackElementNode);
        processFilePermissions(featurePackDescription.getFilePermissions(), featurePackElementNode);
        FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(new BufferedWriter(new FileWriter(outputFile))));
        try {
            writer.writeStartDocument();
            featurePackElementNode.marshall(writer);
            writer.writeEndDocument();
        } finally {
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
    }

    protected void processDependencies(List<String> dependencies, ElementNode featurePackElementNode) {
        if (!dependencies.isEmpty()) {
            final ElementNode dependenciesElementNode = new ElementNode(featurePackElementNode, Element.DEPENDENCIES.getLocalName());
            for (String artifactName : dependencies) {
                final ElementNode artifactElementNode = new ElementNode(dependenciesElementNode, Element.ARTIFACT.getLocalName());
                artifactElementNode.addAttribute(Attribute.NAME.getLocalName(), new AttributeValue(artifactName));
                dependenciesElementNode.addChild(artifactElementNode);
            }
            featurePackElementNode.addChild(dependenciesElementNode);
        }
    }

    protected void processArtifactVersions(Set<Artifact> artifactVersions, ElementNode featurePackElementNode) {
        if (!artifactVersions.isEmpty()) {
            final ElementNode versionsElementNode = new ElementNode(featurePackElementNode, Element.ARTIFACT_VERSIONS.getLocalName());
            for (Artifact artifact : artifactVersions) {
                processArtifact(artifact, versionsElementNode);
            }
            featurePackElementNode.addChild(versionsElementNode);
        }
    }

    protected void processArtifact(Artifact artifact, ElementNode versionsElementNode) {
        final ElementNode artifactElementNode = new ElementNode(versionsElementNode, Element.ARTIFACT.getLocalName());
        final Artifact.GACE GACE = artifact.getGACE();
        artifactElementNode.addAttribute(Attribute.GROUP_ID.getLocalName(), new AttributeValue(GACE.getGroupId()));
        artifactElementNode.addAttribute(Attribute.ARTIFACT_ID.getLocalName(), new AttributeValue(GACE.getArtifactId()));
        artifactElementNode.addAttribute(Attribute.VERSION.getLocalName(), new AttributeValue(artifact.getVersion()));
        if (GACE.getClassifier() != null) {
            artifactElementNode.addAttribute(Attribute.CLASSIFIER.getLocalName(), new AttributeValue(GACE.getClassifier()));
        }
        if (GACE.getExtension() != null) {
            artifactElementNode.addAttribute(Attribute.EXTENSION.getLocalName(), new AttributeValue(GACE.getExtension()));
        }
        versionsElementNode.addChild(artifactElementNode);
    }

    protected void processConfig(Config config, ElementNode featurePackElementNode) {
        if (!config.getStandaloneConfigFiles().isEmpty() || !config.getDomainConfigFiles().isEmpty()) {
            final ElementNode configElementNode = new ElementNode(featurePackElementNode, Element.CONFIG.getLocalName());
            for (ConfigFile configFile : config.getStandaloneConfigFiles()) {
                final ElementNode standaloneElementNode = new ElementNode(featurePackElementNode, Element.STANDALONE.getLocalName());
                processConfigFile(configFile, standaloneElementNode);
                configElementNode.addChild(standaloneElementNode);
            }
            for (ConfigFile configFile : config.getDomainConfigFiles()) {
                final ElementNode domainElementNode = new ElementNode(featurePackElementNode, Element.DOMAIN.getLocalName());
                processConfigFile(configFile, domainElementNode);
                configElementNode.addChild(domainElementNode);
            }
            featurePackElementNode.addChild(configElementNode);
        }
    }

    protected void processConfigFile(ConfigFile configFile, ElementNode configElementNode) {
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

    protected void processCopyArtifacts(List<CopyArtifact> copyArtifacts, ElementNode featurePackElementNode) {
        if (!copyArtifacts.isEmpty()) {
            final ElementNode artifactsElementNode = new ElementNode(featurePackElementNode, Element.COPY_ARTIFACTS.getLocalName());
            for (CopyArtifact copyArtifact : copyArtifacts) {
                final ElementNode copyArtifactElementNode = new ElementNode(featurePackElementNode, Element.COPY_ARTIFACT.getLocalName());
                processCopyArtifact(copyArtifact, copyArtifactElementNode);
                artifactsElementNode.addChild(copyArtifactElementNode);
            }
            featurePackElementNode.addChild(artifactsElementNode);
        }
    }

    protected void processCopyArtifact(CopyArtifact copyArtifact, ElementNode copyArtifactElementNode) {
        processFilters(copyArtifact.getFilters(), copyArtifactElementNode);
        copyArtifactElementNode.addAttribute(Attribute.ARTIFACT.getLocalName(), new AttributeValue(copyArtifact.getArtifact()));
        copyArtifactElementNode.addAttribute(Attribute.TO_LOCATION.getLocalName(), new AttributeValue(copyArtifact.getToLocation()));
        if (copyArtifact.isExtract()) {
            copyArtifactElementNode.addAttribute(Attribute.EXTRACT.getLocalName(), new AttributeValue(Boolean.toString(copyArtifact.isExtract())));
        }
    }

    protected void processFilters(List<FileFilter> fileFilters, ElementNode elementNode) {
        if (!fileFilters.isEmpty()) {
            for (FileFilter fileFilter : fileFilters) {
                processFileFilter(fileFilter, elementNode);
            }
        }
    }

    protected void processFileFilter(FileFilter fileFilter, ElementNode elementNode) {
        final ElementNode fileFilterElementNode = new ElementNode(elementNode, Element.FILTER.getLocalName());
        fileFilterElementNode.addAttribute(Attribute.PATTERN.getLocalName(), new AttributeValue(fileFilter.getPattern().pattern()));
        fileFilterElementNode.addAttribute(Attribute.INCLUDE.getLocalName(), new AttributeValue(Boolean.toString(fileFilter.isInclude())));
        elementNode.addChild(fileFilterElementNode);
    }

    protected void processFilePermissions(List<FilePermission> filePermissions, ElementNode wildflyPackElementNode) {
        if (!filePermissions.isEmpty()) {
            final ElementNode filePermissionsElementNode = new ElementNode(wildflyPackElementNode, Element.FILE_PERMISSIONS.getLocalName());
            for (FilePermission filePermission : filePermissions) {
                processFilePermission(filePermission, filePermissionsElementNode);
            }
            wildflyPackElementNode.addChild(filePermissionsElementNode);
        }
    }

    protected void processFilePermission(FilePermission filePermission, ElementNode filePermissionsElementNode) {
        final ElementNode filePermissionElementNode = new ElementNode(filePermissionsElementNode, Element.PERMISSION.getLocalName());
        processFilters(filePermission.getFilters(), filePermissionElementNode);
        filePermissionElementNode.addAttribute(Attribute.VALUE.getLocalName(), new AttributeValue(filePermission.getValue()));
        filePermissionsElementNode.addChild(filePermissionElementNode);
    }
}
