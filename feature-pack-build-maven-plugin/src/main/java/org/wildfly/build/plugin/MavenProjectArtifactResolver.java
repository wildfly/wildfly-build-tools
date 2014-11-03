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

import org.apache.maven.project.MavenProject;
import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.util.MapArtifactResolver;

/**
 * An artifact resolver for a maven project's artifacts.
 * @author Eduardo Martins
 */
public class MavenProjectArtifactResolver extends MapArtifactResolver {

    public MavenProjectArtifactResolver(MavenProject mavenProject) {
        for (org.apache.maven.artifact.Artifact mavenProjectArtifact : mavenProject.getArtifacts()) {
            final Artifact artifact = new Artifact(mavenProjectArtifact.getGroupId(), mavenProjectArtifact.getArtifactId(), mavenProjectArtifact.getType(), mavenProjectArtifact.getClassifier(), mavenProjectArtifact.getVersion());
            StringBuilder sb = new StringBuilder();
            sb.append(artifact.getGroupId());
            sb.append(':');
            sb.append(artifact.getArtifactId());
            if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
                artifactMap.put(sb.append("::").append(artifact.getClassifier()).toString(), artifact);
            } else {
                artifactMap.put(sb.toString(), artifact);
            }
        }
    }
}
