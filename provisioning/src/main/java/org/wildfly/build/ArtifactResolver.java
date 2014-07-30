package org.wildfly.build;

import org.wildfly.build.pack.model.Artifact;

/**
 * @author Eduardo Martins
 */
public interface ArtifactResolver {

    /**
     * @param coords The artifact coordinates in the format
     *            {@code <groupId>:<artifactId>[::<classifier>]}, must not be {@code null}.
     * @return
     */
    Artifact getArtifact(String coords);

    /**
     *
     * @param GACE
     * @return
     */
    Artifact getArtifact(Artifact.GACE GACE);

}
