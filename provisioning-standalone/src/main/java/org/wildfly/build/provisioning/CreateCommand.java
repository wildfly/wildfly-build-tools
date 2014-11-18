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

import org.wildfly.build.pack.model.Artifact;
import org.wildfly.build.provisioning.model.ServerProvisioningDescription;
import org.wildfly.build.provisioning.model.ServerProvisioningDescriptionXmlWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class that handles the create command
 *
 * @author Stuart Douglas
 */
public class CreateCommand {


    public static void createServer(String[] args) {
        String file = "server-provisioning.xml";
        List<FeaturePack> packs = new ArrayList<>();
        boolean lastCommandPack = false;
        boolean noProvision = false;
        for(int i = 0; i < args.length; ++i) {
            String arg = args[i];
            switch (arg) {
                case CommandLineConstants.PACK: {
                    ++i;
                    if(i == args.length) {
                        printUsageAndExit(1);
                    }
                    packs.add(new FeaturePack(Artifact.parse(args[i])));
                    lastCommandPack = true;
                    break;
                }
                case CommandLineConstants.SUBSYSTEMS: {
                    if(!lastCommandPack) {
                        printUsageAndExit(1);
                    }
                    ++i;
                    if(i == args.length) {
                        printUsageAndExit(1);
                    }
                    String[] subsystems = args[i].split(",");
                    packs.get(packs.size() - 1).subsystems.addAll(Arrays.asList(subsystems));
                    lastCommandPack = false;
                    break;
                }
                case CommandLineConstants.FILE: {
                    ++i;
                    if(i == args.length) {
                        printUsageAndExit(1);
                    }
                    file = args[i];
                    lastCommandPack = false;
                    break;
                }
                case CommandLineConstants.NO_PROVISION: {
                    noProvision = true;
                    lastCommandPack = false;
                    break;
                }
            }
        }
        if(packs.isEmpty()) {
            printUsageAndExit(1);
        }
        doCreate(packs, file);
        if(!noProvision) {
            ProvisionCommand.provision(new File(file));
        }
    }

    private static void doCreate(List<FeaturePack> packs, String file) {
        ServerProvisioningDescription description = new ServerProvisioningDescription();
        for(FeaturePack pack : packs) {
            List<ServerProvisioningDescription.FeaturePack.Subsystem> subsystems = new ArrayList<>();
            for(String subsystem : pack.subsystems) {
                subsystems.add(new ServerProvisioningDescription.FeaturePack.Subsystem(subsystem, true));
            }
            description.getFeaturePacks().add(new ServerProvisioningDescription.FeaturePack(pack.name, null, null, null, subsystems));
        }

        try {
            FileOutputStream out = new FileOutputStream(file);

            try {
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                ServerProvisioningDescriptionXmlWriter.INSTANCE.writeContent(writer, description);
                writer.flush();
                writer.close();
            } finally {
                safeClose(out);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void safeClose(FileOutputStream out) {
        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                StandaloneProvisioningLogger.ROOT_LOGGER.debugf("Failed to close %s", out);
            }
        }
    }

    private static void printUsageAndExit(int exitCode) {
        System.out.print("you did it wrong, TODO: usage");
        System.exit(exitCode);
    }

    private static class FeaturePack {
        final Artifact name;
        final List<String> subsystems = new ArrayList<>();

        private FeaturePack(Artifact name) {
            this.name = name;
        }
    }
}
