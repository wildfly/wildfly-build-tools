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
package org.wildfly.build.configassembly;

import java.io.File;

import org.wildfly.build.util.BaseDirSubsystemInputStreamSources;
import org.wildfly.build.util.FileInputStreamSource;
import org.wildfly.build.util.PropertyResolver;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainMain {


    public static void main(String[] args) throws Exception {
        File baseDir = new File(args[0]);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base dir does not exist: " + baseDir);
        }
        File templateFile = new File(args[1]);
        if (!templateFile.exists()) {
            throw new IllegalArgumentException("Template file does not exist: " + templateFile);
        }
        File subsystemsFile = new File(args[2]);
        if (!subsystemsFile.exists()) {
            throw new IllegalArgumentException("Subsystems file does not exist " + subsystemsFile);
        }
        File outputFile = new File(args[3]);

        SubsystemInputStreamSources subsystemInputStreamSources = new BaseDirSubsystemInputStreamSources(baseDir.getAbsoluteFile());
        ConfigurationAssembler assembler = new ConfigurationAssembler(subsystemInputStreamSources, new FileInputStreamSource(templateFile), "domain", new FileInputStreamSource(subsystemsFile), outputFile, PropertyResolver.NO_OP);
        assembler.assemble();
    }
}
