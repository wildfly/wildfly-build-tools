package org.wildfly.build.pack.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Wildfly feature pack. This is used by both the build and provisioning tools,
 * to represent the contents of a zipped up feature pack.
 *
 * This class is immutable.
 *
 * @author Stuart Douglas
 */
public class Pack {

    /**
     * The default versions of all maven artifacts referenced by this pack.
     */
    private final Map<String, String> versions;

    /**
     * Map map of module name:slot to the module definition
     */
    private final Map<ModuleIdentifier, Module> modules;
    private final Map<String, ContentItem> contents;

    /**
     * The configuration template files.
     */
    private final Set<String> configurationTemplates;

    public Pack(Map<String, String> versions, Map<ModuleIdentifier, Module> modules, Map<String, ContentItem> contents, Set<String> configurationTemplates) {
        this.versions = versions;
        this.modules = modules;
        this.contents = contents;
        this.configurationTemplates = configurationTemplates;
    }


    /**
     * Returns the version for a given artifact
     * @param artifact The artifact
     * @return The version
     */
    public String getVersion(final String artifact) {
        return versions.get(artifact);
    }

    /**
     *
     * @return An immutable map of the versions artifacts to version number
     */
    public Map<String, String> getVersions() {
        return Collections.unmodifiableMap(versions);
    }

    public Map<ModuleIdentifier, Module> getModules() {
        return modules;
    }

}
