package org.wildfly.build.plugin;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.wildfly.build.pack.model.Artifact;
import static org.junit.Assert.assertEquals;

public class MavenProjectArtifactResolverTestCase {
    private final String pomWithDependencies = "pom-with-no-dependencies.xml";
    private final String pomWithDependencyManagement = "pom-with-dependencymanagement.xml";

    @Test
    public void pomWithDependencyManagementTest() throws Exception {
        MavenProjectArtifactResolver mavenProjectArtifactResolver = loadConfigFile(pomWithDependencyManagement);
        assertEquals(mavenProjectArtifactResolver.getArtifact(getArtifact()).getVersion(), "1.7.22.redhat-2");
    }

    @Test
    public void pomWithnoDependenciesTest() throws Exception {
        MavenProjectArtifactResolver mavenProjectArtifactResolver = loadConfigFile(pomWithDependencies);
        assertEquals(mavenProjectArtifactResolver.getArtifact(getArtifact()), null);
    }

    private MavenProjectArtifactResolver loadConfigFile(String filename) throws Exception {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;
        ClassLoader classLoader = getClass().getClassLoader();

        model = pomReader.read(new FileReader(new File(classLoader.getResource(filename).getFile())));
        MavenProject project = new MavenProject(model);
        MavenProjectArtifactResolver mavenProjectArtifactResolver = new MavenProjectArtifactResolver(project);
        return mavenProjectArtifactResolver;
    }

    private Artifact getArtifact() {
        return new Artifact("org.slf4j", "slf4j-api", null, null, null);
    }

}
