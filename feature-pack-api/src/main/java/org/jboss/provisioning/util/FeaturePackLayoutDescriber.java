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
package org.jboss.provisioning.util;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.FeaturePackXmlParser;
import org.jboss.provisioning.xml.PackageXmlParser;

/**
 * Builds an installation description by analyzing the feature-pack layout
 * structure and parsing included XML files.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackLayoutDescriber {

    public static FeaturePackLayoutDescription describeLayout(Path fpLayout, String encoding) throws ProvisioningDescriptionException {
        if(!Files.exists(fpLayout)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpLayout));
        }
        if(!Files.isDirectory(fpLayout)) {
            throw new UnsupportedOperationException(); // TODO
        }

        final FeaturePackLayoutDescription.Builder installBuilder = FeaturePackLayoutDescription.builder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fpLayout)) {
            for(Path packageDir : stream) {
                processGroup(installBuilder, packageDir, encoding);
            }
        } catch (IOException e) {
            failedToReadDirectory(fpLayout, e);
        }
        return installBuilder.build();
    }

    private static void processGroup(final FeaturePackLayoutDescription.Builder installBuilder, Path pkgDir, String encoding) throws ProvisioningDescriptionException {
        assertDirectory(pkgDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgDir)) {
            for(Path artifactDir : stream) {
                processArtifact(installBuilder, artifactDir, encoding);
            }
        } catch (IOException e) {
            failedToReadDirectory(pkgDir, e);
        }
    }

    private static void processArtifact(final FeaturePackLayoutDescription.Builder installBuilder, Path artifactDir, String encoding) throws ProvisioningDescriptionException {
        assertDirectory(artifactDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(artifactDir)) {
            for(Path p : stream) {
                installBuilder.addFeaturePack(describeFeaturePack(p, encoding));
            }
        } catch (IOException e) {
            failedToReadDirectory(artifactDir, e);
        }
    }

    public static FeaturePackDescription describeFeaturePack(Path fpDir, String encoding) throws ProvisioningDescriptionException {
        assertDirectory(fpDir);
        final Path fpXml = fpDir.resolve(Constants.FEATURE_PACK_XML);
        if(!Files.exists(fpXml)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(fpXml));
        }
        final FeaturePackDescription.Builder fpBuilder = FeaturePackDescription.builder();
        final FeaturePackXmlParser fpXmlParser = new FeaturePackXmlParser();
        try (Reader is = Files.newBufferedReader(fpXml, Charset.forName(encoding))) {
            fpXmlParser.parse(is, fpBuilder);
        } catch (IOException e) {
            throw new ProvisioningDescriptionException(Errors.openFile(fpXml));
        } catch (XMLStreamException e) {
            throw new ProvisioningDescriptionException(Errors.parseXml(fpXml), e);
        }
        final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
        if(Files.exists(packagesDir)) {
            processPackages(fpBuilder, packagesDir, encoding);
        }
        return fpBuilder.build();
    }

    private static void processPackages(FeaturePackDescription.Builder fpBuilder, Path packagesDir, String encoding) throws ProvisioningDescriptionException {
        assertDirectory(packagesDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packagesDir)) {
            for(Path path : stream) {
                fpBuilder.addPackage(processPackage(path, encoding));
            }
        } catch (IOException e) {
            failedToReadDirectory(packagesDir, e);
        }
    }

    private static PackageDescription processPackage(Path pkgDir, String encoding) throws ProvisioningDescriptionException {
        assertDirectory(pkgDir);
        final Path pkgXml = pkgDir.resolve(Constants.PACKAGE_XML);
        if(!Files.exists(pkgXml)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(pkgXml));
        }
        final PackageXmlParser pkgParser = new PackageXmlParser();
        try (Reader in = Files.newBufferedReader(pkgXml, Charset.forName(encoding))) {
            return pkgParser.parse(in);
        } catch (IOException e) {
            throw new ProvisioningDescriptionException(Errors.openFile(pkgXml), e);
        } catch (XMLStreamException e) {
            throw new ProvisioningDescriptionException(Errors.parseXml(pkgXml), e);
        }
    }

    private static void assertDirectory(Path dir) throws ProvisioningDescriptionException {
        if(!Files.isDirectory(dir)) {
            throw new ProvisioningDescriptionException(Errors.notADir(dir));
        }
    }

    private static void failedToReadDirectory(Path p, IOException e) throws ProvisioningDescriptionException {
        throw new ProvisioningDescriptionException(Errors.readDirectory(p), e);
    }
}
