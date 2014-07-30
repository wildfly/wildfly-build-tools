package org.wildfly.build.pack.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Eduardo Martins
 */
public class Config {

    private final List<ConfigFile> standaloneConfigFiles = new ArrayList<>();
    private final List<ConfigFile> domainConfigFiles = new ArrayList<>();

    public List<ConfigFile> getStandaloneConfigFiles() {
        return standaloneConfigFiles;
    }

    public List<ConfigFile> getDomainConfigFiles() {
        return domainConfigFiles;
    }
}
