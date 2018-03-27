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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.wildfly.build.AetherArtifactFileResolver;
import org.wildfly.build.Locations;
import org.wildfly.build.featurepack.FeaturePackBuilder;
import org.wildfly.build.featurepack.model.FeaturePackBuild;
import org.wildfly.build.featurepack.model.FeaturePackBuildModelParser;
import org.wildfly.build.util.FileUtils;
import org.wildfly.build.util.MapPropertyResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * The maven plugin that builds a Wildfly feature pack
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("InstanceVariableMayNotBeInitialized")
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class FeaturePackBuildMojo extends AbstractMojo {
    private static final boolean OS_WINDOWS = System.getProperty("os.name").contains("indows");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The configuration file used for feature pack.
     */
    @Parameter(alias = "config-file", defaultValue = "feature-pack-build.xml", property = "wildfly.feature.pack.configFile")
    private String configFile;

    /**
     * The directory the configuration file is located in.
     */
    @Parameter(alias = "config-dir", defaultValue = "${basedir}", property = "wildfly.feature.pack.configDir")
    private File configDir;

    /**
     * A path relative to {@link #configDir} that represents the directory under which of resources such as
     * {@code configuration/standalone/subsystems.xml}, {modules}, {subsystem-templates}, etc.
     */
    @Parameter(alias = "resources-dir", defaultValue = "src/main/resources", property = "wildfly.feature.pack.resourcesDir", required = true)
    private String resourcesDir;

    /**
     * The name of the server.
     */
    @Parameter(alias = "server-name", defaultValue = "${project.build.finalName}", property = "wildfly.feature.pack.serverName")
    private String serverName;

    /**
     * The directory for the built artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "wildfly.feature.pack.buildName")
    private String buildName;

    /**
     * Allows for additional resources or resources to be processed in a different manor. For example resources can be
     * {@linkplain Resource#isFiltering() filtered}.
     */
    @Parameter
    private List<Resource> resources;

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

    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession session;

    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    private MavenResourcesFiltering mavenResourcesFiltering;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        /* normalize resourcesDir */
        if (!resourcesDir.isEmpty()) {
            switch (resourcesDir.charAt(0)) {
            case '/':
            case '\\':
                break;
            default:
                resourcesDir = "/" + resourcesDir;
                break;
            }
        }

        copyContents();
        try (FileInputStream configStream = new FileInputStream(new File(configDir, configFile))) {
            Properties properties = new Properties();
            properties.putAll(project.getProperties());
            properties.putAll(System.getProperties());
            properties.put("project.version", project.getVersion()); //TODO: figure out the correct way to do this
            final FeaturePackBuild build = new FeaturePackBuildModelParser(new MapPropertyResolver(properties)).parse(configStream);
            File target = new File(buildName, serverName);
            FeaturePackBuilder.build(build, target, new MavenProjectArtifactResolver(project), new AetherArtifactFileResolver(repoSystem, repoSession, remoteRepos));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Process additional resources
        if (resources != null) {
            try {
                processResources(getOverwriteResources(),true);
                processResources(getNonOverwriteResources(), false);
            } catch (MavenFilteringException e) {
                throw new MojoExecutionException("Failed to process resources.", e);
            }
        }
    }

    /**
     * @throws IOException
     */
    private void copyContents() {
        try {
            File baseDir = new File(buildName, serverName);
            FileUtils.deleteRecursive(baseDir);

            final Path path = Paths.get(baseDir.getAbsolutePath());

            final Path baseFile = Paths.get(configDir.getAbsolutePath() + resourcesDir);
            doCopy(path.resolve(Locations.CONTENT), baseFile.resolve(Locations.CONTENT));
            doCopy(path.resolve(Locations.MODULES), baseFile.resolve(Locations.MODULES));
            doCopy(path.resolve(Locations.CONFIGURATION), baseFile.resolve(Locations.CONFIGURATION));
            doCopy(path.resolve(Locations.SUBSYSTEM_TEMPLATES), baseFile.resolve(Locations.SUBSYSTEM_TEMPLATES));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doCopy(final Path target, final Path source) throws IOException {
        Files.walkFileTree(source, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String relative = source.relativize(dir).toString();
                boolean include = true;
                if (include) {
                    Path rel = target.resolve(relative);
                    if (!Files.isDirectory(rel)) {
                        if (!rel.toFile().mkdirs()) {
                            throw new IOException("Could not create directory " + rel.toString());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = source.relativize(file).toString();

                Path targetFile = target.resolve(relative);
                FileUtils.copyFile(file.toFile(), targetFile.toFile());
                if (!OS_WINDOWS) {
                    Files.setPosixFilePermissions(targetFile, Files.getPosixFilePermissions(file));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processResources(final List<org.apache.maven.model.Resource> resources, final boolean overwrite) throws MavenFilteringException {
        final MavenResourcesExecution resourcesExecution = new MavenResourcesExecution(resources, new File(buildName, serverName),
                project, "UTF-8", Collections.emptyList(), Collections.emptyList(), session);
        resourcesExecution.setIncludeEmptyDirs(false);
        resourcesExecution.setEscapeWindowsPaths(true);
        resourcesExecution.setOverwrite(overwrite);
        resourcesExecution.setAddDefaultExcludes(true);
        mavenResourcesFiltering.filterResources(resourcesExecution);
    }

    private List<org.apache.maven.model.Resource> getOverwriteResources() {
        final List<org.apache.maven.model.Resource> result = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource.isOverwrite()) {
                result.add(resource);
            }
        }
        return result;
    }

    private List<org.apache.maven.model.Resource> getNonOverwriteResources() {
        final List<org.apache.maven.model.Resource> result = new ArrayList<>();
        for (Resource resource : resources) {
            if (!resource.isOverwrite()) {
                result.add(resource);
            }
        }
        return result;
    }

}
