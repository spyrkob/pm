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

package org.jboss.pm.wildfly.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFPackageDescription {

    public static class Builder {

        private String name;
        private List<String> relativePaths = Collections.emptyList();
        private List<WFModulesDescription> modules = Collections.emptyList();
        private List<String> packageRefs = Collections.emptyList();

        Builder() {
        }

        public void setName(String name) {
            this.name = name;
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

        public void addModule(WFModulesDescription modulesDef) {
            switch(modules.size()) {
                case 0:
                    modules = Collections.singletonList(modulesDef);
                    break;
                case 1:
                    modules = new ArrayList<WFModulesDescription>(modules);
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

        public WFPackageDescription build() {
            return new WFPackageDescription(name, Collections.unmodifiableList(relativePaths), Collections.unmodifiableList(modules), Collections.unmodifiableList(packageRefs));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String name;
    private final List<String> relativePaths;
    private final List<WFModulesDescription> modules;
    private final List<String> packageRefs;

    public WFPackageDescription(String name, List<String> relativePaths, List<WFModulesDescription> modules, List<String> packageRefs) {
        this.name = name;
        this.relativePaths = relativePaths;
        this.modules = modules;
        this.packageRefs = packageRefs;
    }

    public String getName() {
        return name;
    }

    public List<String> getRelativePaths() {
        return relativePaths;
    }

    public List<WFModulesDescription> getModules() {
        return modules;
    }

    public List<String> getPackageRefs() {
        return packageRefs;
    }
}
