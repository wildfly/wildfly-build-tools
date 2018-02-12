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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.wildfly.build.AetherArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.DelegatingArtifactResolver;
import org.wildfly.build.pack.model.FeaturePackArtifactResolver;
import org.wildfly.build.provisioning.ServerProvisioner;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.PropertiesBasedArtifactResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

/**
 * The maven plugin that provisions a Wildfly server from a set of feature packs.
 *
 * @author Eduardo Martins
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class ServerProvisioningMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The configuration file used for provisioning.
     */
    @Parameter(alias = "config-file", defaultValue = "server-provisioning.xml", property = "wildfly.provision.configFile")
    private String configFile;

    /**
     * The directory the configuration file is located in.
     */
    @Parameter(alias = "config-dir", defaultValue = "${basedir}", property = "wildfly.provision.configDir")
    private File configDir;

    /**
     * The name of the server. This is the name of the final build artifact.
     */
    @Parameter(alias = "server-name", defaultValue = "${project.build.finalName}", property = "wildfly.provision.serverName")
    private String serverName;

    /**
     * The directory for the built artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "wildfly.provision.buildName")
    private String buildName;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;


    @Parameter(alias = "system-property-version-overrides", defaultValue = "false", readonly = true)
    private Boolean systemPropertyVersionOverrides = false;

    @Parameter(alias = "allow-maven-version-overrides", defaultValue = "false", readonly = true)
    private Boolean allowMavenVersionOverrides = false;

    @Parameter(alias = "overlay", defaultValue = "false")
    private Boolean overlay = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try (FileInputStream configStream = new FileInputStream(new File(configDir, configFile))) {


            Properties properties = new Properties();
            properties.putAll(project.getModel().getProperties());
            properties.putAll(project.getProperties());
            properties.putAll(System.getProperties());
            properties.put("project.version", project.getVersion()); //TODO: figure out the correct way to do this
            properties.put("project.groupId", project.getGroupId());
            properties.put("project.artifactId", project.getArtifactId());
            properties.put("project.packaging", project.getPackaging());

            final ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(properties)).parse(configStream);
            AetherArtifactFileResolver aetherArtifactFileResolver = new AetherArtifactFileResolver(repoSystem, repoSession, remoteRepos);
            ArtifactResolver overrideArtifactResolver = new FeaturePackArtifactResolver(serverProvisioningDescription.getVersionOverrides());
            if(allowMavenVersionOverrides) {
                overrideArtifactResolver = new DelegatingArtifactResolver(new MavenProjectArtifactResolver(this.project), overrideArtifactResolver);
            }
            if(systemPropertyVersionOverrides) {
                overrideArtifactResolver = new DelegatingArtifactResolver(new PropertiesBasedArtifactResolver(properties), overrideArtifactResolver);
            }


            ServerProvisioner.build(serverProvisioningDescription, new File(buildName, serverName), overlay, aetherArtifactFileResolver, overrideArtifactResolver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
