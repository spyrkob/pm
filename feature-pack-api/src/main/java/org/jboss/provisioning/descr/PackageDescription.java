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

package org.jboss.provisioning.descr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.util.DescrFormatter;

/**
 * This class describes a package as it appears in a feature-pack.
 *
 * @author Alexey Loubyansky
 */
public class PackageDescription {

    public static class Builder {

        protected String name;
        protected List<String> dependencies = Collections.emptyList();

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
            assert dependencyName != null : "dependency is null";
            switch(dependencies.size()) {
                case 0:
                    dependencies = Collections.singletonList(dependencyName);
                    break;
                case 1:
                    dependencies = new ArrayList<String>(dependencies);
                default:
                    dependencies.add(dependencyName);
            }
            return this;
        }

        public PackageDescription build() {
            return new PackageDescription(name, Collections.unmodifiableList(dependencies));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    protected final String name;
    protected final List<String> dependencies;

    protected PackageDescription(String name, List<String> dependencies) {
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

    public List<String> getDependencies() {
        return dependencies;
    }

    void logContent(DescrFormatter logger) throws IOException {
        logger.print("Package ");
        logger.println(name);
        if(!dependencies.isEmpty()) {
            logger.increaseOffset();
            logger.println("Dependencies");
            logger.increaseOffset();
            for(String dependency : dependencies) {
                logger.println(dependency);
            }
            logger.decreaseOffset();
            logger.decreaseOffset();
        }
    }
}
