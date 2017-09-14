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
import java.nio.file.Paths;

import org.jboss.provisioning.ArtifactCoords;
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
                .addCliArgument("--echo-command")
                .addCliArgument("--file=" + script);

        final ProcessBuilder processBuilder = new ProcessBuilder(builder.build()).redirectErrorStream(true);
        processBuilder.environment().put("JBOSS_HOME", installHome.toString());

        final Process cliProcess;
        try {
            cliProcess = processBuilder.start();

            String echoLine = null;
            int opIndex = 1;
            final StringWriter errorWriter = new StringWriter();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(cliProcess.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(errorWriter)) {
                String line = reader.readLine();
                boolean flush = false;
                while (line != null) {
                    if (line.startsWith("executing ")) {
                        echoLine = line;
                        opIndex = 1;
                        writer.flush();
                        errorWriter.getBuffer().setLength(0);
                    } else {
                        if (line.equals("}")) {
                            ++opIndex;
                            flush = true;
                        } else if (flush){
                            writer.flush();
                            errorWriter.getBuffer().setLength(0);
                            flush = false;
                        }
                        writer.write(line);
                        writer.newLine();
                    }
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
                if(echoLine != null) {
                    Path p = Paths.get(echoLine.substring("executing ".length()));
                    final String scriptName = p.getFileName().toString();
                    p = p.getParent();
                    p = p.getParent();
                    p = p.getParent();
                    final String pkgName = p.getFileName().toString();
                    p = p.getParent();
                    p = p.getParent();
                    final String fpVersion = p.getFileName().toString();
                    p = p.getParent();
                    final String fpArtifact = p.getFileName().toString();
                    p = p.getParent();
                    final String fpGroup = p.getFileName().toString();
                    messageWriter.error("Failed to execute script %s from %s package %s line # %d", scriptName,
                            ArtifactCoords.newGav(fpGroup, fpArtifact, fpVersion), pkgName, opIndex);
                    messageWriter.error(errorWriter.getBuffer());
                } else {
                    messageWriter.error("Could not locate the cause of the error in the CLI output.");
                    messageWriter.error(errorWriter.getBuffer());
                }
                final StringBuilder buf = new StringBuilder("CLI configuration scripts failed");
//                try {
//                    final Path scriptCopy = Paths.get("/home/olubyans/pm-test").resolve(script.getFileName());
//                    IoUtils.copy(script, scriptCopy);
//                    buf.append(" (the failed script was copied to ").append(scriptCopy).append(')');
//                } catch(IOException e) {
//                    e.printStackTrace();
//                }
                throw new ProvisioningException(buf.toString());
            }
        } catch (IOException e) {
            throw new ProvisioningException("Embedded CLI process failed", e);
        }
    }
}