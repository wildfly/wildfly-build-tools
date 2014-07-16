package org.wildfly.build.pack.model;

/**
 * Represents a module
 *
 * @author Stuart Douglas
 */
public class Module {

    private final Pack pack;
    private final ModuleIdentifier moduleIdentifier;


    public Module(Pack pack, ModuleIdentifier moduleIdentifier) {
        this.pack = pack;
        this.moduleIdentifier = moduleIdentifier;
    }
}
