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

import org.jboss.forge.addon.resource.AbstractFileResource;
import org.jboss.forge.addon.resource.AbstractResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceException;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionXmlWriter;
import org.wildfly.build.util.MapPropertyResolver;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The impl of {@link ServerProvisioningDescriptionResource}.
 * @author Eduardo Martins
 */
public class ServerProvisioningDescriptionResourceImpl extends AbstractFileResource<ServerProvisioningDescriptionResource> implements ServerProvisioningDescriptionResource {

    /**
     * the description within the resource
     */
    private final ServerProvisioningDescription description;

    /**
     *
     * @param factory
     * @param file
     */
    public ServerProvisioningDescriptionResourceImpl(final ResourceFactory factory, final File file) {
        super(factory, file);
        if (exists()) {
            description = parseXML();
        } else {
            description = new ServerProvisioningDescription();
        }
    }

    @Override
    public Resource<File> createFrom(File file) {
        return new ServerProvisioningDescriptionResourceImpl(getResourceFactory(), file);
    }

    @Override
    public Resource<?> getChild(String name) {
        for (Resource<?> child : listResources()) {
            if (child.getName().trim().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected List<Resource<?>> doListResources() {
        final List<Resource<?>> children = new ArrayList<>();
        for (FeaturePackResourceImpl resource : getFeaturePacks()) {
            children.add(new FullListResourceAdapter(resource));
        }
        for (VersionOverrideResourceImpl resource : getVersionOverrides()) {
            children.add(new FullListResourceAdapter(resource));
        }
        for (CopyArtifactResourceImpl resource : getCopyArtifacts()) {
            children.add(new FullListResourceAdapter(resource));
        }
        return children;
    }


    @Override
    public List<CopyArtifactResourceImpl> getCopyArtifacts() {
        final List<CopyArtifactResourceImpl> result = new ArrayList<>();
        List<CopyArtifact> copyArtifacts = getServerProvisioningDescription().getCopyArtifacts();
        if (copyArtifacts != null) {
            int i = 0;
            for (CopyArtifact copyArtifact : copyArtifacts) {
                result.add(new CopyArtifactResourceImpl(this, "copy-artifacts/"+ i, copyArtifact));
                i++;
            }
        }
        return result;
    }

    @Override
    public List<FeaturePackResourceImpl> getFeaturePacks() {
        final List<FeaturePackResourceImpl> result = new ArrayList<>();
        List<ServerProvisioningDescription.FeaturePack> featurePacks = getServerProvisioningDescription().getFeaturePacks();
        if (featurePacks != null) {
            int i = 0;
            for (ServerProvisioningDescription.FeaturePack featurePack : featurePacks) {
                result.add(new FeaturePackResourceImpl(this, "feature-packs/"+ i, featurePack));
                i++;
            }
        }
        return result;
    }

    @Override
    public List<VersionOverrideResourceImpl> getVersionOverrides() {
        final List<VersionOverrideResourceImpl> result = new ArrayList<>();
        List<Artifact> versionOverrides = getServerProvisioningDescription().getVersionOverrides();
        if (versionOverrides != null) {
            int i = 0;
            for (Artifact versionOverride : versionOverrides) {
                result.add(new VersionOverrideResourceImpl(this, "version-overrides/"+ i, versionOverride));
                i++;
            }
        }
        return result;
    }

    @Override
    public ServerProvisioningDescriptionResource writeXML() {
        try {
            ServerProvisioningDescriptionXmlWriter.INSTANCE.writeContent(getUnderlyingResourceObject(), description);
            return this;
        } catch (Throwable e) {
            throw new ResourceException("Failed to write the server provisioning description", e);
        }
    }

    @Override
    public ServerProvisioningDescription getServerProvisioningDescription() {
        return description;
    }

    private ServerProvisioningDescription parseXML() {
        try (InputStream in = getResourceInputStream()) {
            return new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(System.getProperties())).parse(in);
        } catch (Throwable e) {
            throw new ResourceException("Failed to read the server provisioning description", e);
        }
    }

    private static class FullListResourceAdapter<T> extends AbstractResource<T> {

        private final ServerProvisioningDescriptionChildVirtualResource<T> resource;

        private FullListResourceAdapter(ServerProvisioningDescriptionChildVirtualResource<T> resource) {
            super(resource.getResourceFactory(), resource.getParent());
            this.resource = resource;
        }

        @Override
        protected List<Resource<?>> doListResources() {
            return resource.doListResources();
        }

        @Override
        public boolean delete() throws UnsupportedOperationException {
            return resource.delete();
        }

        @Override
        public boolean delete(boolean recursive) throws UnsupportedOperationException {
            return resource.delete(recursive);
        }

        @Override
        public String getName() {
            return resource.toString();
        }

        @Override
        public Resource<T> createFrom(T file) {
            return resource.createFrom(file);
        }

        @Override
        public T getUnderlyingResourceObject() {
            return resource.getUnderlyingResourceObject();
        }

        @Override
        public InputStream getResourceInputStream() {
            return resource.getResourceInputStream();
        }

        @Override
        public Resource<?> getChild(String name) {
            return resource.getChild(name);
        }

        @Override
        public boolean exists() {
            return resource.exists();
        }
    }
}
