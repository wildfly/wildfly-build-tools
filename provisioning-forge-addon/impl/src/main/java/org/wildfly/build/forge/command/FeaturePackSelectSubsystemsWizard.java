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
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.wildfly.build.forge.command.spi.ServerProvisioningDescriptionChildResourceWizard;
import org.wildfly.build.forge.resource.FeaturePackResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionChildResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.pack.model.FeaturePackFactory;
import org.wildfly.build.provisioning.StandaloneAetherArtifactFileResolver;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import javax.inject.Inject;

/**
 * @author Eduardo Martins
 */
public class FeaturePackSelectSubsystemsWizard extends ServerProvisioningDescriptionChildResourceWizard {

    static final String SUBSYSTEMS_ATTRIBUTE_NAME = "FeaturePackSelectSubsystemsCommand.subsystems";

    static final String TRANSITIVE_ATTRIBUTE_NAME = "FeaturePackSelectSubsystemsCommand.transitive";

    @Inject
    @WithAttributes(label="Transitively provision subsystem dependencies", required=true)
    private UIInput<Boolean> transitive;

    @Override
    public Metadata getMetadata(UIContext context) {
        return super.getMetadata(context)
                .name("feature-pack-select-subsystem")
                .description("Selects feature pack's subsystems to provision");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(transitive);
    }

    @Override
    protected Iterable<? extends FeaturePackResource> getResourceValueChoices(ServerProvisioningDescriptionResource descriptionResource) {
        return descriptionResource.getFeaturePacks();
    }

    @Override
    protected String getItemLabel() {
        return "Feature Pack";
    }

    @Override
    protected NavigationResult next(UINavigationContext context, ServerProvisioningDescriptionResource descriptionResource, ServerProvisioningDescriptionChildResource childResource) throws Exception {
        FeaturePackResource featurePackResource = (FeaturePackResource) childResource;
        ServerProvisioningDescription.FeaturePack provisioningFeaturePack = featurePackResource.getUnderlyingResourceObject();
        FeaturePack featurePack = FeaturePackFactory.createPack(provisioningFeaturePack.getArtifact(), StandaloneAetherArtifactFileResolver.DEFAULT_INSTANCE, null);
        context.getUIContext().getAttributeMap().put(SUBSYSTEMS_ATTRIBUTE_NAME, featurePack.getSubsystems());
        if (transitive.hasValue()) {
            context.getUIContext().getAttributeMap().put(TRANSITIVE_ATTRIBUTE_NAME, transitive.getValue());
        }
        return Results.navigateTo(FeaturePackSelectSubsystemsStep.class);
    }
}
