/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.provisioning.plugin.wildfly;

/**
 * Representation of a module identifier
 *
 * @author Stuart Douglas
 */
class ModuleIdentifier {

    private final String name;
    private final String slot;

    ModuleIdentifier(String name, String slot) {
        this.name = name;
        this.slot = slot;
    }

    ModuleIdentifier(String name) {
        this.name = name;
        this.slot = "main";
    }

    String getName() {
        return name;
    }

    String getSlot() {
        return slot;
    }

    static ModuleIdentifier fromString(String moduleId) {
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
