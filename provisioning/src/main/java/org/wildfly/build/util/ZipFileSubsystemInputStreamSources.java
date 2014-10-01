package org.wildfly.build.util;

import org.wildfly.build.configassembly.SubsystemInputStreamSources;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class ZipFileSubsystemInputStreamSources implements SubsystemInputStreamSources {

    private final Map<String, ZipEntryInputStreamSource> inputStreamSourceMap = new HashMap<>();

    /**
     * Creates a zip entry inputstream source and maps it to the specified filename.
     * @param subsystemFileName
     * @param zipFile
     * @param zipEntry
     */
    public void addSubsystemFileSource(String subsystemFileName, File zipFile, ZipEntry zipEntry) {
       inputStreamSourceMap.put(subsystemFileName, new ZipEntryInputStreamSource(zipFile, zipEntry));
    }

    /**
     * Adds all subsystem input stream sources from the specified factory. Note that only absent sources will be added.
     * @param other
     */
    public void addAllSubsystemFileSources(ZipFileSubsystemInputStreamSources other) {
        for (Map.Entry<String, ZipEntryInputStreamSource> entry : other.inputStreamSourceMap.entrySet()) {
            if (!this.inputStreamSourceMap.containsKey(entry.getKey())) {
                this.inputStreamSourceMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void addAllSubsystemFileSourcesFromZipFile(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            // extract subsystem template and schema, if present
            if (zip.getEntry("subsystem-templates") != null) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.startsWith("subsystem-templates/")) {
                            addSubsystemFileSource(entryName.substring("subsystem-templates/".length()), file, entry);
                        }
                    }
                }
            }
        }
    }

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        return inputStreamSourceMap.get(subsystemFileName);
    }

    @Override
    public String toString() {
        return "zip subsystem parser factory files: "+ inputStreamSourceMap.keySet();
    }

}
