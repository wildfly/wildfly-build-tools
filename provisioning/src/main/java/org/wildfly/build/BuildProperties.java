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
package org.wildfly.build;

/**
 * Properties which customize all build behaviours.
 * @author Eduardo Martins
 */
public class BuildProperties {

    /**
     * indicates if a maven project's artifacts should be included in artifact resolving
     */
    public static final String USE_MAVEN_PROJECT_ARTIFACT_RESOLVER = "use-maven-project-artifact-resolver";

    /**
     * indicates if system properties should be allowed to override artifact versions
     */
    public static final String SYSTEM_PROPERTIES_VERSION_OVERRIDES = "system-property-version-overrides";
}
