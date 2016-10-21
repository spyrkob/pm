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
package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackDescription.Builder;
import org.jboss.provisioning.descr.PackageDependencyDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;
import org.jboss.provisioning.xml.PackageXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyBuilder {

    public static void extractParentAsDependency(Path fpLayoutDir, String encoding, ArtifactCoords.Gav childGav, ArtifactCoords.Gav parentGav) throws ProvisioningDescriptionException {
        new FeaturePackDependencyBuilder(fpLayoutDir, encoding, childGav, parentGav, true).extractParentAsDependency();
    }

    public static FeaturePackDescription describeParentAsDependency(Path fpLayoutDir, String encoding, ArtifactCoords.Gav childGav, ArtifactCoords.Gav parentGav) throws ProvisioningDescriptionException {
        return new FeaturePackDependencyBuilder(fpLayoutDir, encoding, childGav, parentGav, false).describeParentAsDependency();
    }

    private final Path fpLayoutDir;
    private final String encoding;
    private final ArtifactCoords.Gav childGav;
    private final ArtifactCoords.Gav parentGav;
    private final boolean updateXml;


    private FeaturePackDependencyBuilder(Path fpLayoutDir, String encoding, ArtifactCoords.Gav childGav, ArtifactCoords.Gav parentGav, boolean updateXml) {
        this.fpLayoutDir = fpLayoutDir;
        this.encoding = encoding;
        this.childGav = childGav;
        this.parentGav = parentGav;
        this.updateXml = updateXml;
    }

    private void extractParentAsDependency() throws ProvisioningDescriptionException {
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, childGav);
        final FeaturePackDescription originalDescr = FeaturePackLayoutDescriber.describeFeaturePack(fpDir, encoding);
        final FeaturePackDescription newDescr = describeParentAsDependency();

        final Path featurePackXml = fpDir.resolve(Constants.FEATURE_PACK_XML);
        if(!Files.exists(featurePackXml)) {
            throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(featurePackXml));
        }
        try {
            FeaturePackXmlWriter.INSTANCE.write(newDescr, featurePackXml);
        } catch (XMLStreamException | IOException e) {
            throw new ProvisioningDescriptionException(Errors.writeXml(featurePackXml), e);
        }
        for(String name : originalDescr.getPackageNames()) {
            if(!newDescr.hasPackage(name)) {
                IoUtils.recursiveDelete(LayoutUtils.getPackageDir(fpDir, name));
            }
        }
    }

    private FeaturePackDescription describeParentAsDependency() throws ProvisioningDescriptionException {
        final FeaturePacksDiff diffTool = FeaturePacksDiff.newInstance(fpLayoutDir, encoding, childGav, parentGav);
        final FeaturePackDescriptionDiffs diff = diffTool.compare();
        final FeaturePackSpecificDescription childDiff = diff.getFeaturePackDiff1();
        final FeaturePackSpecificDescription parentDiff = diff.getFeaturePackDiff2();

        final Builder fpBuilder = FeaturePackDescription.builder(childGav);

        // add dependency on the parent
        {
            final ProvisionedFeaturePackDescription.Builder depBuilder = ProvisionedFeaturePackDescription.builder(parentGav);
            if (parentDiff.hasUniquePackages()) {
                // exclude packages not found in the child
                try {
                    depBuilder.excludeAllPackages(parentDiff.getUniquePackageNames());
                } catch (ProvisioningException e) {
                    throw new ProvisioningDescriptionException("Failed to exlcude packages", e);
                }
            }
            fpBuilder.addDependency(depBuilder.build());
        }

        // add dependencies not covered by the parent
        if(childDiff.hasDependencies()) {
            fpBuilder.addAllDependencies(childDiff.getDependencies());
        }

        // override parent packages
        if(childDiff.hasConflictingPackages()) {
            final FeaturePackDescription childDescr = diffTool.getFeaturePackDescription1();
            for(String name : childDiff.getConflictingPackageNames()) {
                final PackageDescription pkgDescr = updatePackageDependencies(childDiff, childDescr.getPackageDescription(name));
                fpBuilder.addPackage(pkgDescr);
                if(childDescr.isDefaultPackage(name)) {
                    fpBuilder.markAsDefaultPackage(name);
                }
            }
        }

        // add unique packages
        if(childDiff.hasUniquePackages()) {
            final FeaturePackDescription childDescr = diffTool.getFeaturePackDescription1();
            for(String name : childDiff.getUniquePackageNames()) {
                final PackageDescription pkgDescr = updatePackageDependencies(childDiff, childDiff.getUniquePackage(name));
                fpBuilder.addPackage(pkgDescr);
                if(childDescr.isDefaultPackage(name)) {
                    fpBuilder.markAsDefaultPackage(name);
                }
            }
        }

        return fpBuilder.build();
    }

    private PackageDescription updatePackageDependencies(
            final FeaturePackSpecificDescription childDiff,
            PackageDescription pkgDescr) throws ProvisioningDescriptionException {
        if(pkgDescr.hasDependencies()) {
            final PackageDescription.Builder pkgBuilder = PackageDescription.builder(pkgDescr.getName());
            for(PackageDependencyDescription dep : pkgDescr.getDependencies()) {
                if(!childDiff.isMatchedPackage(dep.getName())) {
                    pkgBuilder.addDependency(dep.getName(), dep.isOptional());
                }
            }
            pkgDescr = pkgBuilder.build();
            if(updateXml) {
                final Path packageXml = LayoutUtils.getPackageDir(LayoutUtils.getFeaturePackDir(fpLayoutDir, childGav), pkgDescr.getName()).resolve(Constants.PACKAGE_XML);
                if(!Files.exists(packageXml)) {
                    throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(packageXml));
                }
                try {
                    IoUtils.recursiveDelete(packageXml);
                    PackageXmlWriter.INSTANCE.write(pkgDescr, packageXml);
                } catch (XMLStreamException | IOException e) {
                    throw new ProvisioningDescriptionException(Errors.writeXml(packageXml));
                }
            }
        }
        return pkgDescr;
    }
}
