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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallationException;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.activation.OptionActivator;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.cl.internal.ProcessedCommand;
import org.jboss.aesh.cl.internal.ProcessedOption;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.plugin.util.MavenPluginUtil;
import org.jboss.provisioning.util.IoUtils;
import org.jboss.provisioning.util.analyzer.FeaturePackDependencyBuilder;
import org.jboss.provisioning.util.analyzer.FeaturePackDescriptionDiffs;
import org.jboss.provisioning.util.analyzer.FeaturePacksDiff;
import org.jboss.provisioning.wildfly.descr.WfFeaturePackLayoutBuilder;
import org.jboss.provisioning.wildfly.descr.WfInstallationDescription;
import org.jboss.provisioning.wildfly.xml.WFInstallationDefParser;
import org.jboss.provisioning.xml.FeaturePackXmlWriter;

/**
 *
 * @author Alexey Loubyansky
 */
@CommandDefinition(name="feature-pack",
    description = "Performs various tasks on feature packs including:\n" +
                  "- creation;\n" +
                  "- installation into the maven repository;\n" +
                  "- dependency analyzis.")
public class FpCommand extends PmSessionCommand {

    private static final String WF_FP_DEF_XML = "wildfly-feature-pack-def.xml";

    private static final String ACTION_ARG_NAME = "action";
    private static final String ANALYZE = "analyze";
    private static final String EXTRACT_PARENT = "extract-parent";
    private static final String INSTALL = "install";

    private static final String INSTALL_DIR_ARG_NAME = "install-dir";
    private static final String ENCODING_ARG_NAME = "encoding";
    private static final String WORK_DIR_ARG_NAME = "work-dir";

    private static class AfterActionActivator implements OptionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            for(Object o : processedCommand.getOptions()) {
                final ProcessedOption option = (ProcessedOption) o;
                if(option.getName().equals(ACTION_ARG_NAME)) {
                    return actionOption(option);
                }
            }
            return false;
        }

        protected boolean actionOption(ProcessedOption option) {
            return option.getValue() != null;
        }
    }

    private static class InstallActivator extends AfterActionActivator {
        @Override
        protected boolean actionOption(ProcessedOption option) {
            if (INSTALL.equals(option.getValue())) {
                return true;
            }
            return false;
        }
    }

    private static class XmlOnlyActivator extends AfterActionActivator {
        @Override
        protected boolean actionOption(ProcessedOption option) {
            if (EXTRACT_PARENT.equals(option.getValue())) {
                return true;
            }
            return false;
        }
    }

    private static class GavActivator extends AfterActionActivator {
        @Override
        public boolean isActivated(ProcessedCommand processedCommand) {
            int conditions = 0;
            for(Object o : processedCommand.getOptions()) {
                final ProcessedOption option = (ProcessedOption) o;
                final String optionValue = option.getValue();
                if(option.getName().equals(ACTION_ARG_NAME)) {
                    if (ANALYZE.equals(optionValue) ||
                            EXTRACT_PARENT.equals(optionValue)) {
                        if (conditions == 1) {
                            return true;
                        }
                        ++conditions;
                    } else {
                        return false;
                    }
                } else if(option.getName().equals(WORK_DIR_ARG_NAME)) {
                    if(optionValue != null) {
                        workDirActivatedValue = optionValue;
                        if(conditions == 1) {
                            return true;
                        }
                        ++conditions;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }

    private class GavCompleter implements OptionCompleter<CompleterInvocation> {
        @Override
        public void complete(CompleterInvocation ci) {
            if(workDirActivatedValue == null) {
                return;
            }
            Path path = Paths.get(workDirActivatedValue);
            if(!Files.isDirectory(path)) {
                return;
            }

            final String currentValue = ci.getGivenCompleteValue();
            int level = 1;
            String prefix = null;
            String chunk = null;
            int groupSeparator = currentValue.indexOf(':');
            if(groupSeparator > 0) {
                path = path.resolve(currentValue.substring(0, groupSeparator));
                ++level;
                if(groupSeparator + 1 < currentValue.length()) {
                    int artifactSeparator = currentValue.indexOf(':', groupSeparator + 1);
                    if(artifactSeparator > 0) {
                        ++level;
                        path = path.resolve(currentValue.substring(groupSeparator + 1, artifactSeparator));
                        if(artifactSeparator + 1 < currentValue.length()) {
                            prefix = currentValue.substring(0, artifactSeparator + 1);
                            chunk = currentValue.substring(artifactSeparator + 1);
                        } else {
                            prefix = currentValue;
                            chunk = null;
                        }
                    } else {
                        prefix = currentValue.substring(0, groupSeparator + 1);
                        chunk = currentValue.substring(groupSeparator + 1);
                    }
                } else {
                    prefix = currentValue;
                }
            } else {
                chunk = currentValue;
            }

            if(!Files.isDirectory(path)) {
                return;
            }

            final List<String> candidates = new ArrayList<String>();
            try {
                addCandidates(path, prefix, chunk, candidates, level);
            } catch (IOException e) {
                return;
            }
            ci.addAllCompleterValues(candidates);
        }

        private void addCandidates(Path path, String prefix, String chunk, final List<String> candidates, int level) throws IOException {
            Path child = null;
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(path,
                    (Path p) -> Files.isDirectory(p) && (chunk == null ? true : p.getFileName().toString().startsWith(chunk)))) {
                final Iterator<Path> iter = stream.iterator();
                while(iter.hasNext()) {
                    child = iter.next();
                    candidates.add(
                            prefix == null ? child.getFileName().toString() :
                                prefix + child.getFileName().toString());
                }
            }
            if(level < 3 && candidates.size() == 1) {
                prefix = candidates.get(0) + ':';
                candidates.clear();
                addCandidates(child, prefix, null, candidates, level + 1);
            }
        }
    }

    @Option(name=ACTION_ARG_NAME, required=true, defaultValue={ANALYZE, EXTRACT_PARENT, INSTALL})
    private String actionArg;

    @Option(name=INSTALL_DIR_ARG_NAME, completer=FileOptionCompleter.class, activator=InstallActivator.class)
    private String installDirArg;

    @Option(name=ENCODING_ARG_NAME, required=true, defaultValue="utf-8")
    private String encodingArg;

    @Option(name=WORK_DIR_ARG_NAME, completer=FileOptionCompleter.class, activator=AfterActionActivator.class)
    private String workDirArg;
    private static String workDirActivatedValue;

    @Option(name="gav1", completer=GavCompleter.class, activator=GavActivator.class)
    private String gav1;

    @Option(name="gav2", completer=GavCompleter.class, activator=GavActivator.class)
    private String gav2;

    @Option(name="xml-only", hasValue=false, required=false, activator=XmlOnlyActivator.class)
    private boolean xmlOnly;

    @Override
    protected void runCommand(PmSession session) throws CommandExecutionException {
        if(INSTALL.equals(actionArg)) {
            installAction();
        } else if(ANALYZE.equals(actionArg)) {
            analyzeAction(session);
        } else if(EXTRACT_PARENT.equals(actionArg)) {
            extractParentAction(session);
        } else {
            throw new CommandExecutionException("Unrecognized action '" + actionArg + "'");
        }
    }

    private void analyzeAction(CommandInvocation ci) throws CommandExecutionException {

        if(workDirArg == null) {
            argumentMissing(WORK_DIR_ARG_NAME);
        }
        if(gav1 == null) {
            argumentMissing("gav1");
        }
        if(gav2 == null) {
            argumentMissing("gav2");
        }
        final Path workDir = Paths.get(workDirArg);
        if(!Files.exists(workDir)) {
            throw new CommandExecutionException(Errors.pathDoesNotExist(workDir));
        }

        final FeaturePackDescriptionDiffs diff;
        try {
            diff = FeaturePacksDiff.compare(workDir, encodingArg, ArtifactCoords.newGav(gav1), ArtifactCoords.newGav(gav2));
        } catch (ProvisioningDescriptionException e) {
            throw new CommandExecutionException("Failed to analyze feature packs", e);
        }

        try {
            ci.println(diff.getFeaturePackDiff1().logContent());
            ci.println(diff.getFeaturePackDiff2().logContent());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void extractParentAction(CommandInvocation ci) throws CommandExecutionException {

        if(workDirArg == null) {
            argumentMissing(WORK_DIR_ARG_NAME);
        }
        if(gav1 == null) {
            argumentMissing("gav1");
        }
        if(gav2 == null) {
            argumentMissing("gav2");
        }
        final Path workDir = Paths.get(workDirArg);
        if(!Files.exists(workDir)) {
            throw new CommandExecutionException(Errors.pathDoesNotExist(workDir));
        }

        if (xmlOnly) {
            try (StringWriter writer = new StringWriter()) {
                final FeaturePackDescription newChildDescr = FeaturePackDependencyBuilder.describeParentAsDependency(
                        workDir, encodingArg, ArtifactCoords.newGav(gav1), ArtifactCoords.newGav(gav2));
                FeaturePackXmlWriter.INSTANCE.write(newChildDescr, writer);
                System.out.println(writer.getBuffer().toString());
            } catch (ProvisioningDescriptionException e) {
                throw new CommandExecutionException("Failed to describe parent as a dependency", e);
            } catch (XMLStreamException e) {
                throw new CommandExecutionException("Failed to write XML", e);
            } catch (IOException e) {
                throw new CommandExecutionException("Failed to write XML", e);
            }
        } else {
            try {
                FeaturePackDependencyBuilder.extractParentAsDependency(workDir, encodingArg, ArtifactCoords.newGav(gav1), ArtifactCoords.newGav(gav2));
            } catch (ProvisioningDescriptionException e) {
                throw new CommandExecutionException("Failed to extract parent as a dependency", e);
            }
        }
    }

    private void installAction() throws CommandExecutionException {

        Path installDir = null;
        WfInstallationDescription wfDescr = null;
        if (installDirArg != null) {
            installDir = Paths.get(installDirArg);
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final InputStream wfInstallDef = cl.getResourceAsStream(WF_FP_DEF_XML);
            if (wfInstallDef == null) {
                throw new CommandExecutionException(WF_FP_DEF_XML + " not found");
            }
            try {
                wfDescr = new WFInstallationDefParser().parse(wfInstallDef);
            } catch (XMLStreamException e) {
                throw new CommandExecutionException("failed to parse " + WF_FP_DEF_XML, e);
            }
        }

        final Path workDir;
        final boolean deleteWorkDir;
        if(workDirArg != null) {
            workDir = Paths.get(workDirArg);
            deleteWorkDir = false;
        } else if (installDir == null) {
            throw new CommandExecutionException(INSTALL_DIR_ARG_NAME + " and/or " + WORK_DIR_ARG_NAME + " are missing");
        } else {
            workDir = IoUtils.createRandomTmpDir();
            deleteWorkDir = true;
        }

        try {
            if (wfDescr != null) {
                WfFeaturePackLayoutBuilder layoutBuilder = new WfFeaturePackLayoutBuilder();
                try {
                    layoutBuilder.build(wfDescr, installDir, workDir);
                } catch (ProvisioningDescriptionException e) {
                    throw new CommandExecutionException("Failed to layout feature packs", e);
                }
            }
            installLayout(workDir);
        } finally {
            if (deleteWorkDir) {
                IoUtils.recursiveDelete(workDir);
            }
        }
    }

    private void installLayout(final Path workDir) throws CommandExecutionException {

        final RepositorySystem repoSystem = Util.newRepositorySystem();
        final RepositorySystemSession repoSession = Util.newRepositorySession(repoSystem);
        try {
            repoSystem.install(repoSession, MavenPluginUtil.getInstallLayoutRequest(workDir));
        } catch (InstallationException | IOException e) {
            throw new CommandExecutionException("Failed to install layout", e);
        }
    }

    private static void argumentMissing(String argumentName) throws CommandExecutionException {
        throw new CommandExecutionException(argumentName + " argument is missing");
    }
}
