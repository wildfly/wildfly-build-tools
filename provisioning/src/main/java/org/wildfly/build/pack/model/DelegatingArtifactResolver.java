package org.wildfly.build.pack.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.wildfly.build.ArtifactResolver;

/**
 * @author Stuart Douglas
 */
public class DelegatingArtifactResolver implements ArtifactResolver {

    private final List<ArtifactResolver> resolvers = new ArrayList<>();

    public DelegatingArtifactResolver(ArtifactResolver... resolvers) {
        this.resolvers.addAll(Arrays.asList(resolvers));
    }

    @Override
    public Artifact getArtifact(String coords) {
        for(ArtifactResolver resolver : resolvers) {
            Artifact res = resolver.getArtifact(coords);
            if(res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public Artifact getArtifact(Artifact.GACE GACE) {
        for(ArtifactResolver resolver : resolvers) {
            Artifact res = resolver.getArtifact(GACE);
            if(res != null) {
                return res;
            }
        }
        return null;
    }
}
