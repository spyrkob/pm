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

package org.jboss.pm.def;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.pm.GAV;
import org.jboss.pm.def.FeaturePackDef.FeaturePackDefBuilder;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class InstallationDefBuilder {

    public static InstallationDefBuilder newInstance(GAV gav, File homeDir) {
        return new InstallationDefBuilder(gav, homeDir);
    }

    private final File homeDir;
    private FileFilter contentFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().charAt(0) != '.';
        }
    };

    private final GAV gav;
    private Map<GAV, FeaturePackDef> featurePacks = Collections.emptyMap();

    InstallationDefBuilder(GAV gav, File homeDir) {
        assert gav != null : "GAV is null";
        assert homeDir != null : "homeDir is null";
        this.gav = gav;
        this.homeDir = homeDir;
    }

    public InstallationDefBuilder setContentFilter(FileFilter filter) {
        this.contentFilter = filter;
        return this;
    }

    public InstallationDefBuilder defineFeaturePack(GAV gav, File path) throws InstallationDefException {

        final FeaturePackDefBuilder fpBuilder = FeaturePackDef.builder(gav);
        if(path.exists()) {
            if(path.isFile()) {
                fpBuilder.addGroup(PackageDef.packageBuilder(path.getName()).addContentPath(path.getAbsolutePath()).build());
            } else {
                for(File f : path.listFiles()) {
                    if(contentFilter.accept(f)) {
                        defineGroup(fpBuilder, f.getName(), f);
                    }
                }
            }
        }
        addFeaturePack(fpBuilder.build());
        return this;
    }

    public InstallationDefBuilder addFeaturePack(FeaturePackDef fp) {
        assert fp != null : "fp is null";
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonMap(fp.getGAV(), fp);
                break;
            case 1:
                featurePacks = new HashMap<GAV, FeaturePackDef>(featurePacks);
            default:
                featurePacks.put(fp.getGAV(), fp);
        }
        return this;
    }

    public InstallationDef build() throws InstallationDefException {
        defineFeaturePack(new GAV(gav.getGroupId() + '.' + gav.getArtifactId(), gav.getArtifactId() + "-feature-pack", gav.getVersion()), homeDir);
        return new InstallationDef(gav, Collections.unmodifiableMap(featurePacks));
    }

    private void defineGroup(FeaturePackDefBuilder fpBuilder, String name, File path) throws InstallationDefException {
        if(path.isFile()) {
            fpBuilder.addGroup(PackageDef.packageBuilder(name).addContentPath(path.getAbsolutePath()).build());
            return;
        }
        final File[] children = path.listFiles();
        if(children.length == 0) {
            fpBuilder.addGroup(GroupDef.groupBuilder(name).build());
            return;
        }
        final PackageDefBuilder pkgBuilder = PackageDef.packageBuilder(name);
        for(File child : children) {
            if(!contentFilter.accept(child)) {
                continue;
            }
            if(child.isFile()) {
                pkgBuilder.addContentPath(child.getAbsolutePath());
            } else {
                final String childName = name + '.' + child.getName();
                defineGroup(fpBuilder, childName, child);
                pkgBuilder.addDependency(childName);
            }
        }
        fpBuilder.addGroup(pkgBuilder.build());
    }
}
