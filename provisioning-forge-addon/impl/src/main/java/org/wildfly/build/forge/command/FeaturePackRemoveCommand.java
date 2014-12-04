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

import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.util.Metadata;
import org.wildfly.build.forge.command.spi.ServerProvisioningDescriptionChildResourceRemoveCommand;
import org.wildfly.build.forge.resource.FeaturePackResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;

/**
 * @author Eduardo Martins
 */
public class FeaturePackRemoveCommand extends ServerProvisioningDescriptionChildResourceRemoveCommand {

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("feature-pack-remove")
                .description("Remove Feature Pack from WildFly Server Provisioning");
    }

    @Override
    protected Iterable<? extends FeaturePackResource> getResourceValueChoices(ServerProvisioningDescriptionResource descriptionResource) {
        return descriptionResource.getFeaturePacks();
    }

    @Override
    protected String getItemLabel() {
        return "Feature Pack";
    }
}
