package org.wildfly.build;

import org.wildfly.build.pack.model.Artifact;

import java.io.File;

/**
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public interface ArtifactFileResolver {

    /**
     *
     * @param artifactCoords The artifact coordinates in the format <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>, must not be null.
     * @return
     */
    File getArtifactFile(String artifactCoords);

    /**
     *
     * @param artifact
     * @return
     */
    File getArtifactFile(Artifact artifact);
}
