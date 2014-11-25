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

import org.jboss.forge.addon.resource.ResourceFactory;
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
import org.wildfly.build.provisioning.ProvisionCommand;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Eduardo Martins
 */
public class ServerProvisioningConfigBuildCommand extends ServerProvisioningDescriptionResourceSelectedCommand {

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    @WithAttributes(label="Build directory name.", defaultValue = ProvisionCommand.DEFAULT_BUILD_DIR)
    private UIInput<String> buildDir;

    @Inject
    @WithAttributes(label="Server name.", defaultValue = ProvisionCommand.DEFAULT_SERVER_NAME)
    private UIInput<String> serverName;

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("server-provisioning-config-build")
                .description("Provision of the configured server");
    }
    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(buildDir).add(serverName);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(context.getUIContext());
        final File buildDir = new File(descriptionResource.getUnderlyingResourceObject().getParent(), this.buildDir.getValue());
        final String serverName = this.serverName.getValue();
        ProvisionCommand.provision(descriptionResource.getServerProvisioningDescription(), buildDir, serverName);
        return Results.success();
    }
}
