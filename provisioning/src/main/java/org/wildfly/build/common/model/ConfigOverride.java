package org.wildfly.build.common.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigOverride {

    private final Map<String, ConfigFileOverride> standaloneConfigFiles = new HashMap<>();
    private final Map<String, ConfigFileOverride> domainConfigFiles = new HashMap<>();

    public Map<String, ConfigFileOverride> getStandaloneConfigFiles() {
        return standaloneConfigFiles;
    }

    public Map<String, ConfigFileOverride> getDomainConfigFiles() {
        return domainConfigFiles;
    }
}
