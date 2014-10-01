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

package org.wildfly.build.pack.model;

import org.wildfly.build.common.model.Config;
import org.wildfly.build.common.model.CopyArtifact;
import org.wildfly.build.common.model.FilePermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Representation of the feature pack metadata
 *
 * @author Eduardo Martins
 */
public class FeaturePackDescription {

    private final List<String> dependencies;
    private final Set<Artifact> artifactVersions = new TreeSet<>();
    private final Config config;
    private final List<CopyArtifact> copyArtifacts;
    private final List<FilePermission> filePermissions;

    public FeaturePackDescription() {
        this(new ArrayList<String>(), new Config(), new ArrayList<CopyArtifact>(), new ArrayList<FilePermission>());
    }

    public FeaturePackDescription(List<String> dependencies, Config config, List<CopyArtifact> copyArtifacts, List<FilePermission> filePermissions) {
        this.dependencies = dependencies;
        this.config = config;
        this.copyArtifacts = copyArtifacts;
        this.filePermissions = filePermissions;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public Set<Artifact> getArtifactVersions() {
        return artifactVersions;
    }

    public Config getConfig() {
        return config;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }

    public List<FilePermission> getFilePermissions() {
        return filePermissions;
    }
}
