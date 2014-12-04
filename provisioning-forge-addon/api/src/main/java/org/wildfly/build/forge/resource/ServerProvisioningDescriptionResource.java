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
package org.wildfly.build.forge.resource;

import org.jboss.forge.addon.resource.FileResource;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import java.util.List;

/**
 * The {@link org.jboss.forge.addon.resource.FileResource} for a {@link org.wildfly.build.provisioning.model.ServerProvisioningDescription}.
 * @author Eduardo Martins
 */
public interface ServerProvisioningDescriptionResource extends FileResource<ServerProvisioningDescriptionResource> {

    /**
     * Writes resource's content to the underlying XML file.
     * @return
     */
    ServerProvisioningDescriptionResource writeXML();

    /**
     * Retrieves the resource's {@link org.wildfly.build.provisioning.model.ServerProvisioningDescription}.
     */
    ServerProvisioningDescription getServerProvisioningDescription();

    /**
     * Retrieves child {@link org.wildfly.build.forge.resource.FeaturePackResource}s.
     * @return
     */
    List<? extends FeaturePackResource> getFeaturePacks();

    /**
     * Retrieves child {@link org.wildfly.build.forge.resource.CopyArtifactResource}s.
     * @return
     */
    List<? extends CopyArtifactResource> getCopyArtifacts();

    /**
     * Retrieves child {@link org.wildfly.build.forge.resource.VersionOverrideResource}s.
     * @return
     */
    List<? extends VersionOverrideResource> getVersionOverrides();
}
