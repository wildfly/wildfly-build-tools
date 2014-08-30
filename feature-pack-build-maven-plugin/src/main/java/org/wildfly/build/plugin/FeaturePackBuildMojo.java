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
import java.util.List;

/**
 * The maven plugin that builds a Wildfly feature pack
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE)
public class FeaturePackBuildMojo extends AbstractMojo {
    private static final boolean OS_WINDOWS = System.getProperty("os.name").contains("indows");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The configuration file used for feature pack.
     */
    @Parameter(alias = "config-file", property = "wildfly.feature.pack.configFile")
    private String configFile;

    /**
     * The directory the configuration file is located in.
     */
    @Parameter(alias = "config-dir", defaultValue = "${basedir}", property = "wildfly.feature.pack.configDir")
    private File configDir;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        copyContents();
        try (FileInputStream configStream = new FileInputStream(new File(configDir, configFile))) {
            final FeaturePackBuild build = new FeaturePackBuildModelParser(new MapPropertyResolver(project.getProperties())).parse(configStream);
            File target = new File(buildName, serverName);
            FeaturePackBuilder.build(build, target, new MavenProjectArtifactResolver(project), new AetherArtifactFileResolver(repoSystem, repoSession, remoteRepos));
        } catch (Exception e) {
            throw new RuntimeException(e);
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

            final Path baseFile = Paths.get(configDir.getAbsolutePath() + "/src/main/resources");
            doCopy(path.resolve(Locations.CONTENT), baseFile.resolve(Locations.CONTENT));
            doCopy(path.resolve(Locations.MODULES), baseFile.resolve(Locations.MODULES));
            doCopy(path.resolve(Locations.CONFIGURATION), baseFile.resolve(Locations.CONFIGURATION));

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

}
