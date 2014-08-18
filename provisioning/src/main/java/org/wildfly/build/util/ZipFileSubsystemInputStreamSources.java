package org.wildfly.build.util;

import org.wildfly.build.configassembly.SubsystemInputStreamSources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

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

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        return inputStreamSourceMap.get(subsystemFileName);
    }

    @Override
    public String toString() {
        return "zip subsystem parser factory files: "+ inputStreamSourceMap.keySet();
    }
}
