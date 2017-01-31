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

package org.jboss.provisioning.plugin.wildfly;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.wildfly.config.GeneratorConfig;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.util.LayoutUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class CliScriptCollector {

    private final ProvisioningContext ctx;
    private GeneratorConfig genConfig;

    List<Path> standaloneScripts = new ArrayList<>();
    List<Path> hcScripts = new ArrayList<>();

    private Map<ArtifactCoords.Gav, Set<String>> processedPackages = new HashMap<>();

    private ArtifactCoords.Gav lastLoggedGav;
    private String lastLoggedPackage;

    CliScriptCollector(ProvisioningContext ctx) {
        this.ctx = ctx;
    }

    void reset() {
        standaloneScripts.clear();
        hcScripts.clear();
        processedPackages.clear();
        genConfig = null;
        lastLoggedGav = null;
        lastLoggedPackage = null;
    }

    void collectScripts(GeneratorConfig genConfig, ProvisionedFeaturePack provisionedFp, String pkgName) throws ProvisioningException {
        System.out.println("Collecting configuration scripts for feature-pack " + provisionedFp.getGav() + " package " + pkgName);
        this.genConfig = genConfig;
        final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
        collectScripts(ctx.getLayoutDescription().getFeaturePack(fpGav.toGa()), pkgName, provisionedFp,
                getProcessedPackages(fpGav),
                LayoutUtils.getFeaturePackDir(ctx.getLayoutDir(), fpGav).resolve(Constants.PACKAGES));
    }

    private void collectScripts(FeaturePackSpec fpSpec, String pkgName, ProvisionedFeaturePack provisionedFp, Set<String> processedPackages, Path packagesDir) throws ProvisioningException {

        final PackageSpec pkgSpec = fpSpec.getPackage(pkgName);
        if(pkgSpec.hasExternalDependencies()) {
            for(String fpDep : pkgSpec.getExternalDependencyNames()) {
                final PackageDependencyGroupSpec externalDeps = pkgSpec.getExternalDependencies(fpDep);
                final ArtifactCoords.Gav externalGav = fpSpec.getDependency(externalDeps.getGroupName()).getTarget().getGav();
                final ProvisionedFeaturePack provisionedExternalFp = ctx.getProvisionedState().getFeaturePack(externalGav);
                final Set<String> externalProcessed = getProcessedPackages(externalGav);
                FeaturePackSpec externalFpSpec = null;
                Path externalPackagesDir = null;
                for(String depPkgName : externalDeps.getPackageNames()) {
                    if(provisionedExternalFp.containsPackage(depPkgName) && externalProcessed.add(depPkgName)) {
                        if(externalFpSpec == null) {
                            externalFpSpec = ctx.getLayoutDescription().getFeaturePack(externalGav.toGa());
                            externalPackagesDir = LayoutUtils.getFeaturePackDir(ctx.getLayoutDir(), externalGav).resolve(Constants.PACKAGES);
                        }
                        collectScripts(externalFpSpec, depPkgName, provisionedExternalFp, externalProcessed, externalPackagesDir);
                    }
                }
            }
        }

        if(pkgSpec.hasLocalDependencies()) {
            for(String depPkgName : pkgSpec.getLocalDependencies().getPackageNames()) {
                if(provisionedFp.containsPackage(depPkgName) && processedPackages.add(depPkgName)) {
                    collectScripts(fpSpec, depPkgName, provisionedFp, processedPackages, packagesDir);
                }
            }
        }

        final Path wfDir = packagesDir.resolve(pkgSpec.getName()).resolve(WfConstants.PM).resolve(WfConstants.WILDFLY);
        if(!Files.exists(wfDir)) {
            return;
        }

        // collect cli scripts
        if(genConfig.hasStandaloneConfig()) {
            for(String script : genConfig.getStandaloneConfig().getScripts()) {
                final Path scriptPath = wfDir.resolve(script);
                if(Files.exists(scriptPath)) {
                    standaloneScripts.add(scriptPath);
                    logScript(provisionedFp, pkgSpec.getName(), scriptPath);
                }
            }
        }
        if(genConfig.hasHostControllerConfig()) {
            for(String script : genConfig.getHostControllerConfig().getScripts()) {
                final Path scriptPath = wfDir.resolve(script);
                if(Files.exists(scriptPath)) {
                    hcScripts.add(scriptPath);
                    logScript(provisionedFp, pkgSpec.getName(), scriptPath);
                }
            }
        }
    }

    private Set<String> getProcessedPackages(ArtifactCoords.Gav fpGav) {
        Set<String> fpProcessed = processedPackages.get(fpGav);
        if(fpProcessed == null) {
            fpProcessed = new HashSet<>();
            processedPackages.put(fpGav, fpProcessed);
        }
        return fpProcessed;
    }

    public void logScript(final ProvisionedFeaturePack provisionedFp, String pkgName, Path script) {
        if(!provisionedFp.getGav().equals(lastLoggedGav)) {
            System.out.println("  " + provisionedFp.getGav());
            lastLoggedGav = provisionedFp.getGav();
            lastLoggedPackage = null;
        }
        if(!pkgName.equals(lastLoggedPackage)) {
            System.out.println("    " + pkgName);
            lastLoggedPackage = pkgName;
        }
        System.out.println("      - " + script.getFileName());
    }
}
