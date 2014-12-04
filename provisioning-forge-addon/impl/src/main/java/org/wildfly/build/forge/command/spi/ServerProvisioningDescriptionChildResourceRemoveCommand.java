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

import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionChildResource;
import org.wildfly.build.forge.resource.ServerProvisioningDescriptionResource;

/**
 * @author Eduardo Martins
 */
public abstract class ServerProvisioningDescriptionChildResourceRemoveCommand extends ServerProvisioningDescriptionChildResourceCommand {

    protected Result execute(UIExecutionContext uiExecutionContext, ServerProvisioningDescriptionResource descriptionResource, ServerProvisioningDescriptionChildResource childResource) {
        if (childResource.delete()) {
            descriptionResource.writeXML();
        }
        return Results.success(childResource + " removed");
    }

}
