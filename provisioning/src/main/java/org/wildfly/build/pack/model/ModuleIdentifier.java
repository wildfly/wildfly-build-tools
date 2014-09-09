/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.build.pack.model;

/**
 * Representation of a module identifier
 *
 * @author Stuart Douglas
 */
public class ModuleIdentifier {

    private final String name;
    private final String slot;

    public ModuleIdentifier(String name, String slot) {
        this.name = name;
        this.slot = slot;
    }

    public ModuleIdentifier(String name) {
        this.name = name;
        this.slot = "main";
    }

    public String getName() {
        return name;
    }

    public String getSlot() {
        return slot;
    }

    public static ModuleIdentifier fromString(String moduleId) {
        String[] parts = moduleId.split(":");
        if (parts.length == 1) {
            return new ModuleIdentifier(parts[0]);
        } else if (parts.length == 2) {
            return new ModuleIdentifier(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Not a valid module identifier " + moduleId);
        }
    }

    @Override
    public String toString() {
        return "ModuleIdentifier{" +
                "name='" + name + '\'' +
                ", slot='" + slot + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModuleIdentifier that = (ModuleIdentifier) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (slot != null ? !slot.equals(that.slot) : that.slot != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (slot != null ? slot.hashCode() : 0);
        return result;
    }
}
