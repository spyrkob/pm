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

package org.jboss.provisioning.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.InstallationDescription;
import org.jboss.provisioning.descr.InstallationDescriptionBuilder;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.FeaturePackXMLParser;
import org.jboss.provisioning.xml.PackageXMLParser;

/**
 * Builds an installation description by analyzing the feature-pack layout
 * structure and parsing included XML files.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutDescriber {

    public static InstallationDescription describeLayout(Path fpLayout) throws InstallationDescriptionException {
        if(!Files.exists(fpLayout)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(fpLayout));
        }
        if(!Files.isDirectory(fpLayout)) {
            throw new UnsupportedOperationException(); // TODO
        }

        final InstallationDescriptionBuilder installBuilder = InstallationDescriptionBuilder.newInstance();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fpLayout)) {
            for(Path packageDir : stream) {
                processGroup(installBuilder, packageDir);
            }
        } catch (IOException e) {
            failedToReadDirectory(fpLayout, e);
        }
        return installBuilder.build();
    }

    private static void processGroup(final InstallationDescriptionBuilder installBuilder, Path pkgDir) throws InstallationDescriptionException {
        assertDirectory(pkgDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgDir)) {
            for(Path artifactDir : stream) {
                processArtifact(installBuilder, artifactDir);
            }
        } catch (IOException e) {
            failedToReadDirectory(pkgDir, e);
        }
    }

    private static void processArtifact(final InstallationDescriptionBuilder installBuilder, Path artifactDir) throws InstallationDescriptionException {
        assertDirectory(artifactDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(artifactDir)) {
            for(Path p : stream) {
                installBuilder.addFeaturePack(describeFeaturePack(p));
            }
        } catch (IOException e) {
            failedToReadDirectory(artifactDir, e);
        }
    }

    public static FeaturePackDescription describeFeaturePack(Path fpDir) throws InstallationDescriptionException {
        assertDirectory(fpDir);
        final Path fpXml = fpDir.resolve(Constants.FEATURE_PACK_XML);
        if(!Files.exists(fpXml)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(fpXml));
        }
        final FeaturePackDescription.Builder fpBuilder = FeaturePackDescription.builder();
        final FeaturePackXMLParser fpXmlParser = new FeaturePackXMLParser();
        try (InputStream is = Files.newInputStream(fpXml)) {
            fpXmlParser.parse(is, fpBuilder);
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.openFile(fpXml));
        } catch (XMLStreamException e) {
            throw new InstallationDescriptionException(Errors.parseXml(fpXml), e);
        }
        final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
        if(Files.exists(packagesDir)) {
            processPackages(fpBuilder, packagesDir);
        }
        return fpBuilder.build();
    }

    private static void processPackages(FeaturePackDescription.Builder fpBuilder, Path packagesDir) throws InstallationDescriptionException {
        assertDirectory(packagesDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packagesDir)) {
            for(Path path : stream) {
                fpBuilder.addPackage(processPackage(path));
            }
        } catch (IOException e) {
            failedToReadDirectory(packagesDir, e);
        }
    }

    private static PackageDescription processPackage(Path pkgDir) throws InstallationDescriptionException {
        assertDirectory(pkgDir);
        final Path pkgXml = pkgDir.resolve(Constants.PACKAGE_XML);
        if(!Files.exists(pkgXml)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(pkgXml));
        }
        final PackageXMLParser pkgParser = new PackageXMLParser();
        try (InputStream in = Files.newInputStream(pkgXml)) {
            return pkgParser.parse(in);
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.openFile(pkgXml), e);
        } catch (XMLStreamException e) {
            throw new InstallationDescriptionException(Errors.parseXml(pkgXml), e);
        }
    }

    private static void assertDirectory(Path dir) throws InstallationDescriptionException {
        if(!Files.isDirectory(dir)) {
            throw new InstallationDescriptionException(Errors.notADir(dir));
        }
    }

    private static void failedToReadDirectory(Path p, IOException e) throws InstallationDescriptionException {
        throw new InstallationDescriptionException(Errors.readDirectory(p), e);
    }
}
