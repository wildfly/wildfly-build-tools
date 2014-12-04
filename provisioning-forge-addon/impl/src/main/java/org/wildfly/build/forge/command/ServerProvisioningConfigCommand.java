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

import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.wildfly.build.forge.command.spi.AbstractUICommand;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;
import org.wildfly.build.provisioning.ProvisionCommand;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Eduardo Martins
 */
public class ServerProvisioningConfigCommand extends AbstractUICommand implements UICommand {

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    @WithAttributes(label="Server Provisioning Description XML File", required=true, defaultValue = ProvisionCommand.DEFAULT_CONFIG_FILE)
    private UIInput<String> fileName;

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("server-provisioning-config")
                .description("WildFly Server Provisioning Configuration");
    }
    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(fileName);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final File currentDir = ((DirectoryResource) context.getUIContext().getInitialSelection().get()).getUnderlyingResourceObject();
        final ServerProvisioningDescriptionResource resource = resourceFactory.create(ServerProvisioningDescriptionResource.class, new File(currentDir, fileName.getValue()));
        boolean exists = resource.exists();
        if (!exists) {
            resource.writeXML();
        }
        context.getUIContext().setSelection(resource);
        return Results.success("Server provisioning description " +resource.getUnderlyingResourceObject()+(exists ? " parsed." : " created."));
    }

    @Override
    public boolean isEnabled(UIContext context) {
        return super.isEnabled(context) && context.getInitialSelection().get() instanceof DirectoryResource;
    }
}
