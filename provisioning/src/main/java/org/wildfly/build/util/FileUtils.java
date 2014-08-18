package org.wildfly.build.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author Eduardo Martins
 */
public class FileUtils {

    public static void extractFile(JarFile jarFile, String jarEntryName, File targetFile) throws IOException {
        byte[] data = new byte[1024];
        ZipEntry entry = jarFile.getEntry(jarEntryName);
        if (entry.isDirectory()) { // if its a directory, create it
            targetFile.mkdir();
            return;
        }
        try (FileOutputStream fos = new java.io.FileOutputStream(targetFile);  InputStream is = jarFile.getInputStream(entry)) {
            int read;
            while ((read = is.read(data)) > 0) {  // write contents of 'is' to 'fos'
                fos.write(data, 0, read);
            }
        }
    }

    public static void copyFile(final InputStream in, final File dest) throws IOException {
        dest.getParentFile().mkdirs();
        byte[] data = new byte[10000];
        try (final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
            int read;
            while ((read = in.read(data)) > 0) {
                out.write(data, 0, read);
            }
        }
    }

    public static void copyFile(final File src, final File dest) throws IOException {
        try (final InputStream in = new BufferedInputStream(new FileInputStream(src))){
            copyFile(in, dest);
        }
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
