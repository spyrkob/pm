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
import org.jboss.pm.descr.FeaturePackDescription;
import org.jboss.pm.descr.InstallationDescription;
import org.jboss.pm.descr.InstallationDescriptionBuilder;
import org.jboss.pm.descr.InstallationDescriptionException;
import org.jboss.pm.descr.PackageDescription;
import org.jboss.pm.util.Errors;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFInstallationDescriptionBuilder {

    private static final List<String> PRODUCT_MODULE = Arrays.asList("org", "jboss", "as", "product");
    //private static final String RELEASE_NAME = "JBoss-Product-Release-Name";
    private static final String RELEASE_VERSION = "JBoss-Product-Release-Version";

    private Path homeDir;
    private String modulesPath;
    private Path modulesDir;
    private InstallationDescriptionBuilder installBuilder;

    private String fpGroupId;
    private String fpArtifactId;
    private String fpVersion;
    private FeaturePackDescription.Builder fpBuilder;

    private PackageDescription.Builder pkgBuilder;

    public InstallationDescription build(WFInstallationDescription wfDescr, Path homeDir) throws InstallationDescriptionException {
        this.homeDir = homeDir;
        modulesPath = wfDescr.getModulesPath();
        modulesDir = homeDir.resolve(modulesPath);
        installBuilder = InstallationDescriptionBuilder.newInstance();
        for(WFFeaturePackDescription fpDescr : wfDescr.getFeaturePacks()) {
            build(fpDescr);
        }
        return installBuilder.build();
    }

    void build(WFFeaturePackDescription fpDescr) throws InstallationDescriptionException {
        fpBuilder = FeaturePackDescription.builder();
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

    void build(WFPackageDescription wfPkg) throws InstallationDescriptionException {
        pkgBuilder = PackageDescription.packageBuilder(wfPkg.getName());
        for(WFModulesDescription wfModules : wfPkg.getModules()) {
            build(wfModules);
        }

        for(String relativePath : wfPkg.getRelativePaths()) {
            final Path f = homeDir.resolve(relativePath);
            if(!Files.exists(f)) {
                throw new InstallationDescriptionException("Failed to locate " + f);
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

        fpBuilder.addTopGroup(pkgBuilder.build());
        pkgBuilder = null;
    }

    void build(WFModulesDescription wfModules) throws InstallationDescriptionException {

        String relativePath = wfModules.getRelativeDir();
        if(relativePath != null) {
            modulesDir = homeDir.resolve(relativePath);
        } else {
            relativePath = modulesPath;
            modulesDir = WFInstallationDescriptionBuilder.this.modulesDir;
        }
        if(wfModules.getNames().isEmpty()) {
            if(!Files.exists(modulesDir)) {
                throw new InstallationDescriptionException("Modules directory " + modulesDir.toAbsolutePath() + " does not exist.");
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

    private void processModules(String modulesPath, List<String> path, Path dir) throws InstallationDescriptionException {

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
                throw new InstallationDescriptionException("Failed to read product info from " + manifest.toAbsolutePath(), e);
            }
            fpVersion = props.getProperty(RELEASE_VERSION);
        }
        moduleName.append('.').append(dir.getFileName().toString()); // adding the slot to the name (hibernate modules in wildfly)
        final PackageDescription.Builder moduleBuilder = PackageDescription.packageBuilder(moduleName.toString());
        addContent(moduleBuilder, dir, contentPath.toString());
        fpBuilder.addGroup(moduleBuilder.build());
        pkgBuilder.addDependency(moduleName.toString());
    }

    private void addContent(PackageDescription.Builder builder, Path f, String relativePath) throws InstallationDescriptionException {
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

    private void failedToReadDirectory(Path p, IOException e) throws InstallationDescriptionException {
        throw new InstallationDescriptionException(Errors.readDirectory(p), e);
    }
}
