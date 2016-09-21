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

package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackDescription.Builder;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.FeaturePackXMLWriter;
import org.jboss.provisioning.xml.PackageXMLWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyBuilder {

    public static void extractParentAsDependency(Path fpLayoutDir, String encoding, GAV childGav, GAV parentGav) throws InstallationDescriptionException {
        new FeaturePackDependencyBuilder(fpLayoutDir, encoding, childGav, parentGav, true).extractParentAsDependency();
    }

    public static FeaturePackDescription describeParentAsDependency(Path fpLayoutDir, String encoding, GAV childGav, GAV parentGav) throws InstallationDescriptionException {
        return new FeaturePackDependencyBuilder(fpLayoutDir, encoding, childGav, parentGav, false).describeParentAsDependency();
    }

    private final Path fpLayoutDir;
    private final String encoding;
    private final GAV childGav;
    private final GAV parentGav;
    private final boolean updateXml;


    private FeaturePackDependencyBuilder(Path fpLayoutDir, String encoding, GAV childGav, GAV parentGav, boolean updateXml) {
        this.fpLayoutDir = fpLayoutDir;
        this.encoding = encoding;
        this.childGav = childGav;
        this.parentGav = parentGav;
        this.updateXml = updateXml;
    }

    private void extractParentAsDependency() throws InstallationDescriptionException {
        final Path fpDir = LayoutUtils.getFeaturePackDir(fpLayoutDir, childGav);
        final FeaturePackDescription originalDescr = FeaturePackLayoutDescriber.describeFeaturePack(fpDir, encoding);
        final FeaturePackDescription newDescr = describeParentAsDependency();

        final Path featurePackXml = fpDir.resolve(Constants.FEATURE_PACK_XML);
        if(!Files.exists(featurePackXml)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(featurePackXml));
        }
        try {
            FeaturePackXMLWriter.INSTANCE.write(newDescr, featurePackXml);
        } catch (XMLStreamException | IOException e) {
            throw new InstallationDescriptionException(Errors.writeXml(featurePackXml), e);
        }
        for(String name : originalDescr.getPackageNames()) {
            if(!newDescr.hasPackage(name)) {
                IoUtils.recursiveDelete(LayoutUtils.getPackageDir(fpDir, name));
            }
        }
    }

    private FeaturePackDescription describeParentAsDependency() throws InstallationDescriptionException {
        final FeaturePacksDiff diffTool = FeaturePacksDiff.newInstance(fpLayoutDir, encoding, childGav, parentGav);
        final FeaturePackDescriptionDiffs diff = diffTool.compare();
        final FeaturePackSpecificDescription childDiff = diff.getFeaturePackDiff1();
        final FeaturePackSpecificDescription parentDiff = diff.getFeaturePackDiff2();

        final Builder fpBuilder = FeaturePackDescription.builder(childGav);

        // add dependency on the parent
        {
            final ProvisionedFeaturePackDescription.Builder depBuilder = ProvisionedFeaturePackDescription.builder().setGAV(parentGav);
            if (parentDiff.hasUniquePackages()) {
                // exclude packages not found in the child
                try {
                    depBuilder.excludeAllPackages(parentDiff.getUniquePackageNames());
                } catch (ProvisioningException e) {
                    throw new InstallationDescriptionException("Failed to exlcude packages", e);
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
                if(childDescr.isTopPackage(name)) {
                    fpBuilder.addTopPackageName(name);
                }
            }
        }

        // add unique packages
        if(childDiff.hasUniquePackages()) {
            final FeaturePackDescription childDescr = diffTool.getFeaturePackDescription1();
            for(String name : childDiff.getUniquePackageNames()) {
                final PackageDescription pkgDescr = updatePackageDependencies(childDiff, childDiff.getUniquePackage(name));
                fpBuilder.addPackage(pkgDescr);
                if(childDescr.isTopPackage(name)) {
                    fpBuilder.addTopPackageName(name);
                }
            }
        }

        return fpBuilder.build();
    }

    private PackageDescription updatePackageDependencies(
            final FeaturePackSpecificDescription childDiff,
            PackageDescription pkgDescr) throws InstallationDescriptionException {
        if(pkgDescr.hasDependencies()) {
            final PackageDescription.Builder pkgBuilder = PackageDescription.builder(pkgDescr.getName());
            for(String dep : pkgDescr.getDependencies()) {
                if(!childDiff.isMatchedPackage(dep)) {
                    pkgBuilder.addDependency(dep);
                }
            }
            pkgDescr = pkgBuilder.build();
            if(updateXml) {
                final Path packageXml = LayoutUtils.getPackageDir(LayoutUtils.getFeaturePackDir(fpLayoutDir, childGav), pkgDescr.getName()).resolve(Constants.PACKAGE_XML);
                if(!Files.exists(packageXml)) {
                    throw new InstallationDescriptionException(Errors.pathDoesNotExist(packageXml));
                }
                try {
                    IoUtils.recursiveDelete(packageXml);
                    PackageXMLWriter.INSTANCE.write(pkgDescr, packageXml);
                } catch (XMLStreamException | IOException e) {
                    throw new InstallationDescriptionException(Errors.writeXml(packageXml));
                }
            }
        }
        return pkgDescr;
    }
}
