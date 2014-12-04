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
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.forge.command.spi.ServerProvisioningDescriptionResourceSelectedCommand;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;
import org.wildfly.build.pack.model.Artifact;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public class CopyArtifactAddCommand extends ServerProvisioningDescriptionResourceSelectedCommand {

    @Inject
    @WithAttributes(label="Artifact coords (groupId:artifactId:version)", required=true)
    private UIInput<String> artifact;

    @Inject
    @WithAttributes(label="Target location", required=true)
    private UIInput<String> location;

    @Inject
    @WithAttributes(label="Extract artifact", required=false)
    private UIInput<Boolean> extract;

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("copy-artifact-add")
                .description("Add a copy of an artifact to WildFly Server Provisioning");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(artifact).add(location).add(extract);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(context.getUIContext());
        final Artifact artifact = Artifact.parse(this.artifact.getValue());
        final String location = this.location.getValue();
        final boolean extract = this.extract.getValue();
        descriptionResource.getServerProvisioningDescription().getCopyArtifacts().add(new CopyArtifact(artifact.getGACE().toString(), location, extract));
        descriptionResource.getServerProvisioningDescription().getVersionOverrides().add(artifact);
        descriptionResource.writeXML();
        return Results.success("Artifact "+artifact+" will be copied to "+location);
    }

}
