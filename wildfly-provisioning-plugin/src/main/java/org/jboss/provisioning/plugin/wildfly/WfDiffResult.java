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




import static org.jboss.provisioning.plugin.wildfly.config.WildFlyPackageTasksParser.NAMESPACE_2_0;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.diff.FileSystemDiffResult;
import org.jboss.provisioning.repomanager.FeaturePackBuilder;
import org.jboss.provisioning.repomanager.PackageBuilder;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.util.IoUtils;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfDiffResult extends FileSystemDiffResult {
    private final List<Path> scripts;

    public WfDiffResult(List<Path> scripts, Set<Path> deletedFiles, Set<Path> addedFiles, Set<Path> modifiedBinaryFiles, Map<Path, List<String>> unifiedDiffs) {
        super(deletedFiles, addedFiles, modifiedBinaryFiles, unifiedDiffs);
        this.scripts = scripts;
    }

    public WfDiffResult(List<Path> scripts, FileSystemDiffResult result) {
        super(result.getDeletedFiles(), result.getAddedFiles(), result.getModifiedBinaryFiles(), result.getUnifiedDiffs());
        this.scripts = scripts;
    }

    public List<Path> getScripts() {
        return Collections.unmodifiableList(scripts);
    }

    @Override
    public FileSystemDiffResult merge(FileSystemDiffResult result) {
        super.merge(result);
        if(result instanceof WfDiffResult) {
            this.scripts.addAll(((WfDiffResult) result).getScripts());
        }
        return this;
    }

    @Override
    public void toFeaturePack(FeaturePackBuilder fpBuilder, ProvisioningRuntime runtime, Path installationHome) throws ProvisioningException {
        super.toFeaturePack(fpBuilder, runtime, installationHome);
        PackageBuilder updatedFiles = fpBuilder.newPackage("wildfly").setDefault();
        try {
            for (Path src : getScripts()) {
                try {
                    String script = EmbeddedServer.startEmbeddedServerCommand("standalone.xml") + System.lineSeparator()
                            + IoUtils.readFile(src).trim() + System.lineSeparator()
                            + "exit" + System.lineSeparator();
                    fpBuilder.writeResources(WfConstants.WILDFLY + '/' + WfConstants.SCRIPTS + '/' + src.getFileName().toString(), script);
                } catch (IOException ex) {
                    throw new ProvisioningException(ex);
                }
            }
        } catch (Exception ex) {
            runtime.getMessageWriter().error(ex,ex.getMessage());
            throw new ProvisioningException(ex);
        }
        updatedFiles.writeContent("pm/wildfly/tasks.xml", toTasks(), false);
    }

    private String toTasks() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\" ?>");
        buffer.append(System.lineSeparator());
        buffer.append(System.lineSeparator());
        buffer.append(String.format("<tasks xmlns=\"%s\">", NAMESPACE_2_0));
        buffer.append(System.lineSeparator());
        buffer.append("    <delete-paths>");
        buffer.append(System.lineSeparator());
        for (Path deleted : getDeletedFiles()) {
            buffer.append(String.format("        <delete path=\"%s\" recursive=\"%s\" />", deleted.toString(), false));
        }
        buffer.append("    </delete-paths>");
        buffer.append(System.lineSeparator());
        buffer.append("</tasks>");
        return buffer.toString();
    }
}
