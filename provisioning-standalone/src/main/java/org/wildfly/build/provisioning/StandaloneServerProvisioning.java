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
                ProvisionCommand.provision(Arrays.copyOfRange(args, 1, args.length));
                break;
            } case "create" : {
                //creates a server provisioning file without provisioning the server
                CreateCommand.createServer(Arrays.copyOfRange(args, 1, args.length));
                break;
            }
        }

    }


    private static void printUsageAndExit(int status) {
        System.out.println("TODO: usage instructions");
        System.exit(status);
    }

    private class Parameters {

    }
}
