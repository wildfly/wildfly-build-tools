package org.wildfly.build.provisioning.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of the server provisioning config
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningDescription {

    private final List<String> featurePacks = new ArrayList<>();

    private boolean copyModuleArtifacts;

    private boolean extractSchemas;

    public List<String> getFeaturePacks() {
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
}
