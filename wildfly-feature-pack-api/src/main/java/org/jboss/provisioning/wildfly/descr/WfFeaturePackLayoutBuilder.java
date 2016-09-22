/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.provisioning.wildfly.descr;

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

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescriptionBuilder;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.FeaturePackDescription.Builder;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;
import org.jboss.provisioning.xml.PackageXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class WfFeaturePackLayoutBuilder {

    private static final List<String> PRODUCT_MODULE = Arrays.asList("org", "jboss", "as", "product");
    //private static final String RELEASE_NAME = "JBoss-Product-Release-Name";
    private static final String RELEASE_VERSION = "JBoss-Product-Release-Version";

    private static final String CONTENT = "content";

    private Path workDir;
    private Path homeDir;
    private String modulesPath;
    private Path modulesDir;
    private FeaturePackLayoutDescriptionBuilder installBuilder;

    private String fpGroupId;
    private String fpArtifactId;
    private String fpVersion;
    private Builder fpBuilder;
    private Path fpDir;

    private PackageDescription.Builder pkgBuilder;

    public FeaturePackLayoutDescription build(WfInstallationDescription wfDescr, Path homeDir, Path workDir) throws ProvisioningDescriptionException {
        this.workDir = workDir;
        mkdirs(workDir);
        this.homeDir = homeDir;
        modulesPath = wfDescr.getModulesPath();
        modulesDir = homeDir.resolve(modulesPath);
        installBuilder = FeaturePackLayoutDescriptionBuilder.newInstance();
        for(WfFeaturePackDescription fpDescr : wfDescr.getFeaturePacks()) {
            build(fpDescr);
        }
        return installBuilder.build();
    }

    private void mkdirs(Path dir) throws ProvisioningDescriptionException {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ProvisioningDescriptionException(Errors.mkdirs(dir.toAbsolutePath()), e);
        }
    }

    void build(WfFeaturePackDescription wfFPDescr) throws ProvisioningDescriptionException {
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

        for(WfPackageDescription wfPkg : wfFPDescr.getPackages()) {
            build(wfPkg);
        }

        if(move) {
            if(fpGroupId == null) {
                throw new ProvisioningDescriptionException(Errors.missingParameter("group-id"));
            }
            if(fpArtifactId == null) {
                throw new ProvisioningDescriptionException(Errors.missingParameter("artifact-id"));
            }
            if(fpVersion == null) {
                throw new ProvisioningDescriptionException(Errors.missingParameter("version"));
            }
            Path fpTarget = workDir.resolve(fpGroupId);
            fpTarget = fpTarget.resolve(fpArtifactId);
            fpTarget = fpTarget.resolve(fpVersion);
            mkdirs(fpTarget);
            try {
                Files.move(fpDir, fpTarget, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new ProvisioningDescriptionException(Errors.moveFile(fpDir, fpTarget), e);
            }
            fpDir = fpTarget;
        }
        fpBuilder.setGAV(new Gav(fpGroupId, fpArtifactId, fpVersion));
        fpBuilder.addProvisioningPlugin(new Gav("org.jboss.pm", "wildfly-feature-pack-maven-plugin", "1.0.0.Alpha-SNAPSHOT"));
        installBuilder.addFeaturePack(fpBuilder.build());

        try {
            FeaturePackXmlWriter.INSTANCE.write(fpBuilder.build(), fpDir.resolve(Constants.FEATURE_PACK_XML));
        } catch (XMLStreamException | IOException e) {
            throw new ProvisioningDescriptionException(Errors.writeXml(fpDir.resolve(Constants.FEATURE_PACK_XML).toAbsolutePath()), e);
        }

        fpBuilder = null;
        fpGroupId = null;
        fpArtifactId = null;
        fpVersion = null;
        fpDir = null;
    }

    void build(WfPackageDescription wfPkg) throws ProvisioningDescriptionException {

        pkgBuilder = PackageDescription.builder(wfPkg.getName());
        for (WfModulesDescription wfModules : wfPkg.getModules()) {
            build(wfModules);
        }

        final Path pkgDir = fpDir.resolve(Constants.PACKAGES).resolve(wfPkg.getName());

        for (String relativePath : wfPkg.getRelativePaths()) {
            final Path f = homeDir.resolve(relativePath);
            if (!Files.exists(f)) {
                if(wfPkg.isOptional()) {
                    pkgBuilder = null;
                    return;
                }
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(f));
            }
            Path pkgContent = pkgDir.resolve(CONTENT);
            mkdirs(pkgContent);
            pkgContent = pkgContent.resolve(f.getFileName().toString());
            if (Files.isDirectory(f)) {
                mkdirs(pkgContent);
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(f)) {
                    final Iterator<Path> children = stream.iterator();
                    if (children.hasNext()) {
                        Path child = children.next();
                        copy(child, pkgContent.resolve(child.getFileName().toString()));
                        while (children.hasNext()) {
                            child = children.next();
                            copy(child, pkgContent.resolve(child.getFileName().toString()));
                        }
                    } else {
                        mkdirs(pkgContent.resolve(f.getFileName().toString()));
                    }
                } catch (IOException e) {
                    failedToReadDirectory(f, e);
                }
            } else {
                copy(f, pkgContent);
            }
        }
        for (String packageRef : wfPkg.getPackageRefs()) {
            pkgBuilder.addDependency(packageRef);
        }

        final PackageDescription pkgDescr = pkgBuilder.build();
        fpBuilder.addTopPackage(pkgDescr);

        writePackageXml(pkgDescr, pkgDir);
        pkgBuilder = null;
    }

    private void writePackageXml(final PackageDescription pkgDescr, final Path pkgDir) throws ProvisioningDescriptionException {
        if(!Files.exists(pkgDir)) {
            mkdirs(pkgDir);
        }
        try {
            PackageXmlWriter.INSTANCE.write(pkgDescr, pkgDir.resolve(Constants.PACKAGE_XML));
        } catch (XMLStreamException | IOException e) {
            throw new ProvisioningDescriptionException(Errors.writeXml(pkgDir.resolve(Constants.PACKAGE_XML).toAbsolutePath()), e);
        }
    }

    void build(WfModulesDescription wfModules) throws ProvisioningDescriptionException {

        String relativePath = wfModules.getRelativeDir();
        if(relativePath != null) {
            modulesDir = homeDir.resolve(relativePath);
        } else {
            relativePath = modulesPath;
            modulesDir = WfFeaturePackLayoutBuilder.this.modulesDir;
        }
        if(wfModules.getNames().isEmpty()) {
            if(!Files.exists(modulesDir)) {
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(modulesDir.toAbsolutePath()));
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

    private void processModules(String modulesPath, List<String> path, Path dir) throws ProvisioningDescriptionException {

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
            relativePathBuf.append('/').append(dir.getFileName().toString());
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
                throw new ProvisioningDescriptionException(Errors.readFile(manifest.toAbsolutePath()), e);
            }
            fpVersion = props.getProperty(RELEASE_VERSION);
        }

        final Path pkgDir = fpDir.resolve(Constants.PACKAGES).resolve(moduleName.toString());
        mkdirs(pkgDir);
        final PackageDescription.Builder moduleBuilder = PackageDescription.builder(moduleName.toString());
        copy(dir, pkgDir.resolve(CONTENT).resolve(relativePath.toString()));
        final PackageDescription pkgDescr = moduleBuilder.build();
        fpBuilder.addPackage(pkgDescr);
        pkgBuilder.addDependency(moduleName.toString());

        writePackageXml(pkgDescr, pkgDir);
    }

    private void copy(Path src, Path target) throws ProvisioningDescriptionException {
        if(!Files.exists(target.getParent())) {
            mkdirs(target.getParent());
        }
        try {
            IoUtils.copy(src, target);
        } catch (IOException e) {
            throw new ProvisioningDescriptionException(Errors.copyFile(src.toAbsolutePath(), target.toAbsolutePath()));
        }
    }

    private void failedToReadDirectory(Path p, IOException e) throws ProvisioningDescriptionException {
        throw new ProvisioningDescriptionException(Errors.readDirectory(p.toAbsolutePath()), e);
    }
}
