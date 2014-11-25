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

import org.wildfly.build.pack.model.Artifact;

/**
 * The {@link org.jboss.forge.addon.resource.Resource} for a {@link org.wildfly.build.provisioning.model.ServerProvisioningDescription} artifact version override.
 * @author Eduardo Martins
 */
public interface VersionOverrideResource extends ServerProvisioningDescriptionChildResource<Artifact> {
}
