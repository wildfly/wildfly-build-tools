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
import org.jboss.forge.addon.resource.VirtualResource;

import java.util.Collections;
import java.util.List;

/**
 * A {@link org.jboss.forge.addon.resource.VirtualResource} which is a child of a {@link ServerProvisioningDescriptionResource}.
 *
 * @author Eduardo Martins
 */
public abstract class ServerProvisioningDescriptionChildVirtualResource<T> extends VirtualResource<T> implements ServerProvisioningDescriptionChildResource<T> {

    private final T t;

    private final String name;

    public ServerProvisioningDescriptionChildVirtualResource(String name, ServerProvisioningDescriptionResource parent, T t) {
        super(parent.getResourceFactory(), parent);
        this.t = t;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ServerProvisioningDescriptionResource getParent() {
        return (ServerProvisioningDescriptionResource) super.getParent();
    }

    @Override
    public T getUnderlyingResourceObject() {
        return t;
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return Collections.emptyList();
    }

    @Override
    public Resource<?> getChild(String name) {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
