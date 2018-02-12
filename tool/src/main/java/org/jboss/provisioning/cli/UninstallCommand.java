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

import org.aesh.command.CommandDefinition;
import org.aesh.command.option.Argument;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name = "uninstall", description = "Uninstalls specified feature-pack")
public class UninstallCommand extends ProvisioningCommand {

    @Argument(completer = GavCompleter.class, required = true)
    private String coord;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        if (coord == null) {
            throw new CommandExecutionException("feature-pack must be set");
        }
        // Is it a stream?
        // For now keep duality. TODO
        // We should retrieve the stream information in the current instalation.
        ArtifactCoords coords = session.getUniverses().resolveStream(coord);
        if (coords != null) {
            coord = coords.toString();
        }
        final ProvisioningManager manager = getManager(session);
        try {
            manager.uninstall(ArtifactCoords.newGav(coord));
        } catch (ProvisioningException e) {
            throw new CommandExecutionException("Provisioning failed", e);
        }
    }
}
