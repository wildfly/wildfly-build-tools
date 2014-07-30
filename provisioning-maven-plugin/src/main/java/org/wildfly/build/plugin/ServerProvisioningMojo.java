/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.build.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.wildfly.build.AetherArtifactFileResolver;
import org.wildfly.build.provisioning.ServerProvisioner;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.util.MapPropertyResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * The maven plugin that provisions a Wildfly server from a set of feature packs.
 *
 * @goal build
 * @phase compile
 * @requiresDependencyResolution runtime
 *
 * @author Eduardo Martins
 */
public class ServerProvisioningMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter alias="config-file"
     * @required
     */
    private String configFile;

    /**
     * @parameter alias="config-dir" default-value="${basedir}"
     */
    private File configDir;

    /**
     * @parameter alias="server-name" default-value="${project.build.finalName}"
     */
    private String serverName;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    private String buildName;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try (FileInputStream configStream = new FileInputStream(new File(configDir, configFile))) {
            final ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(project.getProperties())).parse(configStream);
            ServerProvisioner.build(serverProvisioningDescription, new File(buildName, serverName), new MavenProjectArtifactResolver(project), new AetherArtifactFileResolver(repoSystem, repoSession, remoteRepos));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
