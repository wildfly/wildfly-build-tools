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
