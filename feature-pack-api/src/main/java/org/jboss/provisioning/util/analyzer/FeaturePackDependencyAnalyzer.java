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

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.PMException;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;

/**
 * Analyzes feature pack layouts with the goal to identify dependencies between
 * feature packs.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyAnalyzer {

    public void analyze(Path fpLayoutDir, GAV gav1, GAV gav2) throws PMException {

        final Path fp1Path = getFeaturePackDir(fpLayoutDir, gav1);
        final Path fp2Path = getFeaturePackDir(fpLayoutDir, gav2);

        final FeaturePackDescription fp1Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp1Path);
        final FeaturePackDescription fp2Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp2Path);

        try {
            System.out.println(fp1Descr.logContent());
            System.out.println(fp2Descr.logContent());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Path getFeaturePackDir(Path fpLayoutDir, GAV gav) throws PMException {
        final Path fpPath = fpLayoutDir.resolve(gav.getGroupId()).resolve(gav.getArtifactId()).resolve(gav.getVersion());
        if(!Files.exists(fpPath)) {
            throw new PMException(Errors.pathDoesNotExist(fpPath));
        }
        return fpPath;
    }
}
