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

package org.jboss.pm.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.jboss.pm.Constants;
import org.jboss.pm.descr.FeaturePackDescription;
import org.jboss.pm.descr.GroupDescription;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackInstaller {

    private Path installDir;
    private Path fpPackagesDir;
    private FeaturePackDescription featurePack;
    private Set<String> installedPackages = new HashSet<String>();

    public void install(FeaturePackDescription featurePack, Path fpDir, Path installDir) throws FeaturePackInstallException {

        fpPackagesDir = fpDir.resolve(Constants.PACKAGES);
        this.installDir = installDir;
        this.featurePack = featurePack;

        for(String name : featurePack.getTopGroupNames()) {
            if(!isGroupInstalled(name)) {
                install(featurePack.getGroupDescription(name));
            }
        }
    }

    private void install(GroupDescription group) throws FeaturePackInstallException {
        installedPackages.add(group.getName());
        if(group.hasDependencies()) {
            for(String name : group.getDependencies()) {
                if(!isGroupInstalled(name)) {
                    final GroupDescription dependency = featurePack.getGroupDescription(name);
                    if(dependency == null) {
                        throw new FeaturePackInstallException(Errors.packageNotFound(name));
                    }
                    install(dependency);
                }
            }
        }
        if(group.hasContent()) {
            final Path groupSrcDir = fpPackagesDir.resolve(group.getName()).resolve(Constants.CONTENT);
            assertExists(groupSrcDir);
            try {
                IoUtils.copy(groupSrcDir, installDir);
            } catch (IOException e) {
                throw new FeaturePackInstallException(Errors.packageContentCopyFailed(group.getName()), e);
            }
        }
    }

    private boolean isGroupInstalled(String name) {
        return installedPackages.contains(name);
    }

    private void assertExists(Path path) throws FeaturePackInstallException {
        if(!Files.exists(path)) {
            throw new FeaturePackInstallException(Errors.pathDoesNotExist(path));
        }
    }
}
