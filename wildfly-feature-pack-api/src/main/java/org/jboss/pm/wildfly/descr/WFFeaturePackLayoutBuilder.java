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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.jboss.pm.GAV;
import org.jboss.pm.def.FeaturePackDef;
import org.jboss.pm.def.FeaturePackDef.FeaturePackDefBuilder;
import org.jboss.pm.def.InstallationDef;
import org.jboss.pm.def.InstallationDefBuilder;
import org.jboss.pm.def.InstallationDefException;
import org.jboss.pm.def.PackageDef;
import org.jboss.pm.def.PackageDef.PackageDefBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFFeaturePackLayoutBuilder {

    private static final List<String> PRODUCT_MODULE = Arrays.asList("org", "jboss", "as", "product");
    //private static final String RELEASE_NAME = "JBoss-Product-Release-Name";
    private static final String RELEASE_VERSION = "JBoss-Product-Release-Version";

    private Path homeDir;
    private String modulesPath;
    private Path modulesDir;
    private InstallationDefBuilder installBuilder;

    private String fpGroupId;
    private String fpArtifactId;
    private String fpVersion;
    private FeaturePackDefBuilder fpBuilder;

    private PackageDefBuilder pkgBuilder;

    public InstallationDef build(WFInstallationDescription wfDescr, Path homeDir) throws InstallationDefException {
        this.homeDir = homeDir;
        modulesPath = wfDescr.getModulesPath();
        modulesDir = homeDir.resolve(modulesPath);
        installBuilder = InstallationDefBuilder.newInstance();
        for(WFFeaturePackDescription fpDescr : wfDescr.getFeaturePacks()) {
            build(fpDescr);
        }
        return installBuilder.build();
    }

    void build(WFFeaturePackDescription fpDescr) throws InstallationDefException {
        fpBuilder = FeaturePackDef.builder();
        fpGroupId = fpDescr.getGroupId();
        fpArtifactId = fpDescr.getArtifactId();
        fpVersion = fpDescr.getVersion();

        for(WFPackageDescription wfPkg : fpDescr.getPackages()) {
            build(wfPkg);
        }

        fpBuilder.setGAV(new GAV(fpGroupId, fpArtifactId, fpVersion));
        installBuilder.addFeaturePack(fpBuilder.build());

        fpBuilder = null;
        fpGroupId = null;
        fpArtifactId = null;
        fpVersion = null;
    }

    void build(WFPackageDescription wfPkg) throws InstallationDefException {
        pkgBuilder = PackageDef.packageBuilder(wfPkg.getName());
        for(WFModulesDescription wfModules : wfPkg.getModules()) {
            build(wfModules);
        }

        for(String relativePath : wfPkg.getRelativePaths()) {
            final Path f = homeDir.resolve(relativePath);
            if(!Files.exists(f)) {
                throw new InstallationDefException("Failed to locate " + f);
            }
            if(Files.isDirectory(f)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(f)) {
                    final Iterator<Path> children = stream.iterator();
                    if(children.hasNext()) {
                        addContent(pkgBuilder, children.next(), f.getFileName().toString());
                        while(children.hasNext()) {
                            addContent(pkgBuilder, children.next(), f.getFileName().toString());
                        }
                    } else {
                        pkgBuilder.addContentPath(relativePath);
                    }
                } catch(IOException e) {
                    failedToReadDirectory(f, e);
                }
            } else {
                pkgBuilder.addContentPath(relativePath);
            }
        }
        for(String packageRef : wfPkg.getPackageRefs()) {
            pkgBuilder.addDependency(packageRef);
        }

        fpBuilder.addGroup(pkgBuilder.build());
        pkgBuilder = null;
    }

    void build(WFModulesDescription wfModules) throws InstallationDefException {

        String relativePath = wfModules.getRelativeDir();
        if(relativePath != null) {
            modulesDir = homeDir.resolve(relativePath);
        } else {
            relativePath = modulesPath;
            modulesDir = WFFeaturePackLayoutBuilder.this.modulesDir;
        }
        if(wfModules.getNames().isEmpty()) {
            if(!Files.exists(modulesDir)) {
                throw new InstallationDefException("Modules directory " + modulesDir.toAbsolutePath() + " does not exist.");
            }
            // all
            final List<String> path = new ArrayList<String>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modulesDir)) {
                for (Path entry: stream) {
                    if(Files.isDirectory(entry)) {
                        path.add(entry.getFileName().toString());
                        try (DirectoryStream<Path> stream2 = Files.newDirectoryStream(entry)) {
                            for (Path entry2: stream2) {
                                if(Files.isDirectory(entry)) {
                                    processModules(relativePath, path, entry2);
                                }
                            }
                        } catch(IOException e) {
                            failedToReadDirectory(entry, e);
                        }
                        path.remove(path.size() - 1);
                    }
                }
            } catch(IOException e) {
                failedToReadDirectory(modulesDir, e);
            }
        } else {
            // TODO preselected
        }

    }

    private void processModules(String modulesPath, List<String> path, Path dir) throws InstallationDefException {

        final Path moduleXml = dir.resolve("module.xml");
        if(!Files.exists(moduleXml)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry: stream) {
                    if (Files.isDirectory(entry)) {
                        path.add(dir.getFileName().toString());
                        processModules(modulesPath, path, entry);
                        path.remove(path.size() - 1);
                    }
                }
            } catch(IOException e) {
                failedToReadDirectory(dir, e);
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
        if(fpArtifactId == null && PRODUCT_MODULE.equals(path)) {
            fpArtifactId = dir.getFileName().toString();
            final Path manifest = dir.resolve("dir/META-INF/MANIFEST.MF");
            final Properties props = new Properties();
            try(BufferedReader reader = Files.newBufferedReader(manifest)) {
                props.load(reader);
            } catch(IOException e) {
                throw new InstallationDefException("Failed to read product info from " + manifest.toAbsolutePath(), e);
            }
            fpVersion = props.getProperty(RELEASE_VERSION);
        }
        moduleName.append('.').append(dir.getFileName().toString()); // adding the slot to the name (hibernate modules in wildfly)
        final PackageDefBuilder moduleBuilder = PackageDef.packageBuilder(moduleName.toString());
        addContent(moduleBuilder, dir, contentPath.toString());
        fpBuilder.addGroup(moduleBuilder.build());
        pkgBuilder.addDependency(moduleName.toString());
    }

    private void addContent(PackageDefBuilder builder, Path f, String relativePath) throws InstallationDefException {
        if(Files.isDirectory(f)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(f)) {
                for (Path entry: stream) {
                    addContent(builder, entry, relativePath + '/' + f.getFileName());
                }
            } catch(IOException e) {
                failedToReadDirectory(f, e);
            }
        } else {
            builder.addContentPath(relativePath + '/' + f.getFileName());
        }
    }

    private void failedToReadDirectory(Path p, IOException e) throws InstallationDefException {
        throw new InstallationDefException("Failed to read directory " + p.toAbsolutePath(), e);
    }
}
