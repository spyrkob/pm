/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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

package org.jboss.provisioning.descr;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageDependencyDescription implements Comparable<PackageDependencyDescription> {

    public static PackageDependencyDescription create(String name) {
        return new PackageDependencyDescription(name, false);
    }

    public static PackageDependencyDescription create(String name, boolean optional) {
        return new PackageDependencyDescription(name, optional);
    }

    private final String name;
    private final boolean optional;

    protected PackageDependencyDescription(String name, boolean optional) {
        this.name = name;
        this.optional = optional;
    }

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (optional ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PackageDependencyDescription other = (PackageDependencyDescription) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (optional != other.optional)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + name + (optional ? " optional]" : " required]");
    }

    @Override
    public int compareTo(PackageDependencyDescription o) {
        return name.compareTo(o.name);
    }
}
