package org.wildfly.build.pack.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents an item in the pack. Basically just a thin abstraction over a zip file.
 *
 * @author Stuart Douglas
 */
public class ContentItem {

    private final ZipEntry zipEntry;
    private final ZipFile zipFile;

    public ContentItem(ZipEntry zipEntry, ZipFile zipFile) {
        this.zipEntry = zipEntry;
        this.zipFile = zipFile;
    }


    public InputStream getInputStream() throws IOException {
        return zipFile.getInputStream(zipEntry);
    }

    public boolean isDirectory() {
        return zipEntry.isDirectory();
    }
}
