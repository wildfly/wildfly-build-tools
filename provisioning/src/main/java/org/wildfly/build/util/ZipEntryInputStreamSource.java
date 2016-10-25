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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class ZipEntryInputStreamSource implements InputStreamSource {

    private final File file;
    private final ZipEntry zipEntry;

    public ZipEntryInputStreamSource(File file, ZipEntry zipEntry) {
        this.file = file;
        this.zipEntry = zipEntry;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        final ZipFile zipFile = new ZipFile(file);
        try {
            return new ZipEntryInputStream(zipFile, zipFile.getInputStream(zipEntry));
        } catch (Throwable t) {
            try {
                zipFile.close();
            } catch (Throwable ignore) {

            }
            throw new IOException("failed to retrieve input stream", t);
        }
    }

    private static class ZipEntryInputStream extends InputStream {

        private final ZipFile zipFile;
        private final InputStream zipEntryInputStream;

        ZipEntryInputStream(ZipFile zipFile, InputStream zipEntryInputStream) {
            this.zipFile = zipFile;
            this.zipEntryInputStream = zipEntryInputStream;
        }

        @Override
        public int read() throws IOException {
            return zipEntryInputStream.read();
        }

        @Override
        public int available() throws IOException {
            return zipEntryInputStream.available();
        }

        @Override
        public void close() throws IOException {
            try {
                zipEntryInputStream.close();
            } finally {
                try {
                    this.zipFile.close();
                } catch (Throwable t) {
                    // ignore
                    t.printStackTrace();
                }
            }
        }

        @Override
        public synchronized void mark(int readlimit) {
            zipEntryInputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            zipEntryInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return zipEntryInputStream.markSupported();
        }
    }

}
