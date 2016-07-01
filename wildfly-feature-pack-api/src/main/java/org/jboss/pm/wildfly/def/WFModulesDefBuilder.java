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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.def.PackageDef;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFModulesDefBuilder {

    private static final List<String> PRODUCT_MODULE = Arrays.asList("org", "jboss", "as", "product");
    //private static final String RELEASE_NAME = "JBoss-Product-Release-Name";
    private static final String RELEASE_VERSION = "JBoss-Product-Release-Version";

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

    void processModules(DefBuildContext ctx) throws InstallationDefException {
        final File modulesDir;
        if(relativeDir != null) {
            modulesDir = new File(ctx.getHomeDir(), relativeDir);
        } else {
            modulesDir = ctx.getModulesDir();
        }
        if(names.isEmpty()) {
            if(!modulesDir.exists()) {
                throw new InstallationDefException("Modules directory " + modulesDir.getAbsolutePath() + " does not exist.");
            }
            // all
            final List<String> path = new ArrayList<String>();
            for(File dir : modulesDir.listFiles()) {
                if(dir.isDirectory()) {
                    path.add(dir.getName());
                    for(File child : dir.listFiles()) {
                        if(child.isDirectory()) {
                            processModules(ctx, relativeDir, path, child);
                        }
                    }
                    path.remove(path.size() - 1);
                }
            }
        } else {
            // TODO preselected
        }
    }

    private void processModules(DefBuildContext ctx, String modulesPath, List<String> path, File dir) throws InstallationDefException {

        final File moduleXml = new File(dir, "module.xml");
        if(!moduleXml.exists()) {
            for(File child : dir.listFiles()) {
                if (child.isDirectory()) {
                    path.add(dir.getName());
                    processModules(ctx, modulesPath, path, child);
                    path.remove(path.size() - 1);
                }
            }
            return;
        }

        final StringBuilder moduleName = new StringBuilder();
        final StringBuilder contentPath = new StringBuilder(modulesPath).append('/');
        moduleName.append(path.get(0));
        contentPath.append(path.get(0));
        for(int i = 1; i < path.size(); ++i) {
            final String part = path.get(i);
            moduleName.append('.').append(part);
            contentPath.append('/').append(part);
        }
        if(ctx.fpBuilder.getArtifactId() == null && PRODUCT_MODULE.equals(path)) {
            ctx.fpBuilder.setArtifactId(dir.getName());
            final File manifest = new File(dir, "dir/META-INF/MANIFEST.MF");
            final Properties props = new Properties();
            try(FileReader reader = new FileReader(manifest)) {
                props.load(reader);
            } catch(IOException e) {
                throw new InstallationDefException("Failed to read product info from " + manifest.getAbsolutePath(), e);
            }
            ctx.fpBuilder.setVersion(props.getProperty(RELEASE_VERSION));
        }
        moduleName.append('.').append(dir.getName()); // adding the slot to the name (hibernate modules in wildfly)
        final PackageDefBuilder moduleBuilder = PackageDef.packageBuilder(moduleName.toString());
        addContent(moduleBuilder, dir, contentPath.toString());
        ctx.fpBuilder.addModulePackage(moduleBuilder.build());
        ctx.pkgBuilder.addDependency(moduleName.toString());
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
