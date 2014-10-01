package org.wildfly.build.common.model;

import java.util.Map;

/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigFile {

    private final Map<String, String> properties;
    private final String template;
    private final String subsystems;
    private final String outputFile;

    public ConfigFile(Map<String, String> properties, String template, String subsystems, String outputFile) {
        this.properties = properties;
        this.template = template;
        this.subsystems = subsystems;
        this.outputFile = outputFile;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getTemplate() {
        return template;
    }

    public String getSubsystems() {
        return subsystems;
    }

    public String getOutputFile() {
        return outputFile;
    }
}
