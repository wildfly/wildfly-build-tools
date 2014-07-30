package org.wildfly.build.configassembly;

import org.wildfly.build.util.InputStreamSource;

/**
 * @author Eduardo Martins
 */
public interface SubsystemInputStreamSources {

    /**
     * Retrieves the input stream source mapped to the specified subsystem file name.
     * @param subsystemFileName
     * @return
     */
    InputStreamSource getInputStreamSource(String subsystemFileName);

}
