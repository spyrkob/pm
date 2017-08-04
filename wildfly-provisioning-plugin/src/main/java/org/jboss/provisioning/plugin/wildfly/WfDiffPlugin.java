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


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.diff.FileSystemDiff;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.util.PathFilter;

/**
 * WildFly plugin to compute the model difference between an instance and a clean provisioned instance.
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfDiffPlugin implements DiffPlugin {

    private static final String CONFIGURE_SYNC = "/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)";
    private static final String EXPORT_DIFF = "attachment save --overwrite --operation=/synchronization=simple:export-diff --file=%s";

    private static final PathFilter FILTER_FP = PathFilter.Builder.instance()
            .addDirectories("*" + File.separatorChar + "tmp", "*" + File.separatorChar + "log","*_xml_history", "model_diff")
            .addFiles("standalone.xml", "process-uuid", "logging.properties")
            .build();

    private static final PathFilter FILTER = PathFilter.Builder.instance()
            .addDirectories("*" + File.separatorChar + "tmp", "model_diff")
            .addFiles("standalone.xml", "logging.properties")
            .build();

    @Override
    public void computeDiff(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException {
        final MessageWriter messageWriter = runtime.getMessageWriter();
        messageWriter.verbose("WildFly diff plug-in");
        FileSystemDiff diff = new FileSystemDiff(messageWriter, runtime.getInstallDir(), customizedInstallation);
        String host = getParameter(runtime, "host", "127.0.0.1");
        String port = getParameter(runtime, "port", "9990");
        String protocol = getParameter(runtime, "protocol", "remote+http");
        String username = getParameter(runtime, "username", "admin");
        String password = getParameter(runtime, "password", "passw0rd!");
        String serverConfig = getParameter(runtime, "server-config", "standalone.xml");
        Server server = new Server(customizedInstallation.toAbsolutePath(), serverConfig, messageWriter);
        EmbeddedServer embeddedServer = new EmbeddedServer(runtime.getInstallDir().toAbsolutePath(), messageWriter);
        try {
            Files.createDirectories(target);
            server.startServer();
            embeddedServer.execute(false,
                    String.format(CONFIGURE_SYNC, host, port, protocol, username, password),
                    String.format(EXPORT_DIFF, target.resolve("finalize.cli").toAbsolutePath()));
            WfDiffResult result = new WfDiffResult(
                    Collections.singletonList(target.resolve("finalize.cli").toAbsolutePath()),
                    diff.diff(getFilter(runtime)));
            runtime.setDiff(result.merge(runtime.getDiff()));
        } catch (IOException ex) {
            messageWriter.error(ex, "Couldn't compute the WildFly Model diff because of %s", ex.getMessage());
            Logger.getLogger(WfDiffPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            server.stopServer();
        }
    }

    private PathFilter getFilter(ProvisioningRuntime runtime) {
        if("diff-to-feature-pack".equals(runtime.getOperation())) {
            return FILTER_FP;
        }
       return FILTER;
    }
}
