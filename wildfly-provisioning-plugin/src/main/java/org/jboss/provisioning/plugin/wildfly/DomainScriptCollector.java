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
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningContext;
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
class DomainScriptCollector {

    private static final String DOMAIN_CLI = "domain.cli";
    private static final String HOST_CLI = "host.cli";
    private static final String MAIN_CLI = "main.cli";
    private static final String PROFILE_CLI = "profile.cli";

    private static final String[] NON_PROFILE = new String[]{MAIN_CLI, DOMAIN_CLI};

    private final ProvisioningContext ctx;

    private final Path script;
    private BufferedWriter scriptWriter;

    private Map<ArtifactCoords.Gav, Set<String>> processedNonProfilePackages = new HashMap<>(3);
    private Map<ArtifactCoords.Gav, Set<String>> processedPackages = new HashMap<>(3);
    private ArtifactCoords.Gav lastLoggedGav;
    private String lastLoggedPackage;

    DomainScriptCollector(ProvisioningContext ctx) throws ProvisioningException {
        this.ctx = ctx;
        script = ctx.getTmpDir().resolve("domain.cli");
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(script, StandardOpenOption.CREATE);
            writer.write("embed-host-controller --empty-host-config --empty-domain-config --remove-existing-host-config --remove-existing-domain-config");
            writer.newLine();
            scriptWriter = writer;
        } catch (IOException e) {
            try {
                writer.close();
            } catch (IOException e1) {
            }
            throw new ProvisioningException(Errors.writeFile(script), e);
        }
    }

    private void addCommand(String line) throws ProvisioningException {
        try {
            scriptWriter.write(line);
            scriptWriter.newLine();
        } catch(IOException e) {
            throw new ProvisioningException(Errors.writeFile(script), e);
        }
    }

    private void reset() {
        processedPackages.clear();
        lastLoggedGav = null;
        lastLoggedPackage = null;
    }

    void collectScripts(final ProvisionedFeaturePack provisionedFp, String pkgName, final String profile)
            throws ProvisioningException {
        final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
        System.out.println("Collecting domain configuration scripts for profile " + profile + " from feature-pack " + provisionedFp.getGav() + " package " + pkgName);
        addCommand("set profile=" + profile);
        if(profile.equals("default")) {
            addCommand("set socketGroup=standard");
        } else {
            addCommand("set socketGroup=" + profile);
        }
        collectScripts(ctx.getLayoutDescription().getFeaturePack(fpGav.toGa()), pkgName, provisionedFp,
                getProcessedPackages(fpGav),
                LayoutUtils.getFeaturePackDir(ctx.getLayoutDir(), fpGav).resolve(Constants.PACKAGES));
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

        Set<String> includedNonProfilePackages = processedNonProfilePackages.get(fpSpec.getGav());
        if (includedNonProfilePackages == null) {
            includedNonProfilePackages = new HashSet<>();
            processedNonProfilePackages.put(fpSpec.getGav(), includedNonProfilePackages);
        }

        Path scriptPath;
        final boolean doNonProfile = includedNonProfilePackages.add(pkgName);
        if (doNonProfile) {
            for(String script : NON_PROFILE) {
                scriptPath = wfDir.resolve(script);
                if(Files.exists(scriptPath)) {
                    addScript(scriptPath);
                    logScript(provisionedFp, pkgSpec.getName(), scriptPath);
                }
            }
        }

        scriptPath = wfDir.resolve(PROFILE_CLI);
        if (Files.exists(scriptPath)) {
            addScript(scriptPath);
            logScript(provisionedFp, pkgSpec.getName(), scriptPath);
        }

        if (doNonProfile) {
            scriptPath = wfDir.resolve(HOST_CLI);
            if(Files.exists(scriptPath)) {
                addScript(scriptPath);
                logScript(provisionedFp, pkgSpec.getName(), scriptPath);
            }
        }
    }

    private void addScript(Path p) throws ProvisioningException {
        addCommand("echo executing " + p);
        try {
            try (BufferedReader reader = Files.newBufferedReader(p)) {
                String line = reader.readLine();
                while (line != null) {
                    if(!line.isEmpty()) {
                        addCommand(line);
                    }
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            throw new ProvisioningException("Failed to read " + p);
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

    void run() throws ProvisioningException {
        System.out.println(" Generating domain configuration");
        try {
            scriptWriter.flush();
            scriptWriter.close();
            //IoUtils.copy(script, ctx.getInstallDir().resolve("script.cli"));
        } catch(IOException e) {
            throw new ProvisioningException(Errors.writeFile(script), e);
        }

        this.processedNonProfilePackages.clear();
        this.processedPackages.clear();
        this.scriptWriter = null;
        this.lastLoggedGav = null;
        this.lastLoggedPackage = null;

        final CliCommandBuilder builder = CliCommandBuilder
                .of(ctx.getInstallDir())
                .addCliArgument("--echo-command")
                .addCliArgument("--file=" + script);

        final ProcessBuilder processBuilder = new ProcessBuilder(builder.build()).redirectErrorStream(true);
        processBuilder.environment().put("JBOSS_HOME", ctx.getInstallDir().toString());

        final Process cliProcess;
        try {
            cliProcess = processBuilder.start();

            String echoLine = null;
            int opIndex = 1;
            final StringWriter errorWriter = new StringWriter();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(cliProcess.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(errorWriter)) {
                String line = reader.readLine();
                while (line != null) {
                    if (line.startsWith("executing ")) {
                        echoLine = line;
                        opIndex = 1;
                        writer.flush();
                        errorWriter.getBuffer().setLength(0);
                    } else {
                        if (line.equals("}")) {
                            ++opIndex;
                            writer.flush();
                            errorWriter.getBuffer().setLength(0);
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if(cliProcess.isAlive()) {
                try {
                    cliProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(cliProcess.exitValue() != 0) {
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
                            " package " + pkgName + " line #" + opIndex);
                    System.out.println(errorWriter.getBuffer());
                } else {
                    System.out.println("Could not locate the cause of the error in the CLI output.");
                    System.out.println(errorWriter.getBuffer());
                }
                throw new ProvisioningException("CLI configuration scripts failed.");
            }
        } catch (IOException e) {
            throw new ProvisioningException("Embedded CLI process failed", e);
        }
    }
}
