package org.wildfly.build.provisioning;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class JandexIndexer {

    private static final Logger log = Logger.getLogger(JandexIndexer.class);

    public static void createIndex(File jarFile, OutputStream target) throws IOException {
        ZipOutputStream zo;

        Indexer indexer = new Indexer();

        JarFile jar = new JarFile(jarFile);

        zo = new ZipOutputStream(target);
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    try {
                        final InputStream stream = jar.getInputStream(entry);
                        try {
                            indexer.index(stream);
                        } finally {
                            safeClose(stream);
                        }
                    } catch (Exception e) {
                        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                        log.error("Could not index " + entry.getName() + ": " + message, e);
                    }
                }
            }

            zo.putNextEntry(new ZipEntry("META-INF/jandex.idx"));

            IndexWriter writer = new IndexWriter(zo);
            Index index = indexer.complete();
            writer.write(index);
        } finally {
            safeClose(zo);
            safeClose(jar);
            safeClose(target);
        }
    }


    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.error("Failed to close", e);
            }
        }
    }
}
