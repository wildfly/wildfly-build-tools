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
import org.wildfly.build.util.MapArtifactResolver;
import org.wildfly.build.util.PropertiesArtifactVersionOverrider;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tests wrt {@link org.wildfly.build.pack.model.Artifact} version override.
 * @author Eduardo Martins
 */
public class ArtifactVersionOverridingTestCase {


    /**
     * Test artifact override using {@link org.wildfly.build.util.PropertiesArtifactVersionOverrider}.
     */
    @Test
    public void testPropertyArtifactVersionOverride() {
        final String artifactRef = "groupId:artifactId:1.0";
        final Artifact artifact = Artifact.fromString(artifactRef);
        final Properties properties = new Properties();
        final String overrideVersion = "2.0";
        properties.setProperty(PropertiesArtifactVersionOverrider.OVERRIDE_PROPERTY_NAME_PREFIX + artifact.getDefaultArtifactRef(), overrideVersion);
        final PropertiesArtifactVersionOverrider propertiesArtifactVersionOverrider = new PropertiesArtifactVersionOverrider(properties);
        propertiesArtifactVersionOverrider.override(artifact.getDefaultArtifactRef(), artifact);
        Assert.assertEquals("failed to override artifact version", artifact.getVersion(), overrideVersion);
    }

    /**
     * Test artifact resolver override using {@link org.wildfly.build.util.PropertiesArtifactVersionOverrider}.
     */
    @Test
    public void testPropertyArtifactResolverVersionOverride() {
        final String artifactRef = "groupId:artifactId:1.0";
        final Artifact artifact = Artifact.fromString(artifactRef);
        final Map<String, Artifact> artifactRefs = new HashMap<>();
        artifactRefs.put(artifact.getDefaultArtifactRef(), artifact);
        final MapArtifactResolver artifactRefsResolver = new MapArtifactResolver(artifactRefs);
        final Properties properties = new Properties();
        final String overrideVersion = "2.0";
        properties.setProperty(PropertiesArtifactVersionOverrider.OVERRIDE_PROPERTY_NAME_PREFIX + artifact.getDefaultArtifactRef(), overrideVersion);
        final PropertiesArtifactVersionOverrider propertiesArtifactVersionOverrider = new PropertiesArtifactVersionOverrider(properties);
        propertiesArtifactVersionOverrider.override(artifactRefsResolver);
        Assert.assertEquals("failed to override using artifact ref with version", Artifact.resolve(artifactRef, artifactRefsResolver).getVersion(), overrideVersion);
        Assert.assertEquals("failed to override using standard artifact ref, i.e. containing no version", Artifact.resolve(artifact.getDefaultArtifactRef(), artifactRefsResolver).getVersion(), overrideVersion);
    }

    /**
     * Test artifact version override using the standard {@link org.wildfly.build.util.MapArtifactResolver}.
     */
    @Test
    public void testArtifactResolverVersionOverride() {
        final String groupId = "groupId";
        final String artifactId = "artifactId";
        final String version = "2.0";
        final Artifact artifact = new Artifact(groupId, artifactId, version);
        final Map<String, Artifact> artifactRefs = new HashMap<>();
        artifactRefs.put(artifact.getDefaultArtifactRef(), artifact);
        final MapArtifactResolver artifactRefsResolver = new MapArtifactResolver(artifactRefs);
        Assert.assertEquals("failed to override using artifact ref with version", Artifact.resolve(groupId+':'+artifactId+":1.0", artifactRefsResolver).getVersion(), version);
    }

    /**
     * Test artifact override using {@link org.wildfly.build.pack.model.DelegatingArtifactResolver}.
     */
    @Test
    public void testDelegatingArtifactVersionOverride() {
        final String artifactRef = "artifactRef";
        final Artifact artifact = new Artifact("groupId", "artifactId", "1.0");
        final Map<String, Artifact> artifactMap = new HashMap<>();
        artifactMap.put(artifactRef, artifact);
        final MapArtifactResolver artifactResolver = new MapArtifactResolver(artifactMap);

        final Artifact overrideArtifact = new Artifact(artifact.getGroupId(), artifact.getArtifactId(), "extension", "classifier", "2.0");
        final Map<String, Artifact> overrideArtifactMap = new HashMap<>();
        overrideArtifactMap.put(artifactRef, overrideArtifact);
        final MapArtifactResolver overrideArtifactResolver = new MapArtifactResolver(overrideArtifactMap);

        final ArtifactResolver delegatingArtifactResolver = new DelegatingArtifactResolver(overrideArtifactResolver, artifactResolver);
        final Artifact resolvedArtifact = delegatingArtifactResolver.getArtifact(artifactRef);
        Assert.assertNotNull(resolvedArtifact);
        Assert.assertEquals(resolvedArtifact.getGroupId(), overrideArtifact.getGroupId());
        Assert.assertEquals(resolvedArtifact.getArtifactId(), overrideArtifact.getArtifactId());
        Assert.assertEquals(resolvedArtifact.getExtension(), overrideArtifact.getExtension());
        Assert.assertEquals(resolvedArtifact.getClassifier(), overrideArtifact.getClassifier());
        Assert.assertEquals(resolvedArtifact.getVersion(), overrideArtifact.getVersion());
    }
}
