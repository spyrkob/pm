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

package org.jboss.provisioning.plugin.wildfly.embedded;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.provisioning.ProvisioningException;

/**
 *
 * @author Alexey Loubyansky
 */
public class JBossCli {

    private final Path cliConfig;
    private final boolean silent;
    private final boolean echoCommand;

    public JBossCli(Path cliConfig, boolean silent, boolean echoCommand) {
        this.cliConfig = cliConfig;
        this.silent = silent;
        this.echoCommand = echoCommand;
    }

    /**
     * Starts an embedded server to execute commands
     * @param commands the list of commands to execute on the embedded server.
     * @throws ProvisioningException
     */
    public void execute(String ... commands) throws ProvisioningException {
        Properties props = (Properties) System.getProperties().clone();
        CommandContext ctx = null;
        try {
            if(Files.exists(cliConfig)) {
                System.setProperty("jboss.cli.config", cliConfig.toString());
            }
            ctx = CommandContextFactory.getInstance().newCommandContext(
                    new CommandContextConfiguration.Builder()
                            .setSilent(silent)
                            .setEchoCommand(echoCommand)
                            .setInitConsole(false)
//Waiting for WFCORE-3118   .setValidateOperationRequests(validate)
                            .build());
            for (String cmd : commands) {
                ctx.handle(cmd);
            }
        } catch (Exception e) {
            throw new ProvisioningException("Failed to execute script", e);
        } finally {
            if(ctx != null) {
                ctx.terminateSession();
                clearXMLConfiguration(props);
                System.clearProperty("jboss.cli.config");
            }
        }
    }

    private void clearXMLConfiguration(Properties props) {
        clearProperty(props, "javax.xml.parsers.DocumentBuilderFactory");
        clearProperty(props, "javax.xml.parsers.SAXParserFactory");
        clearProperty(props, "javax.xml.transform.TransformerFactory");
        clearProperty(props, "javax.xml.xpath.XPathFactory");
        clearProperty(props, "javax.xml.stream.XMLEventFactory");
        clearProperty(props, "javax.xml.stream.XMLInputFactory");
        clearProperty(props, "javax.xml.stream.XMLOutputFactory");
        clearProperty(props, "javax.xml.datatype.DatatypeFactory");
        clearProperty(props, "javax.xml.validation.SchemaFactory");
        clearProperty(props, "org.xml.sax.driver");
    }

    private void clearProperty(Properties props , String name) {
        if(props.containsKey(name)) {
            System.setProperty(name, props.getProperty(name));
        } else {
            System.clearProperty(name);
        }
    }

    public void run(String... cmds) {
        System.out.println("RUN " + Arrays.asList(cmds));

        CommandContext ctx = null;
        try {
            ctx = CommandContextFactory.getInstance().newCommandContext(
                    new CommandContextConfiguration.Builder().build());
        } catch (CliInitializationException e) {
            throw new IllegalStateException(e);
        } finally {
            if(ctx != null) {
                ctx.terminateSession();
            }
        }
    }
}
