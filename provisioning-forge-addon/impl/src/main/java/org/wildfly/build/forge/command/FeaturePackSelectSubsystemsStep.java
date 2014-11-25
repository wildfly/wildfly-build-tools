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
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.wildfly.build.forge.command.spi.AbstractUICommand;
import org.wildfly.build.forge.resource.FeaturePackResource;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eduardo Martins
 */
public class FeaturePackSelectSubsystemsStep extends AbstractUICommand implements UIWizardStep {

    @Inject
    @WithAttributes(label="Subsystem to provision", required=true)
    private UISelectOne<String> subsystem;

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(subsystem.setValueChoices((Iterable<String>) builder.getUIContext().getAttributeMap().get(FeaturePackSelectSubsystemsWizard.SUBSYSTEMS_ATTRIBUTE_NAME)));
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final String subsystem = this.subsystem.getValue();
        Boolean transitive = (Boolean) context.getUIContext().getAttributeMap().get(FeaturePackSelectSubsystemsWizard.TRANSITIVE_ATTRIBUTE_NAME);
        if (transitive == null) {
            transitive = context.getPrompt().promptBoolean("Included subsystem's transitive dependencies?");
        }
        final FeaturePackResource resource = (FeaturePackResource) context.getUIContext().getSelection().get();
        context.getUIContext().setSelection(resource.getParent());
        List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = resource.getUnderlyingResourceObject().getSubsystems();
        if (subsystems == null) {
            subsystems = new ArrayList<>();
        }
        subsystems.add(new ServerProvisioningDescription.FeaturePack.Subsystem(subsystem, transitive));
        resource.getUnderlyingResourceObject().setSubsystems(subsystems);
        resource.getParent().writeXML();
        return Results.success("Subsystems selected (after adding "+subsystem+"): "+subsystems);
    }
}
