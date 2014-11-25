package org.wildfly.build.forge.command.spi;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.result.Result;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionChildResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public abstract class ServerProvisioningDescriptionChildResourceCommand extends ServerProvisioningDescriptionResourceSelectedCommand {

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
        final ServerProvisioningDescriptionResource descriptionResource = getSelectedServerProvisioningDescriptionResource(context.getUIContext());
        return execute(context, descriptionResource, resource.getValue());
    }

    protected abstract Result execute(UIExecutionContext uiExecutionContext, ServerProvisioningDescriptionResource descriptionResource, ServerProvisioningDescriptionChildResource childResource) throws Exception;

}
