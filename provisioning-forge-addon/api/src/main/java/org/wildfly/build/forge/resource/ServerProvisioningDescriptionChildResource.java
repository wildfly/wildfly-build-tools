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

/**
 * A {@link org.jboss.forge.addon.resource.Resource} that is a child of an {@link ServerProvisioningDescriptionResource}
 * @author Eduardo Martins
 */
public interface ServerProvisioningDescriptionChildResource<T> extends Resource<T> {

    /**
     * Retrieves the resource's parent.
     * @return
     */
    ServerProvisioningDescriptionResource getParent();

}
