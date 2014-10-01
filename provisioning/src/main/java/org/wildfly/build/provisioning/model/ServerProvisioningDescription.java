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

package org.wildfly.build.provisioning.model;

import java.util.ArrayList;
import java.util.List;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.common.model.CopyArtifact;

/**
 * Representation of the server provisioning config
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningDescription {

    private final List<Artifact> featurePacks = new ArrayList<>();
    private final List<Artifact> versionOverrides = new ArrayList<>();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();

    private boolean copyModuleArtifacts;

    private boolean extractSchemas;

    public List<Artifact> getFeaturePacks() {
        return featurePacks;
    }

    public boolean isCopyModuleArtifacts() {
        return copyModuleArtifacts;
    }

    public void setCopyModuleArtifacts(boolean copyModuleArtifacts) {
        this.copyModuleArtifacts = copyModuleArtifacts;
    }

    public boolean isExtractSchemas() {
        return extractSchemas;
    }

    public void setExtractSchemas(boolean extractSchemas) {
        this.extractSchemas = extractSchemas;
    }

    public List<Artifact> getVersionOverrides() {
        return versionOverrides;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

}
