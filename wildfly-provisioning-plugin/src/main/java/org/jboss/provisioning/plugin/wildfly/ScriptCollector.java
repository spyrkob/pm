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
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisioningContext;
import org.jboss.provisioning.plugin.wildfly.config.PackageScripts;
import org.jboss.provisioning.plugin.wildfly.config.PackageScriptsParser;
import org.jboss.provisioning.plugin.wildfly.config.PackageScripts.Script;
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
abstract class ScriptCollector {

    private final ProvisioningContext ctx;

    private String configName;
    private Path script;
    private BufferedWriter scriptWriter;

    private Map<ArtifactCoords.Gav, Set<String>> processedStaticPackages = new HashMap<>(3);
    private Map<ArtifactCoords.Gav, Set<String>> processedVariablePackages = new HashMap<>(3);
    private ArtifactCoords.Gav lastLoggedGav;
    private String lastLoggedPackage;

    ScriptCollector(ProvisioningContext ctx) throws ProvisioningException {
        this.ctx = ctx;
    }


    protected void init(String configName, String embedCommand) throws ProvisioningException {
        reset();
        processedStaticPackages.clear();
        this.configName = configName;
        script = ctx.getTmpDir().resolve(configName);
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(script, StandardOpenOption.CREATE_NEW);
            writer.write(embedCommand);
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
            try {
                scriptWriter.close();
            } catch (IOException eClose) {
            }
            throw new ProvisioningException(Errors.writeFile(script), e);
        }
    }

    private void addCommand(String line, String prefix) throws ProvisioningException {
        try {
            if(line.isEmpty()) {
                scriptWriter.newLine();
                return;
            }
            switch(line.charAt(0)) {
                case '#':
                    scriptWriter.write('#');
                case '/':
                case ':':
                    if(prefix != null) {
                        scriptWriter.write(prefix);
                    }
                default:
            }
        } catch(IOException e) {
            try {
                scriptWriter.close();
            } catch (IOException eClose) {
            }
            throw new ProvisioningException(Errors.writeFile(script), e);
        }
        addCommand(line);
    }

    private void reset() {
        processedVariablePackages.clear();
        lastLoggedGav = null;
        lastLoggedPackage = null;
    }

    void collectScripts(final ProvisionedFeaturePack provisionedFp, String pkgName, final String profile)
            throws ProvisioningException {
        final ArtifactCoords.Gav fpGav = provisionedFp.getGav();
        final StringBuilder buf = new StringBuilder();
        buf.append("Collecting ").append(configName).append(" configuration scripts");
        if(profile != null) {
            buf.append(" for profile ").append(profile);
        }
        buf.append(" from feature-pack ").append(provisionedFp.getGav().toString()).append(" package ").append(pkgName);
        System.out.println(buf);
        if(profile != null) {
            addCommand("set profile=" + profile);
            addCommand("/profile=$profile:add");
        }
        if(profile == null || profile.equals("default")) {
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

        Set<String> includedStaticPackages = processedStaticPackages.get(fpSpec.getGav());
        if (includedStaticPackages == null) {
            includedStaticPackages = new HashSet<>();
            processedStaticPackages.put(fpSpec.getGav(), includedStaticPackages);
        }
        final boolean includeStatic = includedStaticPackages.add(pkgName);

        final PackageScripts scripts;
        final Path scriptsPath = wfDir.resolve("scripts.xml");
        if(Files.exists(scriptsPath)) {
            try(InputStream input = Files.newInputStream(scriptsPath)) {
                scripts = new PackageScriptsParser().parse(input);
            } catch (IOException e) {
                throw new ProvisioningException(Errors.readFile(scriptsPath), e);
            } catch (XMLStreamException e) {
                throw new ProvisioningException(Errors.parseXml(scriptsPath), e);
            }
        } else {
            scripts = PackageScripts.DEFAULT;
        }
        collect(scripts, provisionedFp, pkgSpec, wfDir, includeStatic);
    }

    protected abstract void collect(final PackageScripts scripts,
            final ProvisionedFeaturePack provisionedFp,
            final PackageSpec pkgSpec,
            final Path wfDir,
            final boolean includeStatic) throws ProvisioningException;

    protected void addScripts(final ProvisionedFeaturePack provisionedFp, final PackageSpec pkgSpec, final Path wfDir,
            final boolean includeStatic, List<Script> scripts) throws ProvisioningException {
        for(Script script : scripts) {
            if(!includeStatic && script.isStatic()) {
                continue;
            }
            if(script.getLine() != null) {
                final Path scriptPath = wfDir.resolve("scripts.xml");
                addCommand("echo executing " + scriptPath);
                addCommand(script.getLine(), script.getPrefix());
                logScript(provisionedFp, pkgSpec.getName(), scriptPath);
                return;
            }
            if(script.getPath() == null) {
                throw new ProvisioningException("Script path is missing");
            }
            final Path scriptPath = wfDir.resolve(script.getPath());
            if(!Files.exists(scriptPath)) {
                continue;
            }
            if(script.hasParameters()) {
                // parameters set before 'echo executing ' to correctly identify line numbers for the commands
                for(Map.Entry<String, String> param : script.getParameters().entrySet()) {
                    addCommand("set " + param.getKey() + '=' + param.getValue());
                }
            }
            addCommand("echo executing " + scriptPath);
            try (BufferedReader reader = Files.newBufferedReader(scriptPath)) {
                final String prefix = script.getPrefix();
                String line = reader.readLine();
                while (line != null) {
                    if (!line.isEmpty()) {
                        addCommand(line, prefix);
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new ProvisioningException("Failed to read " + scriptPath);
            }
            if(script.hasParameters()) {
                for(String param : script.getParameters().keySet()) {
                    addCommand("unset " + param);
                }
            }
            logScript(provisionedFp, pkgSpec.getName(), scriptPath);
        }
    }

    protected void addScript(Path p, Script script) throws ProvisioningException {
        if(script.hasParameters()) {
            // parameters set before 'echo executing ' to correctly identify line numbers for the commands
            for(Map.Entry<String, String> param : script.getParameters().entrySet()) {
                addCommand("set " + param.getKey() + '=' + param.getValue());
            }
        }
        addCommand("echo executing " + p);
        try (BufferedReader reader = Files.newBufferedReader(p)) {
            final String prefix = script.getPrefix();
            String line = reader.readLine();
            while (line != null) {
                if (!line.isEmpty()) {
                    addCommand(line, prefix);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new ProvisioningException("Failed to read " + p);
        }
        if(script.hasParameters()) {
            // parameters set before 'echo executing ' to correctly identify line numbers for the commands
            for(String param : script.getParameters().keySet()) {
                addCommand("unset " + param);
            }
        }
    }

    private Set<String> getProcessedPackages(ArtifactCoords.Gav fpGav) {
        Set<String> fpProcessed = processedVariablePackages.get(fpGav);
        if(fpProcessed == null) {
            fpProcessed = new HashSet<>();
            processedVariablePackages.put(fpGav, fpProcessed);
        }
        return fpProcessed;
    }

    protected void logScript(final ProvisionedFeaturePack provisionedFp, String pkgName, Path script) {
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
        System.out.print(" Generating ");
        System.out.print(configName);
        System.out.println(" configuration");
        try {
            scriptWriter.flush();
            scriptWriter.close();
            //IoUtils.copy(script, ctx.getInstallDir().resolve(script.getFileName().toString() + ".cli"));
        } catch(IOException e) {
            throw new ProvisioningException(Errors.writeFile(script), e);
        }

        this.processedStaticPackages.clear();
        this.processedVariablePackages.clear();
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
                boolean flush = false;
                while (line != null) {
                    if (line.startsWith("executing ")) {
                        echoLine = line;
                        opIndex = 1;
                        writer.flush();
                        errorWriter.getBuffer().setLength(0);
                    } else {
                        if (line.equals("}")) {
                            ++opIndex;
                            flush = true;
                        } else if (flush){
                            writer.flush();
                            errorWriter.getBuffer().setLength(0);
                            flush = false;
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
