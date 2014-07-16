package org.wildfly.build.featurepack.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the feature pack build config
 *
 * @author Stuart Douglas
 */
public class FeaturePackBuild {

    private final List<String> dependencies = new ArrayList<>();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();
    private final List<FilePermission> filePermissions = new ArrayList<>();
    private final List<String> mkDirs = new ArrayList<>();
    private final List<FileFilter> windows = new ArrayList<>();
    private final List<FileFilter> unix = new ArrayList<>();

    public List<String> getDependencies() {
        return dependencies;
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
