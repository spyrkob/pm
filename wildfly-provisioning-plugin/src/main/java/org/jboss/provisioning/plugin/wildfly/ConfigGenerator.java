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

package org.jboss.provisioning.plugin.wildfly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.cli.CommandLineException;
import org.wildfly.core.launcher.CliCommandBuilder;

/**
 *
 * @author Alexey Loubyansky
 */
class ConfigGenerator {

    public static ConfigGenerator newInstance(Path installDir) {
        return new ConfigGenerator(installDir);
    }

    private final Path installDir;
    private final List<String> commands = new ArrayList<>();

    private ConfigGenerator(Path installDir) {
        this.installDir = installDir;
        commands.add("embed-server --empty-config --remove-existing");
    }

    public ConfigGenerator addCommandLine(String cmdLine) {
        commands.add(cmdLine);
        return this;
    }

    public ConfigGenerator addCommandLines(Collection<String> cmdLines) {
        commands.addAll(cmdLines);
        return this;
    }

    public ConfigGenerator addCommandLines(Path p) throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(p)) {
            String line = reader.readLine();
            while(line != null) {
                addCommandLine(line);
                line = reader.readLine();
            }
        }
        return this;
    }

    public void generate() throws CommandLineException {
        commands.add("exit");
        CliCommandBuilder builder = CliCommandBuilder
                .of(installDir)
                .setCommands(commands);

        final ProcessBuilder processBuilder = new ProcessBuilder(builder.build()).redirectErrorStream(true);
        processBuilder.environment().put("JBOSS_HOME", installDir.toString());

        Process cliProcess;
        try {
            cliProcess = processBuilder.start();
            cliProcess.waitFor();
            if(cliProcess.exitValue() != 0) {
                final InputStream cliOutput = cliProcess.getInputStream();
                if(cliOutput == null) {
                    System.out.println("CLI output is not available");
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(cliOutput))) {
                        String line = reader.readLine();
                        while (line != null) {
                            System.out.println("cli> " + line);
                            line = reader.readLine();
                        }
                    }
                }
                throw new CommandLineException("Embeedded CLI scripts failed.");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //final CommandContext cliCtx = CommandContextFactory.getInstance().newCommandContext();
        //cliCtx.handle("embed-server --empty-config --remove-existing --jboss-home=" + installDir);
/*        for(String cmdLine : commands) {
            cliCtx.handle(cmdLine);
        }
        cliCtx.handle("exit");
*/    }
}
