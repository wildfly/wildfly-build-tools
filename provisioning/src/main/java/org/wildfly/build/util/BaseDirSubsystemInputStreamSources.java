package org.wildfly.build.util;

import org.wildfly.build.configassembly.SubsystemInputStreamSources;

import java.io.File;

/**
 * @author Eduardo Martins
 */
public class BaseDirSubsystemInputStreamSources implements SubsystemInputStreamSources {

    private final File baseDir;

    public BaseDirSubsystemInputStreamSources(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        final File subsystemFile = new File(baseDir, subsystemFileName);
        return new FileInputStreamSource(subsystemFile);
    }

}
