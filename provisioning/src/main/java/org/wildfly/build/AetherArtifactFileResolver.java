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

package org.wildfly.build;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.List;

/**
 * @author Eduardo Martins
 */
public class AetherArtifactFileResolver implements ArtifactFileResolver {

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepos;

    public AetherArtifactFileResolver(RepositorySystem repoSystem, RepositorySystemSession repoSession, List<RemoteRepository> remoteRepos) {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
    }

    private File getArtifactFile(Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        final ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch ( ArtifactResolutionException e ) {
            throw new RuntimeException("failed to resolve artifact "+artifact, e);
        }
        return result.getArtifact().getFile();
    }

    @Override
    public File getArtifactFile(org.wildfly.build.pack.model.Artifact artifact) {
        final String groupId = artifact.getGroupId();
        final String artifactId = artifact.getArtifactId();
        final String extension = artifact.getPackaging() != null ? artifact.getPackaging() : "jar";
        final String classifier = artifact.getClassifier() != null ? artifact.getClassifier() : "";
        return getArtifactFile(new DefaultArtifact(groupId, artifactId, classifier, extension, artifact.getVersion()));
    }
}
