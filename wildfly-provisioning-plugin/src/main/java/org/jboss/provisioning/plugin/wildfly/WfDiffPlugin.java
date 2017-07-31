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


import java.nio.file.Path;

import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;

/**
 * WildFly plugin to compute the model difference between an instance and a clean provisioned instance.
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfDiffPlugin implements DiffPlugin {

    private static final String CONFIGURE_SYNC = "/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)";
    private static final String EXPORT_DIFF = "attachment save --operation=/synchronization=simple:export-diff --file=%s";

    @Override
    public void calculateConfiguationChanges(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException {
        final MessageWriter messageWriter = runtime.getMessageWriter();
        messageWriter.verbose("WildFly diff plug-in");
        String host = getParameter(runtime, "host", "127.0.0.1");
        String port = getParameter(runtime, "port", "9990");
        String protocol = getParameter(runtime, "protocol", "remote+http");
        String username = getParameter(runtime, "username", "admin");
        String password = getParameter(runtime, "password", "passw0rd!");
        String serverConfig = getParameter(runtime, "server-config", "standalone.xml");
        Server server = new Server(customizedInstallation.toAbsolutePath(), serverConfig, messageWriter);
        EmbeddedServer embeddedServer = new EmbeddedServer(
                runtime.getInstallDir().toAbsolutePath(),
                runtime.getInstallDir().resolve("bin").resolve("jboss-cli.xml").toAbsolutePath(),
                messageWriter);
        try {
            server.startServer();
            embeddedServer.execute(true,
                    String.format(CONFIGURE_SYNC, host, port, protocol, username, password),
                    String.format(EXPORT_DIFF, target.toAbsolutePath()));
        } finally {
            server.stopServer();
        }
    }

    private String getParameter(ProvisioningRuntime runtime, String name, String defaultValue) {
        String value = runtime.getParameter(name);
        if (value != null && ! value.isEmpty()) {
            return runtime.getParameter(name);
        }
        return defaultValue;
    }
}
