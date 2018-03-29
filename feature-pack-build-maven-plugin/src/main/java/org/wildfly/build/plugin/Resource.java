/*
 * Copyright 2018 Red Hat, Inc.
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

package org.wildfly.build.plugin;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Represents a resource which should be processed by the feature pack.
 * <p>
 * This can be used to add additional resource directories to a feature pack or use filtering for existing resources.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Resource extends org.apache.maven.model.Resource {

    /**
     * Indicates for a {@linkplain #isFiltering() non-filtered} resource if the target should be overwritten or not.
     */
    @SuppressWarnings("InstanceVariableMayNotBeInitialized")
    @Parameter(defaultValue = "false")
    private boolean overwrite;

    boolean isOverwrite() {
        return overwrite;
    }
}
