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

import org.wildfly.build.common.model.CopyArtifact;

import java.util.Iterator;

/**
 * The impl of {@link org.wildfly.build.forge.resource.FeaturePackResource}.
 * @author Eduardo Martins
 */
public class CopyArtifactResourceImpl extends ServerProvisioningDescriptionChildVirtualResource<CopyArtifact> implements CopyArtifactResource {

    /**
     *
     * @param parent
     * @param copyArtifact
     */
    public CopyArtifactResourceImpl(final ServerProvisioningDescriptionResource parent, String name, CopyArtifact copyArtifact) {
        super(name, parent, copyArtifact);
    }

    @Override
    public boolean delete() throws UnsupportedOperationException {
        final ServerProvisioningDescriptionResource descriptionResource = getParent();
        final Iterator<CopyArtifact> iterator = descriptionResource.getServerProvisioningDescription().getCopyArtifacts().iterator();
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
        CopyArtifact artifact = getUnderlyingResourceObject();
        sb.append(" --> artifact = ").append(artifact.getArtifact()).append(", toLocation = ").append(artifact.getToLocation()).append(", extract = ").append(artifact.isExtract());
        return sb.toString();
    }
}