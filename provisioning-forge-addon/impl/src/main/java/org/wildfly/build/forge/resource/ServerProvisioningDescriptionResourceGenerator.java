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
package org.wildfly.build.forge.resource;

import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.ResourceGenerator;

import java.io.File;

/**
 * The {@link ServerProvisioningDescriptionResource}'s {@link org.jboss.forge.addon.resource.ResourceGenerator}.
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionResourceGenerator implements ResourceGenerator<ServerProvisioningDescriptionResource, File> {

    @Override
    public boolean handles(Class<?> type, Object resource) {
        return (resource instanceof File) && ((File) resource).getName().endsWith(".xml");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Resource<File>> T getResource(ResourceFactory factory, Class<ServerProvisioningDescriptionResource> type, File resource) {
        return (T) new ServerProvisioningDescriptionResourceImpl(factory, resource);
    }

    @Override
    public <T extends Resource<File>> Class<?> getResourceType(ResourceFactory factory, Class<ServerProvisioningDescriptionResource> type, File resource) {
        return ServerProvisioningDescriptionResource.class;
    }
}
