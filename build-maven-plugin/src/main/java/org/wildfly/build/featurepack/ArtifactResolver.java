package org.wildfly.build.featurepack;

import java.io.File;

/**
 * @author Stuart Douglas
 */
public interface ArtifactResolver {

    String getVersion(String artifact);

    File getArtifact(String artifact);
}
