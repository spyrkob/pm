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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.wildfly.core.launcher.CliCommandBuilder;
import org.wildfly.core.launcher.Launcher;
import org.wildfly.core.launcher.ProcessHelper;
import org.wildfly.core.launcher.StandaloneCommandBuilder;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfDiffPlugin implements DiffPlugin {

    private static final String CONFIGURE_SYNC = "/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)";
    private static final String EXPORT_DIFF = "attachment save --operation=/synchronization=simple:export-diff --file=%s";

    @Override
    public void calculateConfiguationChanges(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException {
        if(runtime.trace()) {
            System.out.println("WildFly diff plug-in");
        }
//         JBoss Modules overrides the default providers
        Process process = null;
        Process embeddedServer = null;
        try {
            String host = getParameter(runtime, "host", "127.0.0.1");
            String port = getParameter(runtime, "port", "9990");
            String protocol = getParameter(runtime, "protocol", "remote+http");
            String username = getParameter(runtime, "username", "admin");
            String password = getParameter(runtime, "password", "passw0rd!");
            String serverConfig = getParameter(runtime, "server-config", "standalone.xml");
            Path synchronizationFile = runtime.getInstallDir().resolve("bin").resolve("synchronization.cli");
            Files.deleteIfExists(synchronizationFile);
            try(BufferedWriter out = Files.newBufferedWriter(synchronizationFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                out.write("embed-server --admin-only --server-config=standalone.xml");
                out.newLine();
                out.write(String.format(CONFIGURE_SYNC, host, port, protocol, username, password));
                out.newLine();
                out.write(String.format(EXPORT_DIFF, target.toAbsolutePath()));
                out.newLine();
                out.write("exit");
                out.newLine();
                out.flush();
            }
            process = launchServer(customizedInstallation.toAbsolutePath(), serverConfig, runtime.trace());
             if(!process.isAlive() && process.exitValue() != 0) {
                throw new ProvisioningException(String.format("Error executing synchronization. Couldn't start the installaed server at %s", customizedInstallation.toAbsolutePath()));
            }
            embeddedServer = launchEmbeddedServerProcess(runtime.getInstallDir(), synchronizationFile, runtime.trace());
            if(!embeddedServer.isAlive() && embeddedServer.exitValue() != 0) {
                throw new ProvisioningException("Error executing synchronization");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(WfDiffPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                ProcessHelper.destroyProcess(embeddedServer);
                ProcessHelper.destroyProcess(process);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getParameter(ProvisioningRuntime runtime, String name, String defaultValue) {
        String value = runtime.getParameter(name);
        if (value != null && ! value.isEmpty()) {
            return runtime.getParameter(name);
        }
        return defaultValue;
    }
private static Process launchEmbeddedServerProcess(Path installDir, Path synchronizationFile, boolean trace) throws IOException {
        if (trace) {
            System.out.printf("Starting embeded admin-only server for %s%n", installDir);
        }
        Path logFile = new File("synchronization.log").toPath();
        Files.deleteIfExists(logFile);
        CliCommandBuilder builder = CliCommandBuilder.of(installDir);
        if(trace) {
            builder.addCliArgument("--echo-command");
        }
        builder.setScriptFile(synchronizationFile);
        Launcher launcher = new Launcher(builder)
                .setRedirectErrorStream(true)
                .redirectOutput(logFile)
                .setDirectory(installDir.resolve("bin"))
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }

    private static Process launchServer(Path installDir, String serverConfig,  boolean trace) throws IOException {
        if (trace) {
            System.out.printf("Starting full server for %s using configuration file %s%n", installDir , serverConfig );
        }
        Launcher launcher = new Launcher(StandaloneCommandBuilder.of(installDir).setServerConfiguration(serverConfig))
                .setRedirectErrorStream(true)
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }

}
