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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.def.PackageDef;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFPackageDefBuilder {

    private final String name;
    private List<String> relativePaths = Collections.emptyList();
    private List<WFModulesDefBuilder> modules = Collections.emptyList();
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

    public void addModule(WFModulesDefBuilder modulesDef) {
        switch(modules.size()) {
            case 0:
                modules = Collections.singletonList(modulesDef);
                break;
            case 1:
                modules = new ArrayList<WFModulesDefBuilder>(modules);
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

    public PackageDef build(DefBuildContext ctx) throws InstallationDefException {
        ctx.pkgBuilder = PackageDef.packageBuilder(name);
        try {
        if(!modules.isEmpty()) {
            for(WFModulesDefBuilder modulesBuilder : modules) {
                modulesBuilder.processModules(ctx);
            }
        }
        for(String relativePath : relativePaths) {
            final File f = new File(ctx.getHomeDir(), relativePath);
            if(!f.exists()) {
                throw new InstallationDefException("Failed to locate " + f.getAbsolutePath());
            }
            if(f.isDirectory()) {
                final File[] children = f.listFiles();
                if(children.length == 0) {
                    ctx.pkgBuilder.addContentPath(relativePath);
                } else {
                    for (File c : children) {
                        addContent(ctx.pkgBuilder, c, f.getName());
                    }
                }

            } else {
                ctx.pkgBuilder.addContentPath(relativePath);
            }
        }
        for(String packageRef : packageRefs) {
            ctx.pkgBuilder.addDependency(packageRef);
        }
        return ctx.pkgBuilder.build();
        } finally {
            ctx.pkgBuilder = null;
        }
    }

    private void addContent(PackageDefBuilder builder, File f, String relativePath) {
        if(f.isDirectory()) {
            for(File c : f.listFiles()) {
                addContent(builder, c, relativePath + '/' + f.getName());
            }
        } else {
            builder.addContentPath(relativePath + '/' + f.getName());
        }
    }
}
