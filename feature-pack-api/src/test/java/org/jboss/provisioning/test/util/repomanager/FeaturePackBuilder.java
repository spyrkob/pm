/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
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

    static Path getArtifactPath(Path repoHome, final ArtifactCoords coords) {
        Path p = repoHome;
        final String[] groupParts = coords.getGroupId().split("\\.");
        for (String part : groupParts) {
            p = p.resolve(part);
        }
        p = p.resolve(coords.getArtifactId());
        p = p.resolve(coords.getVersion());
        final StringBuilder fileName = new StringBuilder();
        fileName.append(coords.getArtifactId()).append('-').append(coords.getVersion()).append('.').append(coords.getExtension());
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
    private Set<ArtifactCoords> plugins = Collections.emptySet();

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

    public FeaturePackBuilder addDependency(String name, ArtifactCoords.Gav gav) throws ProvisioningDescriptionException {
        return addDependency(name, FeaturePackConfig.forGav(gav));
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

    public PackageBuilder newPackage(String name) {
        return newPackage(name, false);
    }

    public PackageBuilder newPackage(String name, boolean isDefault) {
        final PackageBuilder pkg = PackageBuilder.newInstance(this, name);
        if(isDefault) {
            pkg.setDefault();
        }
        addPackage(pkg);
        return pkg;
    }

    public FeaturePackBuilder addPlugIn(String coords) {
        return addPlugIn(ArtifactCoords.fromString(coords));
    }

    public FeaturePackBuilder addPlugIn(ArtifactCoords coords) {
        switch (plugins.size()) {
            case 0:
                plugins = Collections.singleton(coords);
                break;
            case 1:
                plugins = new LinkedHashSet<>(plugins);
            default:
                plugins.add(coords);
        }
        return this;
    }

    public FeaturePackSpec build(Path repoHome) throws ProvisioningDescriptionException {
        final Path fpWorkDir = TestUtils.mkRandomTmpDir();
        final FeaturePackSpec fpSpec;
        try {
            for (PackageBuilder pkg : pkgs) {
                final PackageSpec pkgDescr = pkg.build(fpWorkDir);
                if(pkg.isDefault()) {
                    fpBuilder.addDefaultPackage(pkgDescr.getName());
                }
            }
            if(!plugins.isEmpty()) {
                for(ArtifactCoords coords : plugins) {
                    fpBuilder.addProvisioningPlugin(coords);
                }
            }
            fpSpec = fpBuilder.build();
            final FeaturePackXmlWriter writer = FeaturePackXmlWriter.getInstance();
            writer.write(fpSpec, fpWorkDir.resolve(Constants.FEATURE_PACK_XML));

            final Path fpZip;
            fpZip = getArtifactPath(repoHome, fpSpec.getGav().toArtifactCoords());
            TestUtils.mkdirs(fpZip.getParent());
            ZipUtils.zip(fpWorkDir, fpZip);
            return fpSpec;
        } catch(ProvisioningDescriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            IoUtils.recursiveDelete(fpWorkDir);
        }
    }
}
