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
import java.io.StringWriter;
import java.nio.file.Path;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningException;
import org.wildfly.core.launcher.CliCommandBuilder;

/**
 * @author Alexey Loubyansky
 *
 */
public class CliScriptRunner {

    public static void runCliScript(Path installHome, Path script, MessageWriter messageWriter) throws ProvisioningException {
        final CliCommandBuilder builder = CliCommandBuilder
                .of(installHome)
                .addCliArgument("--no-operation-validation")
                .addCliArgument("--echo-command")
                .addCliArgument("--file=" + script);
        messageWriter.verbose("Executing jboss console: " + builder.build());
        final ProcessBuilder processBuilder = new ProcessBuilder(builder.build()).redirectErrorStream(true);
        processBuilder.environment().put("JBOSS_HOME", installHome.toString());

        final Process cliProcess;
        try {
            cliProcess = processBuilder.start();

            final StringWriter errorWriter = new StringWriter();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(cliProcess.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(errorWriter)) {
                String line = reader.readLine();
                boolean flush = false;
                while (line != null) {
                    if (line.equals("}")) {
                        flush = true;
                    } else if (flush) {
                        writer.flush();
                        errorWriter.getBuffer().setLength(0);
                        flush = false;
                    }
                    writer.write(line);
                    writer.newLine();
                    line = reader.readLine();
                }
            } catch (IOException e) {
                messageWriter.error(e, e.getMessage());
            }

            if(cliProcess.isAlive()) {
                try {
                    cliProcess.waitFor();
                } catch (InterruptedException e) {
                    messageWriter.error(e, e.getMessage());
                }
            }

            if(cliProcess.exitValue() != 0) {
//                try {
//                    final Path scriptCopy = Paths.get("/home/olubyans/pm-test").resolve(script.getFileName());
//                    IoUtils.copy(script, scriptCopy);
//                    buf.append(" (the failed script was copied to ").append(scriptCopy).append(')');
//                } catch(IOException e) {
//                    e.printStackTrace();
//                }
                throw new ProvisioningException(errorWriter.getBuffer().toString());
            }
        } catch (IOException e) {
            throw new ProvisioningException("CLI process failed", e);
        }
    }
}
