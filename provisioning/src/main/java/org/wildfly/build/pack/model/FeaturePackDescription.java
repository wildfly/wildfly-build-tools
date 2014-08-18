/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.pack.model;

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
