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
