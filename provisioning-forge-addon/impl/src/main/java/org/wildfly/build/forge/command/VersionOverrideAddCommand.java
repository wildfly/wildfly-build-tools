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
package org.wildfly.build.forge.command;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.wildfly.build.forge.command.spi.ServerProvisioningDescriptionResourceSelectedCommand;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public class VersionOverrideAddCommand extends ServerProvisioningDescriptionResourceSelectedCommand {

    @Inject
    @WithAttributes(label="Artifact's groupId", required=true)
    private UIInput<String> groupId;

    @Inject
    @WithAttributes(label="Artifact's artifactId", required=true)
    private UIInput<String> artifactId;

    @Inject
    @WithAttributes(label="Artifact's version", required=true)
    private UIInput<String> version;

    @Inject
    @WithAttributes(label="Artifact's classifier", required=false)
    private UIInput<String> classifier;

    @Inject
    @WithAttributes(label="Artifact's extension", required=false)
    private UIInput<String> extension;

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("version-override-add")
                .description("Add an artifact version override to WildFly Server Provisioning");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(groupId).add(artifactId).add(classifier).add(extension).add(version);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(context.getUIContext());
        final Artifact artifact = new Artifact(groupId.getValue(), artifactId.getValue(), classifier.getValue(), extension.getValue(), version.getValue());
        final ServerProvisioningDescription description = descriptionResource.getServerProvisioningDescription();
        description.getVersionOverrides().add(artifact);
        descriptionResource.writeXML();
        return Results.success("Added version override "+artifact);
    }
}
