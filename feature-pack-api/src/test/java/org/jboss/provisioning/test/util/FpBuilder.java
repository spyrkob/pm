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

package org.jboss.provisioning.test.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FpBuilder {

    public static FpBuilder newInstance() {
        return newInstance(null);
    }

    public static FpBuilder newInstance(FpRepoBuilder repo) {
        return new FpBuilder(repo);
    }

    private final FpRepoBuilder repo;
    private final FeaturePackDescription.Builder fpBuilder = FeaturePackDescription.builder();
    private List<PkgBuilder> pkgs = Collections.emptyList();

    protected FpBuilder(FpRepoBuilder repo) {
        this.repo = repo;
    }

    public FpRepoBuilder getRepo() {
        return repo;
    }

    public FpBuilder setGav(ArtifactCoords.Gav gav) {
        fpBuilder.setGav(gav);
        return this;
    }

    public FpBuilder addDependency(ProvisionedFeaturePackDescription dep) {
        fpBuilder.addDependency(dep);
        return this;
    }

    public FpBuilder addDependency(ArtifactCoords.Gav gav) {
        return addDependency(ProvisionedFeaturePackDescription.builder().setGav(gav).build());
    }

    public FpBuilder addPackage(PkgBuilder pkg) {
        switch (pkgs.size()) {
            case 0:
                pkgs = Collections.singletonList(pkg);
                break;
            case 1:
                pkgs = new ArrayList<>(pkgs);
            default:
                pkgs.add(pkg);
        }
        return this;
    }

    public PkgBuilder newPackage() {
        return newPackage(null);
    }

    public PkgBuilder newPackage(String name) {
        return newPackage(name, false);
    }

    public PkgBuilder newPackage(String name, boolean isDefault) {
        final PkgBuilder pkg = PkgBuilder.newInstance(this);
        if(name != null) {
            pkg.setName(name);
        }
        if(isDefault) {
            pkg.setDefault();
        }
        addPackage(pkg);
        return pkg;
    }

    void write(Path repoHome) {
        final Path fpWorkDir = TestFiles.mkRandomTmpDir();
        final FeaturePackDescription fpDescr;
        try {
            for (PkgBuilder pkg : pkgs) {
                final PackageDescription pkgDescr = pkg.write(fpWorkDir);
                if(pkg.isDefault()) {
                    fpBuilder.addDefaultPackage(pkgDescr);
                } else {
                    fpBuilder.addPackage(pkgDescr);
                }
            }
            fpDescr = fpBuilder.build();
            final FeaturePackXmlWriter writer = FeaturePackXmlWriter.INSTANCE;
            writer.write(fpDescr, fpWorkDir.resolve(Constants.FEATURE_PACK_XML));

            final Path fpZip;
            {
                Path p = repoHome;
                final String[] groupParts = fpDescr.getGav().getGroupId().split("\\.");
                for (String part : groupParts) {
                    p = p.resolve(part);
                }
                p = p.resolve(fpDescr.getGav().getArtifactId());
                p = p.resolve(fpDescr.getGav().getVersion());
                final StringBuilder fileName = new StringBuilder();
                fileName.append(fpDescr.getGav().getArtifactId()).append('-').append(fpDescr.getGav().getVersion()).append(".zip");
                p = p.resolve(fileName.toString());
                fpZip = p;
            }
            TestFiles.mkdirs(fpZip.getParent());
            ZipUtils.zip(fpWorkDir, fpZip);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            IoUtils.recursiveDelete(fpWorkDir);
        }
    }
}
