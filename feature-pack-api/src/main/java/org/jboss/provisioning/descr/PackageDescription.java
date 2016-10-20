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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes a package as it appears in a feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class PackageDescription {

    public static class Builder {

        protected String name;
        protected Map<String, PackageDependencyDescription> dependencies = Collections.emptyMap();

        protected Builder() {
            this(null);
        }

        protected Builder(String name) {
            this.name = name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addDependency(String dependencyName) {
            return addDependency(dependencyName, false);
        }

        public Builder addDependency(String dependencyName, boolean optional) {
            return addDependency(PackageDependencyDescription.create(dependencyName, optional));
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

        public PackageDescription build() {
            return new PackageDescription(name, Collections.unmodifiableMap(dependencies));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    protected final String name;
    protected final Map<String, PackageDependencyDescription> dependencies;

    protected PackageDescription(String name, Map<String, PackageDependencyDescription> dependencies) {
        assert name != null : "name is null";
        assert dependencies != null : "dependencies is null";
        this.name = name;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public Set<String> getDependencyNames() {
        return dependencies.keySet();
    }
    public Collection<PackageDependencyDescription> getDependencies() {
        return dependencies.values();
    }

    void logContent(DescrFormatter logger) throws IOException {
        logger.print("Package ");
        logger.println(name);
        if(!dependencies.isEmpty()) {
            logger.increaseOffset();
            logger.println("Dependencies");
            logger.increaseOffset();
            for(PackageDependencyDescription dependency : dependencies.values()) {
                logger.println(dependency.toString());
            }
            logger.decreaseOffset();
            logger.decreaseOffset();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        PackageDescription other = (PackageDescription) obj;
        if (dependencies == null) {
            if (other.dependencies != null)
                return false;
        } else if (!dependencies.equals(other.dependencies))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
