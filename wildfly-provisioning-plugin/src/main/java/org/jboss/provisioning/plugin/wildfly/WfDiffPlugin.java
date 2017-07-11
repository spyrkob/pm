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

import java.io.IOException;
import java.nio.file.Path;
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
        System.out.println("WildFly diff plug-in");
        Process process = null;
        Process embeddedServer = null;
        try {
            process = launchServer(customizedInstallation.toAbsolutePath());
            String host = getParameter(runtime, "host", "127.0.0.1");
            String port = getParameter(runtime, "port", "9990");
            String protocol = getParameter(runtime, "protocol", "remote+http");
            String username = getParameter(runtime, "username", "admin");
            String password = getParameter(runtime, "password", "passw0rd!");
            embeddedServer = launchEmbeddedServer(runtime.getInstallDir(),
                    "embed-server --server-config=standalone.xml --jboss-home=" + runtime.getInstallDir().toAbsolutePath(),
                    String.format("/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)", host, port, protocol, username, password),
                    "attachment save --operation=/synchronization=simple:export-diff --file=" + target.toAbsolutePath());
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

    private static Process launchEmbeddedServer(Path installDir, String ... commands) throws IOException {
        System.out.println("Starting full server for " + installDir);
        Launcher launcher = new Launcher(CliCommandBuilder.of(installDir).setCommands(commands))
                .setRedirectErrorStream(true)
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }

    private static Process launchServer(Path installDir) throws IOException {
        System.out.println("Starting full server for " + installDir);
        Launcher launcher = new Launcher(StandaloneCommandBuilder.of(installDir))
                .setRedirectErrorStream(true)
                .addEnvironmentVariable("JBOSS_HOME", installDir.toString());
        return launcher.launch();
    }

}
