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
package org.wildfly.build.forge.command.spi;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionChildResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public abstract class ServerProvisioningDescriptionChildResourceWizard extends ServerProvisioningDescriptionResourceSelectedCommand implements UIWizard {

    @Inject
    protected UISelectOne<ServerProvisioningDescriptionChildResource> resource;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(builder.getUIContext());
        builder.add(resource.setValueChoices((Iterable<ServerProvisioningDescriptionChildResource>) getResourceValueChoices(descriptionResource)).setLabel(getItemLabel()).setRequired(true));
    }

    protected abstract Iterable<? extends ServerProvisioningDescriptionChildResource> getResourceValueChoices(ServerProvisioningDescriptionResource descriptionResource);

    protected abstract String getItemLabel();

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(context.getUIContext());
        context.getUIContext().setSelection(resource.getValue());
        return next(context, descriptionResource, resource.getValue());
    }

    protected abstract NavigationResult next(UINavigationContext context, ServerProvisioningDescriptionResource descriptionResource, ServerProvisioningDescriptionChildResource childResource) throws Exception;

}
