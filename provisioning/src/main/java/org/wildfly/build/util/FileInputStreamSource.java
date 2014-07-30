package org.wildfly.build.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Eduardo Martins
 */
public class FileInputStreamSource implements InputStreamSource {

    private final File file;

    public FileInputStreamSource(File file) {
        this.file = file.getAbsoluteFile();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }
}
