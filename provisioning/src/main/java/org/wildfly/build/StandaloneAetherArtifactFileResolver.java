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

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link org.wildfly.build.AetherArtifactFileResolver} which does not requires Maven.
 * @author Eduardo Martins
 */
public class StandaloneAetherArtifactFileResolver extends AetherArtifactFileResolver {

    /**
     * a default instance
     */
    public static final StandaloneAetherArtifactFileResolver DEFAULT_INSTANCE = new StandaloneAetherArtifactFileResolver();

    /**
     *
     * @return
     */
    private static File getDefaultLocalRepositoryDir() {
        // create the standalone aether artifact file resolver, reuse maven local repo if found at standard location, or the tmp dir
        final File mavenLocalRepositoryBaseDir = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
        return mavenLocalRepositoryBaseDir.exists() ? mavenLocalRepositoryBaseDir : (new File(new File(System.getProperty("java.io.tmpdir")), "repository"));
    }

    /**
     * Constructs a new instance using the default local repository base dir, and the default remote repositories.
     */
    public StandaloneAetherArtifactFileResolver() {
        this(getDefaultLocalRepositoryDir(), getStandardRemoteRepositories());
    }

    /**
     * Constructs a new instance using the default local repository base dir, and provided remote repositories.
     * @param remoteRepositories
     */
    public StandaloneAetherArtifactFileResolver(List<RemoteRepository> remoteRepositories) {
        this(getDefaultLocalRepositoryDir(), newRepositorySystem(), remoteRepositories);
    }

    /**
     * Constructs a new instance using the provided local repository base dir, and the default remote repositories.
     * @param localRepositoryBaseDir
     */
    public StandaloneAetherArtifactFileResolver(File localRepositoryBaseDir) {
        this(localRepositoryBaseDir, getStandardRemoteRepositories());
    }

    /**
     * Constructs a new instance using the provided local repository base dir, and provided remote repositories.
     * @param localRepositoryBaseDir
     * @param remoteRepositories
     */
    public StandaloneAetherArtifactFileResolver(File localRepositoryBaseDir, List<RemoteRepository> remoteRepositories) {
        this(localRepositoryBaseDir, newRepositorySystem(), remoteRepositories);
    }

    private StandaloneAetherArtifactFileResolver(File localRepositoryBaseDir, RepositorySystem repositorySystem, List<RemoteRepository> remoteRepositories) {
        super(repositorySystem, newRepositorySystemSession(repositorySystem, localRepositoryBaseDir), remoteRepositories);
    }

    /**
     * Retrieves a new instance of standalone {@link org.eclipse.aether.RepositorySystem}.
     * @return
     */
    private static RepositorySystem newRepositorySystem() {
        return MavenRepositorySystemUtils.newServiceLocator()
                .addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
                .addService(TransporterFactory.class, FileTransporterFactory.class)
                .addService(TransporterFactory.class, HttpTransporterFactory.class)
                .getService(RepositorySystem.class);
    }

    /**
     * Retrieves a new instance of a standalone {@link org.eclipse.aether.RepositorySystemSession}, which {@link org.eclipse.aether.repository.LocalRepositoryManager} points to 'target/local-repository' dir.
     * @param system
     * @return
     */
    private static RepositorySystemSession newRepositorySystemSession(RepositorySystem system, File localRepositoryBaseDir) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(session, new LocalRepository(localRepositoryBaseDir));
        session.setLocalRepositoryManager(localRepositoryManager);
        return session;
    }

    /**
     * Retrieves the standard remote repositories, i.e., maven central and jboss.org
     * @return
     */
    public static List<RemoteRepository> getStandardRemoteRepositories() {
        final List<RemoteRepository> remoteRepositories = new ArrayList<>();
        remoteRepositories.add(new RemoteRepository.Builder("central","default","http://central.maven.org/maven2/").build());
        remoteRepositories.add(new RemoteRepository.Builder("jboss-community-repository", "default", "http://repository.jboss.org/nexus/content/groups/public/").build());
        return remoteRepositories;
    }
}
