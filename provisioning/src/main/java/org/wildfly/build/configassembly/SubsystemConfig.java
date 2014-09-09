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

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemConfig {
    final String subsystem;
    final String supplement;

    SubsystemConfig(String subsystem, String supplement) {
        this.subsystem = subsystem;
        this.supplement = supplement;
    }

    String getSubsystem() {
        return subsystem;
    }

    String getSupplement() {
        return supplement;
    }

    @Override
    public String toString() {
        return "[subsystem=" + subsystem + ", supplement=" + supplement + "]";
    }
}
