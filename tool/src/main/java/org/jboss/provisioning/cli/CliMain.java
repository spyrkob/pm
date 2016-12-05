/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.command.invocation.CommandInvocationServices;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.extensions.exit.Exit;
import org.jboss.aesh.extensions.less.aesh.Less;
import org.jboss.aesh.extensions.ls.Ls;
import org.jboss.aesh.extensions.mkdir.Mkdir;
import org.jboss.aesh.extensions.pwd.Pwd;
import org.jboss.aesh.extensions.rm.Rm;

/**
 *
 * @author Alexey Loubyansky
 */
public class CliMain {

    public static void main(String[] args) throws Exception {
        final Settings settings = new SettingsBuilder().logging(true).create();

        final PmSession pmSession = new PmSession();
        pmSession.updatePrompt(settings.getAeshContext());

        final CommandInvocationServices ciServices = new CommandInvocationServices();
        ciServices.registerDefaultProvider(pmSession);

        final AeshConsole aeshConsole = new AeshConsoleBuilder().settings(settings).prompt(pmSession.getPrompt())
                // provisioning commands
                .addCommand(new InstallCommand())
                .addCommand(new ProvisionedSpecCommand())
                .addCommand(new ProvisionSpecCommand())
                .addCommand(new UninstallCommand())
                // filesystem-related commands
                .addCommand(new CdCommand())
                .addCommand(new Exit())
                .addCommand(new Less())
                .addCommand(new Ls())
                .addCommand(new Mkdir())
                .addCommand(new Rm())
                .addCommand(new Pwd())
                .commandInvocationProvider(ciServices)
                .create();
        aeshConsole.start();
    }
}
