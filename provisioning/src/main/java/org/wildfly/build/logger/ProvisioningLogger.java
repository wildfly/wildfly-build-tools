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

package org.wildfly.build.logger;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Stuart Douglas
 */
@MessageLogger(projectCode = "WFPROV") //todo: a better project code
public interface ProvisioningLogger extends BasicLogger {

    ProvisioningLogger ROOT_LOGGER = Logger.getMessageLogger(ProvisioningLogger.class, ProvisioningLogger.class.getPackage().getName());

    @Message("Cannot parse artifact spec %s")
    RuntimeException cannotParseArtifact(String artifact);
}
