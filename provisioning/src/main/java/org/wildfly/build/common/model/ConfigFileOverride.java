package org.wildfly.build.common.model;

import org.wildfly.build.configassembly.SubsystemConfig;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigFileOverride {

    private final Map<String, String> properties;
    private final boolean useTemplate;
    private Map<String, Map<String, SubsystemConfig>> subsystems;
    private final String outputFile;

    public ConfigFileOverride(Map<String, String> properties, boolean useTemplate, Map<String, Map<String, SubsystemConfig>> subsystems, String outputFile) {
        this.properties = properties;
        this.useTemplate = useTemplate;
        this.subsystems = subsystems;
        this.outputFile = outputFile;
    }

    public ConfigFileOverride(boolean useTemplate, String outputFile) {
        this(new HashMap<String, String>(), useTemplate, null, outputFile);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isUseTemplate() {
        return useTemplate;
    }

    public Map<String, Map<String, SubsystemConfig>> getSubsystems() {
        return subsystems;
    }

    public void setSubsystems(Map<String, Map<String, SubsystemConfig>> subsystems) {
        this.subsystems = subsystems;
    }

    public String getOutputFile() {
        return outputFile;
    }

}
