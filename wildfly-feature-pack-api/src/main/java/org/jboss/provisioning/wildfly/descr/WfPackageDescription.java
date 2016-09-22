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
package org.jboss.provisioning.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfPackageDescription {

    public static class Builder {

        private String name;
        private List<String> relativePaths = Collections.emptyList();
        private List<WfModulesDescription> modules = Collections.emptyList();
        private List<String> packageRefs = Collections.emptyList();
        private boolean optional;

        Builder() {
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        public void addRelativePath(String path) {
            switch(relativePaths.size()) {
                case 0:
                    relativePaths = Collections.singletonList(path);
                    break;
                case 1:
                    relativePaths = new ArrayList<String>(relativePaths);
                default:
                    relativePaths.add(path);
            }
        }

        public void addModule(WfModulesDescription modulesDef) {
            switch(modules.size()) {
                case 0:
                    modules = Collections.singletonList(modulesDef);
                    break;
                case 1:
                    modules = new ArrayList<WfModulesDescription>(modules);
                default:
                    modules.add(modulesDef);
            }
        }

        public void addPackageRef(String packageRef) {
            switch(packageRefs.size()) {
                case 0:
                    packageRefs = Collections.singletonList(packageRef);
                    break;
                case 1:
                    packageRefs = new ArrayList<String>(packageRefs);
                default:
                    packageRefs.add(packageRef);
            }
        }

        public WfPackageDescription build() {
            return new WfPackageDescription(name, optional,
                    Collections.unmodifiableList(relativePaths),
                    Collections.unmodifiableList(modules),
                    Collections.unmodifiableList(packageRefs));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String name;
    private final boolean optional;
    private final List<String> relativePaths;
    private final List<WfModulesDescription> modules;
    private final List<String> packageRefs;

    public WfPackageDescription(String name, boolean optional, List<String> relativePaths, List<WfModulesDescription> modules, List<String> packageRefs) {
        this.name = name;
        this.optional = optional;
        this.relativePaths = relativePaths;
        this.modules = modules;
        this.packageRefs = packageRefs;
    }

    public String getName() {
        return name;
    }

    public boolean isOptional() {
        return optional;
    }

    public List<String> getRelativePaths() {
        return relativePaths;
    }

    public List<WfModulesDescription> getModules() {
        return modules;
    }

    public List<String> getPackageRefs() {
        return packageRefs;
    }
}
