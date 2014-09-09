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

package org.wildfly.build.featurepack.model;

import org.wildfly.build.pack.model.Config;
import org.wildfly.build.pack.model.CopyArtifact;
import org.wildfly.build.pack.model.FileFilter;
import org.wildfly.build.pack.model.FilePermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the feature pack build config
 *
 * @author Stuart Douglas
 */
public class FeaturePackBuild {

    private final List<String> dependencies = new ArrayList<>();
    private final Config config = new Config();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();
    private final List<FilePermission> filePermissions = new ArrayList<>();
    private final List<String> mkDirs = new ArrayList<>();
    private final List<FileFilter> windows = new ArrayList<>();
    private final List<FileFilter> unix = new ArrayList<>();

    public List<String> getDependencies() {
        return dependencies;
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

    public List<String> getMkDirs() {
        return mkDirs;
    }

    public List<FileFilter> getWindows() {
        return windows;
    }

    public List<FileFilter> getUnix() {
        return unix;
    }
}
