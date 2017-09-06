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
import java.util.Properties;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.CommandContextConfiguration;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningException;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class WfEmbeddedSupport<B extends WfEmbeddedSupport<B>> {

    public class EmbeddedSession implements AutoCloseable {

        private final CommandContext ctx;
        private final Properties sysProps;

        protected EmbeddedSession() throws ProvisioningException {
            sysProps = (Properties) System.getProperties().clone();
            CommandContext ctx = null;
            try {
                setCliConfigProp();
                ctx = CommandContextFactory.getInstance().newCommandContext(
                        new CommandContextConfiguration.Builder()
                        .setSilent(!messageWriter.isVerboseEnabled())
                        .setEchoCommand(messageWriter.isVerboseEnabled()).setInitConsole(false)
                         // Waiting for WFCORE-3118 .setValidateOperationRequests(validate)
                        .build());
                ctx.handle(embedCommand());
                this.ctx = ctx;
            } catch (Exception e) {
                close();
                throw new ProvisioningException("Error initializing the embedded session", e);
            }
        }

        public void handle(String line) throws ProvisioningException {
            try {
                ctx.handle(line);
            } catch (CommandLineException e) {
                throw new ProvisioningException("Failed to handle " + line, e);
            }
        }

        @Override
        public void close() {
            if (ctx != null) {
                ctx.handleSafe(stopEmbeddedCommand());
                ctx.terminateSession();
                clearSystemProps(sysProps);
            }
        }
    }

    protected Path installHome;
    protected Path cliConfig;
    protected MessageWriter messageWriter;
    protected boolean validate = true;

    @SuppressWarnings("unchecked")
    public B setInstallHome(Path installHome) {
        this.installHome = installHome;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setCliConfig(Path cliConfig) {
        this.cliConfig = cliConfig;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setMessageWriter(MessageWriter msgWriter) {
        this.messageWriter = msgWriter;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B setValidate(boolean validate) {
        this.validate = validate;
        return (B) this;
    }

    protected abstract String embedCommand();

    protected abstract String stopEmbeddedCommand();

    public void execute(String... commands) throws ProvisioningException {
        try(EmbeddedSession session = new EmbeddedSession()) {
            for (String cmd : commands) {
                session.handle(cmd);
            }
        } catch (ProvisioningException e) {
            throw e;
        }
    }

    public EmbeddedSession newSession() throws ProvisioningException {
        return new EmbeddedSession();
    }

    private void clearSystemProps(Properties props) {
        clearXMLConfiguration(props);
        System.clearProperty("jboss.cli.config");
    }

    private void setCliConfigProp() {
        if(cliConfig != null && Files.exists(cliConfig)) {
            System.setProperty("jboss.cli.config", cliConfig.toString());
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
}
