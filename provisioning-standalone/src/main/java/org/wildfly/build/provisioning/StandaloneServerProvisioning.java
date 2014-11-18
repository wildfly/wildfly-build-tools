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

package org.wildfly.build.provisioning;

import org.wildfly.build.AetherArtifactFileResolver;
import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.DelegatingArtifactResolver;
import org.wildfly.build.pack.model.FeaturePackArtifactResolver;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.PropertiesBasedArtifactResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * The standalone app to provision a WildFly server.
 *
 * @author Eduardo Martins
 */
public class StandaloneServerProvisioning {
    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            printUsageAndExit(1);
        }
        String operation = args[0];
        switch (operation) {
            case "provision": {
                provision(Arrays.copyOfRange(args, 1, args.length));
                break;
            } case "create" : {
                //creates a server provisioning file without provisioning the server
                CreateCommand.createServer(Arrays.copyOfRange(args, 1, args.length));
                break;
            }
        }

    }

    private static void provision(String[] args) {

        final File configFile = new File(args.length == 1 ? args[0] : "server-provisioning.xml");
        // environment is the sys properties
        final Properties environment = System.getProperties();
        // setup build dir
        final File buildDir = new File("target");
        buildDir.mkdirs();
        // create the standalone aether artifact file resolver, reuse maven local repo if found at standard location
        final File mavenLocalRepositoryBaseDir = new File(new File(System.getProperty("user.home"), ".m2"), "repository");
        final AetherArtifactFileResolver aetherArtifactFileResolver = new StandaloneAetherArtifactFileResolver(mavenLocalRepositoryBaseDir.exists() ? mavenLocalRepositoryBaseDir : (new File(buildDir, "repository")));
        try (FileInputStream configStream = new FileInputStream(configFile)) {
            // parse description
            final ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(environment)).parse(configStream);
            // create version override artifact resolver
            ArtifactResolver overrideArtifactResolver = new FeaturePackArtifactResolver(serverProvisioningDescription.getVersionOverrides());
            if(Boolean.valueOf(environment.getProperty("system-property-version-overrides", "false"))) {
                overrideArtifactResolver = new DelegatingArtifactResolver(new PropertiesBasedArtifactResolver(environment), overrideArtifactResolver);
            }
            // provision the server
            final File outputDir = new File(buildDir, "wildfly");
            ServerProvisioner.build(serverProvisioningDescription, outputDir, aetherArtifactFileResolver, overrideArtifactResolver);
            System.out.print("Server provisioning at "+outputDir+" complete.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void printUsageAndExit(int status) {
        System.out.println("TODO: usage instructions");
        System.exit(status);
    }

    private class Parameters {

    }
}
