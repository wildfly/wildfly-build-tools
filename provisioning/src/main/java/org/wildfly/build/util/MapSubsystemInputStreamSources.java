package org.wildfly.build.util;

import org.wildfly.build.configassembly.SubsystemInputStreamSources;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public class MapSubsystemInputStreamSources implements SubsystemInputStreamSources {

    private final Map<String, InputStreamSource> inputStreamSources = new HashMap<>();

    public Map<String, InputStreamSource> getInputStreamSources() {
        return inputStreamSources;
    }

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        return inputStreamSources.get(subsystemFileName);
    }

    @Override
    public String toString() {
        return "subsystem input stream sources: "+ new HashSet(inputStreamSources.values());
    }
}
