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
package org.jboss.provisioning.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.io.Resource;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@CommandDefinition(name="diff", description="Saves current provisioned configuration into the specified file.")
public class ProvisionedConfigExportCommand extends ProvisioningCommand {

    @Arguments(completer=FileOptionCompleter.class, description="File to save the provisioned spec too.")
    private List<Resource> fileArg;

    @Option(name = "username", required = true,
            description = "User to connect to provisionned server.")
    protected String username;
    @Option(name = "password", required = true,
            description = "Password to connect to provisionned server.")
    protected String password;
    @Option(name = "port", required = false, defaultValue="9990",
            description = "Protocol to connect to provisionned server.")
    protected String port;
    @Option(name = "host", required = false, defaultValue="127.0.0.1",
            description = "Protocol to connect to provisionned server.")
    protected String host;
    @Option(name = "protocol", required = false, defaultValue = "remote+http",
            description = "Protocol to connect to provisionned server.")
    protected String protocol;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        if(fileArg == null || fileArg.isEmpty() || fileArg.isEmpty()) {
            throw new CommandExecutionException("Missing required file path arguments.");
        }
        if(fileArg.size() > 2) {
            throw new CommandExecutionException("The command expects only two argument.");
        }

        final Resource specResource = fileArg.get(0).resolve(session.getAeshContext().getCurrentWorkingDirectory()).get(0);
        final Path sourceFile = Paths.get(specResource.getAbsolutePath());
        Map<String, String> parameters = new HashMap<>(5);
        if (host != null) {
            parameters.put("host", host);
        }
        if (port != null) {
            parameters.put("port", port);
        }
        if (protocol != null) {
            parameters.put("protocol", protocol);
        }
        if (username != null) {
            parameters.put("username", username);
        }
        if (password != null) {
            parameters.put("password", password);
        }
        final Resource specTargetResource = fileArg.get(1).resolve(session.getAeshContext().getCurrentWorkingDirectory()).get(0);
        final Path targetFile = Paths.get(specTargetResource.getAbsolutePath());
        try {
            getManager(sourceFile).exportConfigurationChanges(targetFile, parameters);
        } catch (ProvisioningException | IOException e) {
            throw new CommandExecutionException("Failed to export provisioned state", e);
        }
    }

    protected ProvisioningManager getManager(Path sourceFile) {
        return ProvisioningManager.builder()
                .setArtifactResolver(ArtifactResolverImpl.getInstance())
                .setInstallationHome(sourceFile)
                .build();
    }
}
