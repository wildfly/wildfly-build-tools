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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.wildfly.build.Locations;
import org.wildfly.build.featurepack.ArtifactResolver;
import org.wildfly.build.featurepack.FeaturePackBuilder;
import org.wildfly.build.featurepack.model.FeaturePackBuild;
import org.wildfly.build.featurepack.model.FeaturePackBuildModelParser;
import org.wildfly.build.util.MapPropertyResolver;

/**
 * The maven plugin that builds a Wildfly feature pack
 *
 * @author Stuart Douglas
 */
@Mojo(name = "build", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class BuildMojo extends AbstractMojo {
    private static final boolean OS_WINDOWS = System.getProperty("os.name").contains("indows");

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(alias = "config-file", required = true)
    private String configFile;

    @Parameter(defaultValue = "${basedir}", alias = "config-dir")
    private File configDir;

    @Parameter(defaultValue = "${project.build.finalName}", alias = "server-name")
    private String serverName;

    @Parameter(defaultValue = "${project.build.directory}")
    private String buildName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Map<String, Artifact> artifactMap = buildArtifactMap();
        copyContents();
        FileInputStream configStream = null;
        try {
            configStream = new FileInputStream(new File(configDir, configFile));
            final FeaturePackBuild build = new FeaturePackBuildModelParser(new MapPropertyResolver(project.getProperties())).parse(configStream);
            File target = new File(buildName, serverName);
            FeaturePackBuilder.build(build, target, new MapArtifactResolver(artifactMap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(configStream);
        }
    }

    /**
     * Builds a map of all the artifacts, keyed by GAV both with and without the version component
     */
    private Map<String, Artifact> buildArtifactMap() {
        Map<String, Artifact> artifactMap = new HashMap<>();
        for (Artifact artifact : project.getArtifacts()) {
            StringBuilder sb = new StringBuilder();
            sb.append(artifact.getGroupId());
            sb.append(':');
            sb.append(artifact.getArtifactId());
            if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
                artifactMap.put(sb.toString() + "::" + artifact.getClassifier(), artifact);
                artifactMap.put(sb.toString() + ":" + artifact.getVersion() + ":" + artifact.getClassifier(), artifact);
            } else {
                artifactMap.put(sb.toString(), artifact);
                sb.append(':');
                sb.append(artifact.getVersion());
                artifactMap.put(sb.toString(), artifact);
            }
        }
        return artifactMap;

    }

    private void safeClose(final Closeable... closeable) {
        for (Closeable c : closeable) {
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e) {
                    getLog().error("Failed to close resource", e);
                }
            }
        }
    }


    /**
     * @throws IOException
     */
    public void copyContents() {
        try {
            File baseDir = new File(buildName, serverName);
            deleteRecursive(baseDir);

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
                System.out.println(dir.toAbsolutePath());
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
                copyFile(file.toFile(), targetFile.toFile());
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


    public void copyFile(final File src, final File dest) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(src));
        try {
            copyFile(in, dest);
        } finally {
            safeClose(in);
        }
    }

    public void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        byte[] data = new byte[10000];
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
        try {
            int read;
            while ((read = in.read(data)) > 0) {
                out.write(data, 0, read);
            }
        } finally {
            safeClose(out);
        }
    }


    public void deleteRecursive(final File file) {

        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursive(f);
            }
        }
        file.delete();
    }


    private static class MapArtifactResolver implements ArtifactResolver {
        private final Map<String, Artifact> artifactMap;

        public MapArtifactResolver(Map<String, Artifact> artifactMap) {
            this.artifactMap = artifactMap;
        }

        @Override
        public String getVersion(String artifact) {
            Artifact af = artifactMap.get(artifact);
            if (af == null) {
                return null;
            }
            return af.getVersion();
        }

        @Override
        public File getArtifact(String artifact) {
            Artifact af = artifactMap.get(artifact);
            if (af == null) {
                return null;
            }
            return af.getFile();
        }

        @Override
        public String toString() {
            return "MapArtifactResolver{" +
                    "artifactMap=" + artifactMap +
                    '}';
        }
    }
}
