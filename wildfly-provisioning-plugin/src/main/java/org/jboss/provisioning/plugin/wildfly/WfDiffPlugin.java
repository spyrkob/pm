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
import java.io.OutputStreamWriter;
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

    @Override
    public void calculateConfiguationChanges(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException {
        if(runtime.trace()) {
            System.out.println("WildFly diff plug-in");
        }
//         JBoss Modules overrides the default providers
        Process process = null;
        Process embeddedServer = null;
        try {
            process = launchServer(customizedInstallation.toAbsolutePath(), runtime.trace());
            String host = getParameter(runtime, "host", "127.0.0.1");
            String port = getParameter(runtime, "port", "9990");
            String protocol = getParameter(runtime, "protocol", "remote+http");
            String username = getParameter(runtime, "username", "admin");
            String password = getParameter(runtime, "password", "passw0rd!");
            embeddedServer = launchEmbeddedServerProcess(runtime.getInstallDir(), runtime.trace());
            Path logFile = runtime.getInstallDir().resolve("standalone").resolve("log").resolve("synchronization.log");
            Files.deleteIfExists(logFile);
            try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(embeddedServer.getOutputStream(), StandardCharsets.UTF_8));
                    BufferedWriter log = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(logFile, StandardOpenOption.CREATE), StandardCharsets.UTF_8));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(embeddedServer.getInputStream(), StandardCharsets.UTF_8))) {
                log.write(reader.readLine());log.newLine();
                out.write("embed-server --admin-only --server-config=standalone.xml");
                out.newLine();
                out.flush();
                log.write(reader.readLine());log.newLine();
                out.write(String.format("/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)", host, port, protocol, username, password));
                out.newLine();
                out.flush();
                log.write(reader.readLine());log.newLine();
                log.write(reader.readLine());log.newLine();
                out.write(("attachment save --operation=/synchronization=simple:export-diff --file=" + target.toAbsolutePath()));
                out.newLine();
                out.flush();
                log.write(reader.readLine());log.newLine();
                String lastLine = reader.readLine();
                log.write(lastLine);log.newLine();
                if (runtime.trace()) {
                    System.out.println(lastLine);
                }
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

    private static Process launchEmbeddedServerProcess(Path installDir, boolean trace) throws IOException {
        if (trace) {
            System.out.println("Starting embeded admin-only server for " + installDir);
        }
        Launcher launcher = new Launcher(CliCommandBuilder.of(installDir))
                .setRedirectErrorStream(true)
                .setDirectory(installDir.resolve("bin"))
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }
    private static Process launchServer(Path installDir, boolean trace) throws IOException {
        if (trace) {
            System.out.println("Starting full server for " + installDir);
        }
        Launcher launcher = new Launcher(StandaloneCommandBuilder.of(installDir))
                .setRedirectErrorStream(true)
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }

}
