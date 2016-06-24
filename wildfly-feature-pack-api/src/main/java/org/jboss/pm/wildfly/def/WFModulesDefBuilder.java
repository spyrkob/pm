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

import org.jboss.pm.def.FeaturePackDef.FeaturePackDefBuilder;
import org.jboss.pm.def.PackageDef;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFModulesDefBuilder {

    private final String relativeDir;
    private List<String> names = Collections.emptyList();

    public WFModulesDefBuilder(String relativeDir) {
        this.relativeDir = relativeDir;
    }

    public void addModule(String name) {
        switch(names.size()) {
            case 0:
                names = Collections.singletonList(name);
                break;
            case 1:
                names = new ArrayList<String>(names);
            default:
                names.add(name);
        }
    }

    void processModules(FeaturePackDefBuilder fpBuilder, PackageDefBuilder pkgBuilder, DefBuildContext ctx) {
        final File modulesDir;
        if(relativeDir != null) {
            modulesDir = new File(ctx.getHomeDir(), relativeDir);
        } else {
            modulesDir = ctx.getModulesDir();
        }
        if(names.isEmpty()) {
            // all
            final List<String> path = new ArrayList<String>();
            for(File dir : modulesDir.listFiles()) {
                if(dir.isDirectory()) {
                    path.add(dir.getName());
                    for(File child : dir.listFiles()) {
                        if(child.isDirectory()) {
                            processModules(fpBuilder, pkgBuilder, relativeDir, path, child);
                        }
                    }
                    path.remove(path.size() - 1);
                }
            }
        } else {
            // TODO preselected
        }
    }

    private void processModules(FeaturePackDefBuilder fpBuilder, PackageDefBuilder pkgBuilder, String modulesPath, List<String> path, File dir) {

        final File moduleXml = new File(dir, "module.xml");
        if(!moduleXml.exists()) {
            for(File child : dir.listFiles()) {
                if (child.isDirectory()) {
                    path.add(dir.getName());
                    processModules(fpBuilder, pkgBuilder, relativeDir, path, child);
                    path.remove(path.size() - 1);
                }
            }
            return;
        }

        final StringBuilder moduleName = new StringBuilder();
        final StringBuilder contentPath = new StringBuilder(relativeDir).append('/');
        moduleName.append(path.get(0));
        contentPath.append(path.get(0));
        for(int i = 1; i < path.size(); ++i) {
            final String part = path.get(i);
            moduleName.append('.').append(part);
            contentPath.append('/').append(part);
        }
        final PackageDefBuilder moduleBuilder = PackageDef.packageBuilder(moduleName.toString());
        addContent(moduleBuilder, dir, contentPath.toString());
        fpBuilder.addGroup(moduleBuilder.build());
        pkgBuilder.addDependency(moduleName.toString());
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
