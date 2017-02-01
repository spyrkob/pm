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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.wildfly.core.launcher.CliCommandBuilder;

/**
 * Collects the CLI scripts from the packages and runs them to produce the configuration.
 *
 * @author Alexey Loubyansky
 */
class ConfigGenerator {

    private final ProvisioningContext ctx;

    private GeneratorConfig genConfig;
    private List<Path> standaloneScripts = new ArrayList<>();
    private List<Path> hcScripts = new ArrayList<>();
    private Map<ArtifactCoords.Gav, Set<String>> processedPackages = new HashMap<>();
    private ArtifactCoords.Gav lastLoggedGav;
    private String lastLoggedPackage;

    ConfigGenerator(ProvisioningContext ctx) {
        this.ctx = ctx;
    }

    private void reset() {
        standaloneScripts.clear();
        hcScripts.clear();
        processedPackages.clear();
        genConfig = null;
        lastLoggedGav = null;
        lastLoggedPackage = null;
    }

    void configure(final ProvisionedFeaturePack provisionedFp, String pkgName, final GeneratorConfig genConfig)
            throws ProvisioningException {

        this.genConfig = genConfig;
        final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
        System.out.println("Collecting configuration scripts for feature-pack " + provisionedFp.getGav() + " package " + pkgName);
        collectScripts(ctx.getLayoutDescription().getFeaturePack(fpGav.toGa()), pkgName, provisionedFp,
                getProcessedPackages(fpGav),
                LayoutUtils.getFeaturePackDir(ctx.getLayoutDir(), fpGav).resolve(Constants.PACKAGES));

        if (!standaloneScripts.isEmpty()) {
            System.out.println(" Generating " + genConfig.getStandaloneConfig().getServerConfig());
            runScripts("embed-server --empty-config --remove-existing --server-config=" + genConfig.getStandaloneConfig().getServerConfig(), standaloneScripts);
        }
        if (!hcScripts.isEmpty()) {
            System.out.println(" Generating " + genConfig.getHostControllerConfig().getDomainConfig() + " and " + genConfig.getHostControllerConfig().getHostConfig());
            runScripts(
                    "embed-host-controller --empty-host-config --empty-domain-config --remove-existing-host-config --remove-existing-domain-config --domain-config=" +
                    genConfig.getHostControllerConfig().getDomainConfig() +
                    " --host-config=" + genConfig.getHostControllerConfig().getHostConfig(),
                    hcScripts);
        }
        reset();
    }

    private void collectScripts(FeaturePackSpec fpSpec, String pkgName, ProvisionedFeaturePack provisionedFp,
            Set<String> processedPackages, Path packagesDir) throws ProvisioningException {

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

    private void logScript(final ProvisionedFeaturePack provisionedFp, String pkgName, Path script) {
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

    private void runScripts(final String embedCommand, List<Path> scripts) throws ProvisioningException {
        final List<String> commands = new ArrayList<>();
        commands.add(embedCommand);
        for (Path p : scripts) {
            commands.add("echo executing " + p);
            try {
                try(BufferedReader reader = Files.newBufferedReader(p)) {
                    String line = reader.readLine();
                    while(line != null) {
                        commands.add(line);
                        line = reader.readLine();
                    }
                }
            } catch (IOException e) {
                throw new ProvisioningException("Failed to read " + p);
            }
        }
        commands.add("exit");

        run(commands);
    }

    private void run(List<String> commands) throws ProvisioningException {
        CliCommandBuilder builder = CliCommandBuilder
                .of(ctx.getInstallDir())
                .setCommands(commands);

        final ProcessBuilder processBuilder = new ProcessBuilder(builder.build()).redirectErrorStream(true);
        processBuilder.environment().put("JBOSS_HOME", ctx.getInstallDir().toString());

        Process cliProcess;
        try {
            cliProcess = processBuilder.start();
            cliProcess.waitFor();
            if(cliProcess.exitValue() != 0) {
                final InputStream cliOutput = cliProcess.getInputStream();
                if(cliOutput == null) {
                    System.out.println("CLI output is not available");
                } else {
                    String echoLine = null;
                    int opIndex = 0;
                    final StringWriter errorWriter = new StringWriter();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(cliOutput));
                            BufferedWriter writer = new BufferedWriter(errorWriter)) {
                        String line = reader.readLine();
                        while (line != null) {
                            if(line.startsWith("executing ")) {
                                echoLine = line;
                                opIndex = 0;
                            } else {
                                if(line.equals("{")) {
                                    ++opIndex;
                                    writer.flush();
                                    errorWriter.getBuffer().setLength(0);
                                }
                                writer.write(line);
                                writer.newLine();
                            }
                            line = reader.readLine();
                        }
                    }

                    if(echoLine != null) {
                        Path p = Paths.get(echoLine.substring("executing ".length()));
                        final String scriptName = p.getFileName().toString();
                        p = p.getParent();
                        p = p.getParent();
                        p = p.getParent();
                        final String pkgName = p.getFileName().toString();
                        p = p.getParent();
                        p = p.getParent();
                        final String fpVersion = p.getFileName().toString();
                        p = p.getParent();
                        final String fpArtifact = p.getFileName().toString();
                        p = p.getParent();
                        final String fpGroup = p.getFileName().toString();
                        System.out.println("Failed to execute script " + scriptName +
                                " from " +  ArtifactCoords.newGav(fpGroup, fpArtifact, fpVersion) +
                                " package " + pkgName + " operation #" + opIndex);
                        System.out.println(errorWriter.getBuffer());
                    } else {
                        System.out.println("Could not locate the cause of the error in the CLI output.");
                        for(String line : commands) {
                            System.out.println(line);
                        }
                    }
                }
                throw new ProvisioningException("Embeedded CLI scripts failed.");
            }
        } catch (IOException | InterruptedException e) {
            throw new ProvisioningException("Embedded CLI process failed", e);
        }
    }
}
