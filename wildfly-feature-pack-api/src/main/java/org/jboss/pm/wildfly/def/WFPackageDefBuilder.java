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

package org.jboss.pm.wildfly.def;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.pm.def.PackageDef;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFPackageDefBuilder {

    private final String name;
    private List<String> relativePaths = Collections.emptyList();
    private List<String> modules = Collections.emptyList();
    private List<String> packageRefs = Collections.emptyList();

    public WFPackageDefBuilder(String name) {
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

    public void addModule(String module) {
        switch(modules.size()) {
            case 0:
                modules = Collections.singletonList(module);
                break;
            case 1:
                modules = new ArrayList<String>(modules);
            default:
                modules.add(module);
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

    public PackageDef build() {
        final PackageDefBuilder builder = PackageDef.packageBuilder(name);
        for(String relativePath : relativePaths) {
            builder.addContentPath(relativePath);
        }
        for(String packageRef : packageRefs) {
            builder.addDependency(packageRef);
        }
        return builder.build();
    }
}
