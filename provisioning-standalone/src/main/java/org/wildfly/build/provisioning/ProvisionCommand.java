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

import org.wildfly.build.ArtifactResolver;
import org.wildfly.build.pack.model.DelegatingArtifactResolver;
import org.wildfly.build.pack.model.FeaturePackArtifactResolver;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionModelParser;
import org.wildfly.build.util.MapPropertyResolver;
import org.wildfly.build.util.PropertiesBasedArtifactResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author Stuart Douglas
 */
public class ProvisionCommand {

    public static final String DEFAULT_CONFIG_FILE = "server-provisioning.xml";
    public static final String DEFAULT_BUILD_DIR = "target";
    public static final String DEFAULT_SERVER_NAME = "wildfly";

    public static void provision(String[] args) {
        final File configFile = new File(args.length == 1 ? args[0] : DEFAULT_CONFIG_FILE);
        provision(configFile);
    }

    public static void provision(File configFile) {
        provision(configFile, null, null);
    }

    public static void provision(File configFile, File buildDir, String serverName) {
        try (FileInputStream configStream = new FileInputStream(configFile)) {
            // parse description
            final ServerProvisioningDescription serverProvisioningDescription = new ServerProvisioningDescriptionModelParser(new MapPropertyResolver(System.getProperties())).parse(configStream);
            provision(serverProvisioningDescription, buildDir, serverName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void provision(ServerProvisioningDescription serverProvisioningDescription) {
        provision(serverProvisioningDescription, null, null);
    }

    public static void provision(ServerProvisioningDescription serverProvisioningDescription, File buildDir, String serverName) {
        //TODO: better target selection, also make sure provisioning file is copied
        if (buildDir == null) {
            buildDir = new File(DEFAULT_BUILD_DIR);
        }
        if (serverName == null) {
            serverName = DEFAULT_SERVER_NAME;
        }
        // environment is the sys properties
        final Properties environment = System.getProperties();
        // create version override artifact resolver
        ArtifactResolver overrideArtifactResolver = new FeaturePackArtifactResolver(serverProvisioningDescription.getVersionOverrides());
        if(Boolean.valueOf(environment.getProperty("system-property-version-overrides", "false"))) {
            overrideArtifactResolver = new DelegatingArtifactResolver(new PropertiesBasedArtifactResolver(environment), overrideArtifactResolver);
        }
        // provision the server
        final File outputDir = new File(buildDir, serverName);
        ServerProvisioner.build(serverProvisioningDescription, outputDir, StandaloneAetherArtifactFileResolver.DEFAULT_INSTANCE, overrideArtifactResolver);
        System.out.println("Server provisioning at " + outputDir + " complete.");
    }
}
