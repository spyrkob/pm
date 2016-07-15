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

package org.jboss.pm.descr;

import java.io.File;
import java.io.FileFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.pm.GAV;

/**
 * This class collects feature packs descriptions and produces an installation
 * description.
 *
 * @author Alexey Loubyansky
 */
public class InstallationDescriptionBuilder {

    public static InstallationDescriptionBuilder newInstance() {
        return new InstallationDescriptionBuilder();
    }

    private FileFilter contentFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().charAt(0) != '.';
        }
    };

    private Map<GAV, FeaturePackDescription> featurePacks = Collections.emptyMap();

    InstallationDescriptionBuilder() {
    }

    public InstallationDescriptionBuilder setContentFilter(FileFilter filter) {
        this.contentFilter = filter;
        return this;
    }

    public InstallationDescriptionBuilder defineFeaturePack(GAV gav, File path) throws InstallationDescriptionException {

        final FeaturePackDescription.Builder fpBuilder = FeaturePackDescription.builder(gav);
        if(path.exists()) {
            if(path.isFile()) {
                fpBuilder.addTopGroup(PackageDescription.packageBuilder(path.getName()).addContentPath(path.getAbsolutePath()).build());
            } else {
                for(File f : path.listFiles()) {
                    if(contentFilter.accept(f)) {
                        defineGroup(fpBuilder, f.getName(), f, true);
                    }
                }
            }
        }
        addFeaturePack(fpBuilder.build());
        return this;
    }

    public InstallationDescriptionBuilder addFeaturePack(FeaturePackDescription fp) {
        assert fp != null : "fp is null";
        switch(featurePacks.size()) {
            case 0:
                featurePacks = Collections.singletonMap(fp.getGAV(), fp);
                break;
            case 1:
                featurePacks = new HashMap<GAV, FeaturePackDescription>(featurePacks);
            default:
                featurePacks.put(fp.getGAV(), fp);
        }
        return this;
    }

    public InstallationDescription build() throws InstallationDescriptionException {
        return new InstallationDescription(Collections.unmodifiableMap(featurePacks));
    }

    private void defineGroup(FeaturePackDescription.Builder fpBuilder, String name, File path, boolean top) throws InstallationDescriptionException {
        if(path.isFile()) {
            if(top) {
                fpBuilder.addTopGroup(PackageDescription.packageBuilder(name).addContentPath(path.getAbsolutePath()).build());
            } else {
                fpBuilder.addGroup(PackageDescription.packageBuilder(name).addContentPath(path.getAbsolutePath()).build());
            }
            return;
        }
        final File[] children = path.listFiles();
        if(children.length == 0) {
            if(top) {
                fpBuilder.addTopGroup(GroupDescription.groupBuilder(name).build());
            } else {
                fpBuilder.addGroup(GroupDescription.groupBuilder(name).build());
            }
            return;
        }
        final PackageDescription.Builder pkgBuilder = PackageDescription.packageBuilder(name);
        for(File child : children) {
            if(!contentFilter.accept(child)) {
                continue;
            }
            if(child.isFile()) {
                pkgBuilder.addContentPath(child.getAbsolutePath());
            } else {
                final String childName = name + '.' + child.getName();
                defineGroup(fpBuilder, childName, child, false);
                pkgBuilder.addDependency(childName);
            }
        }
        if(top) {
            fpBuilder.addTopGroup(pkgBuilder.build());
        } else {
            fpBuilder.addGroup(pkgBuilder.build());
        }
    }
}
