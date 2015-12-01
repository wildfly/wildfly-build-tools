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

package org.wildfly.build.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class FileUtils {

    public static void extractFile(JarFile jarFile, String jarEntryName, File targetFile) throws IOException {
        byte[] data = new byte[1024];
        ZipEntry entry = jarFile.getEntry(jarEntryName);
        if (entry.isDirectory()) { // if its a directory, create it
            targetFile.mkdirs();
            return;
        }
        File parent = targetFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new java.io.FileOutputStream(targetFile); InputStream is = jarFile.getInputStream(entry)) {
            int read;
            while ((read = is.read(data)) > 0) {  // write contents of 'is' to 'fos'
                fos.write(data, 0, read);
            }
        }
    }

    public static void extractSchemas(File file, File outputDirectory) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            // schemas are in dir 'schema'
            if (zip.getEntry("schema") != null) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (outputDirectory != null && entryName.startsWith("schema/")) {
                            try (InputStream in = zip.getInputStream(entry)) {
                                FileUtils.copyFile(in, new File(outputDirectory, entryName.substring("schema/".length())));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(final File src, final File dest) throws IOException {
        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String readFile(final File file) {
        try {
            return readFile(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(InputStream file) {
        try (final BufferedInputStream stream = new BufferedInputStream(file)) {
            byte[] buff = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int read = -1;
            while ((read = stream.read(buff)) != -1) {
                builder.append(new String(buff, 0, read));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteRecursive(final File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursive(f);
            }
        }
        file.delete();
    }

}
