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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.jboss.pm.Constants;
import org.jboss.pm.GAV;
import org.jboss.pm.descr.FeaturePackDescription;
import org.jboss.pm.descr.InstallationDescription;
import org.jboss.pm.descr.InstallationDescriptionBuilder;
import org.jboss.pm.descr.InstallationDescriptionException;
import org.jboss.pm.descr.PackageDescription;
import org.jboss.pm.descr.FeaturePackDescription.Builder;
import org.jboss.pm.fp.xml.FeaturePackXMLWriter;
import org.jboss.pm.fp.xml.PackageXMLWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFFeaturePackLayoutBuilder {

    private static final List<String> PRODUCT_MODULE = Arrays.asList("org", "jboss", "as", "product");
    //private static final String RELEASE_NAME = "JBoss-Product-Release-Name";
    private static final String RELEASE_VERSION = "JBoss-Product-Release-Version";

    private static final String CONTENT = "content";

    private Path workDir;
    private Path homeDir;
    private String modulesPath;
    private Path modulesDir;
    private InstallationDescriptionBuilder installBuilder;

    private String fpGroupId;
    private String fpArtifactId;
    private String fpVersion;
    private Builder fpBuilder;
    private Path fpDir;

    private PackageDescription.Builder pkgBuilder;

    public InstallationDescription build(WFInstallationDescription wfDescr, Path homeDir, Path workDir) throws InstallationDescriptionException {
        this.workDir = workDir;
        mkdirs(workDir);
        this.homeDir = homeDir;
        modulesPath = wfDescr.getModulesPath();
        modulesDir = homeDir.resolve(modulesPath);
        installBuilder = InstallationDescriptionBuilder.newInstance();
        for(WFFeaturePackDescription fpDescr : wfDescr.getFeaturePacks()) {
            build(fpDescr);
        }
        return installBuilder.build();
    }

    private void mkdirs(Path dir) throws InstallationDescriptionException {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new InstallationDescriptionException("Failed to create directory " + dir.toAbsolutePath(), e);
        }
    }

    void build(WFFeaturePackDescription wfFPDescr) throws InstallationDescriptionException {
        fpBuilder = FeaturePackDescription.builder();
        fpGroupId = wfFPDescr.getGroupId();
        fpArtifactId = wfFPDescr.getArtifactId();
        fpVersion = wfFPDescr.getVersion();

        boolean move = false;
        fpDir = workDir.resolve(fpGroupId);
        if(fpArtifactId != null) {
            fpDir = fpDir.resolve(fpArtifactId);
            fpDir = fpDir.resolve(fpVersion);
        } else {
            fpDir = workDir.resolve("tmp");
            mkdirs(fpDir);
            move = true;
        }

        for(WFPackageDescription wfPkg : wfFPDescr.getPackages()) {
            build(wfPkg);
        }

        if(move) {
            Path fpTarget = workDir.resolve(fpGroupId);
            fpTarget = fpTarget.resolve(fpArtifactId);
            fpTarget = fpTarget.resolve(fpVersion);
            mkdirs(fpTarget);
            try {
                Files.move(fpDir, fpTarget, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new InstallationDescriptionException("Failed to move feature pack dir " + fpDir.toAbsolutePath() + " to " + fpTarget.toAbsolutePath(), e);
            }
            fpDir = fpTarget;
        }
        fpBuilder.setGAV(new GAV(fpGroupId, fpArtifactId, fpVersion));
        FeaturePackDescription fpDescr = fpBuilder.build();
        installBuilder.addFeaturePack(fpDescr);

        try {
            FeaturePackXMLWriter.INSTANCE.write(fpDescr, fpDir.resolve(Constants.FEATURE_PACK_XML));
        } catch (XMLStreamException | IOException e) {
            throw new InstallationDescriptionException("Failed to write " + fpDir.resolve(Constants.FEATURE_PACK_XML).toAbsolutePath(), e);
        }

        fpBuilder = null;
        fpGroupId = null;
        fpArtifactId = null;
        fpVersion = null;
        fpDir = null;
    }

    void build(WFPackageDescription wfPkg) throws InstallationDescriptionException {

        final Path pkgDir = fpDir.resolve(Constants.PACKAGES).resolve(wfPkg.getName());
        mkdirs(pkgDir);

        pkgBuilder = PackageDescription.packageBuilder(wfPkg.getName());
        for(WFModulesDescription wfModules : wfPkg.getModules()) {
            build(wfModules);
        }

        for(String relativePath : wfPkg.getRelativePaths()) {
            final Path f = homeDir.resolve(relativePath);
            if(!Files.exists(f)) {
                throw new InstallationDescriptionException("Failed to locate " + f);
            }
            Path pkgContent = pkgDir.resolve(CONTENT);
            mkdirs(pkgContent);
            pkgContent = pkgContent.resolve(f.getFileName().toString());
            if(Files.isDirectory(f)) {
                mkdirs(pkgContent);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(f)) {
                    final Iterator<Path> children = stream.iterator();
                    if(children.hasNext()) {
                        Path child = children.next();
                        addContent(pkgBuilder, child, f.getFileName().toString(), pkgContent.resolve(child.getFileName().toString()));
                        while(children.hasNext()) {
                            child = children.next();
                            addContent(pkgBuilder, child, f.getFileName().toString(), pkgContent.resolve(child.getFileName().toString()));
                        }
                    } else {
                        mkdirs(pkgContent.resolve(f.getFileName().toString()));
                        pkgBuilder.addContentPath(relativePath);
                    }
                } catch(IOException e) {
                    failedToReadDirectory(f, e);
                }
            } else {
                copy(f, pkgContent);
                pkgBuilder.addContentPath(relativePath);
            }
        }
        for(String packageRef : wfPkg.getPackageRefs()) {
            pkgBuilder.addDependency(packageRef);
        }

        final PackageDescription pkgDescr = pkgBuilder.build();
        fpBuilder.addTopGroup(pkgDescr);

        writePackageXml(pkgDescr, pkgDir);

        pkgBuilder = null;
    }

    private void writePackageXml(final PackageDescription pkgDescr, final Path pkgDir) throws InstallationDescriptionException {
        try {
            PackageXMLWriter.INSTANCE.write(pkgDescr, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new InstallationDescriptionException("Failed to write " + pkgDir.resolve(Constants.PACKAGE_XML).toAbsolutePath(), e);
        }
    }

    void build(WFModulesDescription wfModules) throws InstallationDescriptionException {

        String relativePath = wfModules.getRelativeDir();
        if(relativePath != null) {
            modulesDir = homeDir.resolve(relativePath);
        } else {
            relativePath = modulesPath;
            modulesDir = WFFeaturePackLayoutBuilder.this.modulesDir;
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

        final Path moduleXml = dir.resolve(Constants.MODULES_XML);
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

        final String moduleName;
        final String relativePath;
        {
            final StringBuilder moduleNameBuf = new StringBuilder();
            final StringBuilder relativePathBuf = new StringBuilder(modulesPath).append('/');
            moduleNameBuf.append(path.get(0));
            relativePathBuf.append(path.get(0));
            for (int i = 1; i < path.size(); ++i) {
                final String part = path.get(i);
                moduleNameBuf.append('.').append(part);
                relativePathBuf.append('/').append(part);
            }
            moduleNameBuf.append('.').append(dir.getFileName().toString()); // adding the slot to the name (hibernate modules in wildfly)
            moduleName = moduleNameBuf.toString();
            relativePath = relativePathBuf.toString();
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

        final Path pkgDir = fpDir.resolve(Constants.PACKAGES).resolve(moduleName.toString());
        mkdirs(pkgDir);
        final PackageDescription.Builder moduleBuilder = PackageDescription.packageBuilder(moduleName.toString());
        addContent(moduleBuilder, dir, relativePath.toString(), pkgDir.resolve(CONTENT).resolve(relativePath.toString()));
        final PackageDescription pkgDescr = moduleBuilder.build();
        fpBuilder.addGroup(pkgDescr);
        pkgBuilder.addDependency(moduleName.toString());

        writePackageXml(pkgDescr, pkgDir);
    }

    private void addContent(PackageDescription.Builder builder, Path src, String relativePath, Path target) throws InstallationDescriptionException {
        if(Files.isDirectory(src)) {
            mkdirs(target);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
                for (Path entry: stream) {
                    addContent(builder, entry, relativePath + '/' + src.getFileName(), target.resolve(entry.getFileName().toString()));
                }
            } catch(IOException e) {
                failedToReadDirectory(src, e);
            }
        } else {
            copy(src, target);
            builder.addContentPath(relativePath + '/' + src.getFileName());
        }
    }

    private void copy(Path src, Path target) throws InstallationDescriptionException {
        try {
            Files.copy(src, target);
        } catch (IOException e) {
            throw new InstallationDescriptionException("Failed to copy " + src.toAbsolutePath() + " to " + target.toAbsolutePath());
        }
    }

    private void failedToReadDirectory(Path p, IOException e) throws InstallationDescriptionException {
        throw new InstallationDescriptionException("Failed to read directory " + p.toAbsolutePath(), e);
    }
}
