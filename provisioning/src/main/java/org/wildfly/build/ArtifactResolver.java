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

import org.wildfly.build.pack.model.Artifact;

/**
 * Resolver that returns a new artifact with the same coordinates but with the version defined.
 *
 * @author Eduardo Martins
 */
public interface ArtifactResolver {

    /**
     *
     * @param unversioned
     * @return A new artifact with the version defined, or null if the version cannot be determined
     */
    Artifact getArtifact(Artifact unversioned);

}
