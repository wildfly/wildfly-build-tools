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

package org.wildfly.build.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.jboss.logging.Logger;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.Artifact;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public class MavenProjectArtifactResolver implements ArtifactResolver {
    private static final Logger logger = Logger.getLogger(MavenProjectArtifactResolver.class);

    private final Map<String, Artifact> artifactMap;

    public MavenProjectArtifactResolver(MavenProject mavenProject) {
        this.artifactMap = new HashMap<>();
        //Fix WFBUILD-39 Wildfly-build-tools ignores dependency management
        if (mavenProject.getDependencyManagement() != null) {
            for (Dependency dependency : mavenProject.getDependencyManagement().getDependencies()) {
                final Artifact artifact = new Artifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), dependency.getVersion());
                artifactMap.put(new Artifact(artifact, null).toString(), artifact);
                logger.debug("Adding dependencymanagement: " + artifact.toJBossModulesString());
            }
        }
        for (org.apache.maven.artifact.Artifact mavenProjectArtifact : mavenProject.getArtifacts()) {
            final Artifact artifact = new Artifact(mavenProjectArtifact.getGroupId(), mavenProjectArtifact.getArtifactId(), mavenProjectArtifact.getType(), mavenProjectArtifact.getClassifier(), mavenProjectArtifact.getVersion());
            artifactMap.put(new Artifact(artifact, null).toString(), artifact);
            logger.debug("Adding dependency: " + artifact.toJBossModulesString());
        }
    }
    @Override
    public Artifact getArtifact(Artifact GACE) {
        return artifactMap.get(GACE.toString());
    }

}
