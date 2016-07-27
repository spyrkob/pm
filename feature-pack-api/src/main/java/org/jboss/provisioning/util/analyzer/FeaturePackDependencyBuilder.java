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

import java.io.StringWriter;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.FeaturePackDescription.Builder;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.xml.FeaturePackXMLWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyBuilder {

    public static void extractParentAsDependency(Path fpLayoutDir, GAV childGav, GAV parentGav) throws InstallationDescriptionException {
        final FeaturePacksDiff diffTool = FeaturePacksDiff.newInstance(fpLayoutDir, childGav, parentGav);
        final FeaturePackDescriptionDiffs diff = diffTool.compare();
        final FeaturePackSpecificDescription childDiff = diff.getFeaturePackDiff1();
        final FeaturePackSpecificDescription parentDiff = diff.getFeaturePackDiff2();

        final Builder fpBuilder = FeaturePackDescription.builder(childGav);

        // add dependency on the parent
        {
            final FeaturePackDependencyDescription.Builder depBuilder = FeaturePackDependencyDescription.builder(parentGav);
            if (parentDiff.hasUniquePackages()) {
                // exclude packages not found in the child
                depBuilder.excludeAllPackages(parentDiff.getUniquePackageNames());
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
                fpBuilder.addPackage(childDescr.getPackageDescription(name));
                if(childDescr.isTopPackage(name)) {
                    fpBuilder.addTopPackageName(name);
                }
            }
        }

        // add unique packages
        if(childDiff.hasUniquePackages()) {
            final FeaturePackDescription childDescr = diffTool.getFeaturePackDescription1();
            for(String name : childDiff.getUniquePackageNames()) {
                fpBuilder.addPackage(childDiff.getUniquePackage(name));
                if(childDescr.isTopPackage(name)) {
                    fpBuilder.addTopPackageName(name);
                }
            }
        }

        final StringWriter writer = new StringWriter();
        try {
            FeaturePackXMLWriter.INSTANCE.write(fpBuilder.build(), writer);
        } catch (XMLStreamException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(writer.getBuffer().toString());
    }
}
