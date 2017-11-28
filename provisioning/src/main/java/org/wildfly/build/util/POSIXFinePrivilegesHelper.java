/*
 * Copyright 2017 Red Hat, Inc.
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

package org.wildfly.build.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.wildfly.build.provisioning.ServerProvisioner.OS_WINDOWS;

public class POSIXFinePrivilegesHelper {

    private static final String JAR_FILE_MASK = "rw-r--r--";
    private static final String PROPERTIES_FILE_MASK = "rw-------";

    public static void fineTunePrivileges(Path path) throws IOException {
        if (!OS_WINDOWS)
            Files.setPosixFilePermissions(path, getPermissionsMasks(path));
    }

    private static Set<PosixFilePermission> getPermissionsMasks(Path dest) {
        final String suffix = dest.toAbsolutePath().toString();
        if (suffix.endsWith(".jar")) {
            return PosixFilePermissions.fromString(JAR_FILE_MASK);
        } else if (suffix.endsWith(".properties")) {
            return PosixFilePermissions.fromString(PROPERTIES_FILE_MASK);
        } else
            return getDefaultPermissionsFor(dest);
    }

    private static Set<PosixFilePermission> getDefaultPermissionsFor(Path file) {
        try {
            return Files.getPosixFilePermissions(file);
        } catch (IOException e) {
            // As we have ensure earlier we run on Posix, this should never happen, thus is an illegal state
            throw new IllegalStateException(e);
        }
    }
}
