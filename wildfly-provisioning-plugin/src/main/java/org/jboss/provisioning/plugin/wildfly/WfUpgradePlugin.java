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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.UpgradePlugin;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.diff.FileSystemMerge;
import org.jboss.provisioning.diff.Strategy;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfUpgradePlugin implements UpgradePlugin {

    @Override
    public void upgrade(ProvisioningRuntime runtime, Path customizedInstallation) throws ProvisioningException {
        try {
            FileSystemMerge fsMerge = FileSystemMerge.Factory.getInstance(Strategy.OURS, runtime.getMessageWriter(),runtime.getInstallDir(), customizedInstallation);
            fsMerge.executeUpdate(runtime.getDiff());
            EmbeddedServer embeddedServer = new EmbeddedServer(runtime.getInstallDir().toAbsolutePath(), runtime.getMessageWriter());
            for(Path script :  ((WfDiffResult)runtime.getDiff()).getScripts()) {
                List<String> lines = Files.readAllLines(script);
                embeddedServer.execute(false, lines);
            }
        } catch (IOException ex) {
            runtime.getMessageWriter().error(ex, "Error upgrading");
            throw new ProvisioningException(ex.getMessage(), ex);
        }
    }
}
