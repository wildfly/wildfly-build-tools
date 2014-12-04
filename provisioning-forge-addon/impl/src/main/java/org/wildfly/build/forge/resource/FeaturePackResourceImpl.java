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

import org.wildfly.build.provisioning.model.ServerProvisioningDescription;

import java.util.Iterator;

/**
 * The impl of {@link FeaturePackResource}.
 * @author Eduardo Martins
 */
public class FeaturePackResourceImpl extends ServerProvisioningDescriptionChildVirtualResource<ServerProvisioningDescription.FeaturePack> implements FeaturePackResource {

    public FeaturePackResourceImpl(final ServerProvisioningDescriptionResource parent, String name, ServerProvisioningDescription.FeaturePack featurePack) {
        super(name, parent, featurePack);
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        final ServerProvisioningDescriptionResource descriptionResource = getParent();
        final Iterator<ServerProvisioningDescription.FeaturePack> iterator = descriptionResource.getServerProvisioningDescription().getFeaturePacks().iterator();
        while(iterator.hasNext()) {
            if (iterator.next().equals(getUnderlyingResourceObject())) {
                iterator.remove();
                descriptionResource.writeXML();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean delete(boolean recursive) throws UnsupportedOperationException {
        return delete();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        ServerProvisioningDescription.FeaturePack featurePack = getUnderlyingResourceObject();
        sb.append(" --> artifact = ").append(featurePack.getArtifact().getGACE()).append(", version = ").append(featurePack.getArtifact().getVersion());
        if (featurePack.getSubsystems() != null) {
            sb.append(", subsystems = ").append(featurePack.getSubsystems());
        }
        return sb.toString();
    }
}