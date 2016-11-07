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

package org.jboss.provisioning.test.util.repomanager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.ZipUtils;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackBuilder {

    static Path getFeaturePackArtifactPath(Path repoHome, final Gav gav) {
        Path p = repoHome;
        final String[] groupParts = gav.getGroupId().split("\\.");
        for (String part : groupParts) {
            p = p.resolve(part);
        }
        p = p.resolve(gav.getArtifactId());
        p = p.resolve(gav.getVersion());
        final StringBuilder fileName = new StringBuilder();
        fileName.append(gav.getArtifactId()).append('-').append(gav.getVersion()).append(".zip");
        return p.resolve(fileName.toString());
    }

    public static FeaturePackBuilder newInstance() {
        return newInstance(null);
    }

    public static FeaturePackBuilder newInstance(FeaturePackRepoManager.Installer installer) {
        return new FeaturePackBuilder(installer);
    }

    private final FeaturePackRepoManager.Installer installer;
    private final FeaturePackSpec.Builder fpBuilder = FeaturePackSpec.builder();
    private List<PackageBuilder> pkgs = Collections.emptyList();

    protected FeaturePackBuilder(FeaturePackRepoManager.Installer repo) {
        this.installer = repo;
    }

    public FeaturePackRepoManager.Installer getInstaller() {
        return installer;
    }

    public FeaturePackBuilder setGav(ArtifactCoords.Gav gav) {
        fpBuilder.setGav(gav);
        return this;
    }

    public FeaturePackBuilder addDependency(String name, FeaturePackConfig dep) throws ProvisioningDescriptionException {
        fpBuilder.addDependency(name, dep);
        return this;
    }

    public FeaturePackBuilder addDependency(FeaturePackConfig dep) throws ProvisioningDescriptionException {
        return addDependency(null, dep);
    }

    public FeaturePackBuilder addDependency(ArtifactCoords.Gav gav) throws ProvisioningDescriptionException {
        return addDependency(FeaturePackConfig.forGav(gav));
    }

    public FeaturePackBuilder addPackage(PackageBuilder pkg) {
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

    public PackageBuilder newPackage() {
        return newPackage(null);
    }

    public PackageBuilder newPackage(String name) {
        return newPackage(name, false);
    }

    public PackageBuilder newPackage(String name, boolean isDefault) {
        final PackageBuilder pkg = PackageBuilder.newInstance(this);
        if(name != null) {
            pkg.setName(name);
        }
        if(isDefault) {
            pkg.setDefault();
        }
        addPackage(pkg);
        return pkg;
    }

    public FeaturePackSpec build(Path repoHome) {
        final Path fpWorkDir = TestUtils.mkRandomTmpDir();
        final FeaturePackSpec fpSpec;
        try {
            for (PackageBuilder pkg : pkgs) {
                final PackageSpec pkgDescr = pkg.build(fpWorkDir);
                if(pkg.isDefault()) {
                    fpBuilder.addDefaultPackage(pkgDescr);
                } else {
                    fpBuilder.addPackage(pkgDescr);
                }
            }
            fpSpec = fpBuilder.build();
            final FeaturePackXmlWriter writer = FeaturePackXmlWriter.getInstance();
            writer.write(fpSpec, fpWorkDir.resolve(Constants.FEATURE_PACK_XML));

            final Path fpZip;
            fpZip = getFeaturePackArtifactPath(repoHome, fpSpec.getGav());
            TestUtils.mkdirs(fpZip.getParent());
            ZipUtils.zip(fpWorkDir, fpZip);
            return fpSpec;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            IoUtils.recursiveDelete(fpWorkDir);
        }
    }
}
