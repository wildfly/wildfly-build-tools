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
package org.wildfly.build.test;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.pack.model.DelegatingArtifactResolver;
import org.wildfly.build.pack.model.FeaturePack;
import org.wildfly.build.util.MapArtifactResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests wrt {@link org.wildfly.build.pack.model.Artifact} resolving.
 * @author Eduardo Martins
 */
public class ArtifactResolvingTestCase {

    /**
     * Test artifact resolving using the standard {@link org.wildfly.build.util.MapArtifactResolver}.
     */
    @Test
    public void testMapArtifactResolver() {
        final String artifactRef = "groupId:artifactId:1.0";
        final Artifact artifact = Artifact.fromString(artifactRef);
        final Map<String, Artifact> artifactRefs = new HashMap<>();
        artifactRefs.put(artifact.getDefaultArtifactRef(), artifact);
        MapArtifactResolver artifactRefsResolver = new MapArtifactResolver(artifactRefs);
        Assert.assertEquals("failed to resolve using artifact ref name with version", Artifact.resolve(artifactRef, artifactRefsResolver), artifact);
        Assert.assertEquals("failed to resolve using standard artifact ref, i.e. containing no version", Artifact.resolve(artifact.getDefaultArtifactRef(), artifactRefsResolver), artifact);
    }

    /**
     * Test artifact resolving using {@link org.wildfly.build.pack.model.DelegatingArtifactResolver}.
     */
    @Test
    public void testDelegatingOverride() {
        final String artifactRef1 = "groupId:artifactId1:1.0";
        final Artifact artifact1 = Artifact.fromString(artifactRef1);
        final Map<String, Artifact> artifactMap1 = new HashMap<>();
        artifactMap1.put(artifact1.getDefaultArtifactRef(), artifact1);
        final MapArtifactResolver artifactResolver1 = new MapArtifactResolver(artifactMap1);
        final String artifactRef2 = "groupId:artifactId2:1.0";
        final Artifact artifact2 = Artifact.fromString(artifactRef2);
        final Map<String, Artifact> artifactMap2 = new HashMap<>();
        artifactMap2.put(artifact2.getDefaultArtifactRef(), artifact2);
        final MapArtifactResolver artifactResolver2 = new MapArtifactResolver(artifactMap2);
        final ArtifactResolver delegatingArtifactResolver = new DelegatingArtifactResolver(artifactResolver1, artifactResolver2);
        Assert.assertEquals("failed to resolve using artifact ref name with version", Artifact.resolve(artifactRef1, delegatingArtifactResolver), artifact1);
        Assert.assertEquals("failed to resolve using standard artifact ref, i.e. containing no version", Artifact.resolve(artifact1.getDefaultArtifactRef(), delegatingArtifactResolver), artifact1);
        Assert.assertEquals("failed to resolve using artifact ref name with version", Artifact.resolve(artifactRef2, delegatingArtifactResolver), artifact2);
        Assert.assertEquals("failed to resolve using standard artifact ref, i.e. containing no version", Artifact.resolve(artifact2.getDefaultArtifactRef(), delegatingArtifactResolver), artifact2);
    }

    /**
     * Test resolving through {@link org.wildfly.build.pack.model.Artifact#resolve(String, org.wildfly.build.ArtifactResolver)}, with an empty resolver.
     */
    @Test
    public void testArtifactResolve() {
        final String groupId = "groupId";
        final String artifactId = "artifactId";
        final String version = "version";
        Artifact artifact = Artifact.resolve(groupId+':'+artifactId+':'+version, new MapArtifactResolver());
        Assert.assertNotNull(artifact);
        Assert.assertEquals(artifact.getGroupId(), groupId);
        Assert.assertEquals(artifact.getArtifactId(), artifactId);
        Assert.assertEquals(artifact.getVersion(), version);
    }

    /**
     * Test resolving through {@link org.wildfly.build.pack.model.FeaturePack#resolveArtifact(String, org.wildfly.build.ArtifactResolver)}, with an empty resolver.
     */
    @Test
    public void testFeaturePackArtifactResolve() {
        final String groupId = "groupId";
        final String artifactId = "artifactId";
        final String version = "version";
        Artifact artifact = FeaturePack.resolveArtifact(groupId + ':' + artifactId + ':' + version, new MapArtifactResolver());
        Assert.assertNotNull(artifact);
        Assert.assertEquals(artifact.getGroupId(), groupId);
        Assert.assertEquals(artifact.getArtifactId(), artifactId);
        Assert.assertEquals(artifact.getVersion(), version);
        Assert.assertEquals(artifact.getExtension(), "zip");
    }
}
