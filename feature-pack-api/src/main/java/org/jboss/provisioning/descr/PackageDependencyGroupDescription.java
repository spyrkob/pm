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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A group of package dependencies.
 * Dependencies on packages belonging to the same feature pack are put
 * into a single group.
 *
 * @author Alexey Loubyansky
 */
public class PackageDependencyGroupDescription {

    public static class Builder {

        private final String groupName;
        protected Map<String, PackageDependencyDescription> dependencies = Collections.emptyMap();

        private Builder(String name) {
            this.groupName = name;
        }

        public Builder addDependency(String packageName) {
            return addDependency(packageName, false);
        }

        public Builder addDependency(String packageName, boolean optional) {
            return addDependency(PackageDependencyDescription.create(packageName, optional));
        }

        public Builder addDependency(PackageDependencyDescription depDescr) {
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonMap(depDescr.getName(), depDescr);
                    break;
                case 1:
                    dependencies = new HashMap<>(dependencies);
                default:
                    dependencies.put(depDescr.getName(), depDescr);
            }
            return this;
        }

        public PackageDependencyGroupDescription build() {
            return new PackageDependencyGroupDescription(this);
        }
    }

    public static Builder builder() {
        return builder(null);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    private final String groupName;
    private final Map<String, PackageDependencyDescription> dependencies;

    private PackageDependencyGroupDescription(Builder builder) {
        this.groupName = builder.groupName;
        this.dependencies = Collections.unmodifiableMap(builder.dependencies);
    }

    public boolean isExternal() {
        return groupName != null;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isEmpty() {
        return dependencies.isEmpty();
    }

    public Set<String> getPackageNames() {
        return dependencies.keySet();
    }

    public Collection<PackageDependencyDescription> getDescriptions() {
        return dependencies.values();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((groupName == null) ? 0 : groupName.hashCode());
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
        PackageDependencyGroupDescription other = (PackageDependencyGroupDescription) obj;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (groupName == null) {
            if (other.groupName != null)
                return false;
        } else if (!groupName.equals(other.groupName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(groupName == null) {
            buf.append("local ");
        } else {
            buf.append(groupName).append(' ');
        }
        return buf.append(dependencies).append(']').toString();
    }
}
