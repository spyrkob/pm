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

package org.jboss.provisioning.plugin.test;

import java.io.IOException;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.plugin.ProvisioningPlugin;
import org.jboss.provisioning.repomanager.FeaturePackRepositoryManager;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class MultiplePluginsTestCase extends PmProvisionConfigTestBase {

    public static class Plugin1 implements ProvisioningPlugin {
        @Override
        public void postInstall(ProvisioningRuntime ctx) throws ProvisioningException {
            try {
                writeFile(ctx);
            } catch (IOException e) {
                throw new ProvisioningException("Failed to write a file");
            }
        }

        protected void writeFile(ProvisioningRuntime ctx) throws IOException {
            writeFile(ctx, "plugin1.txt", "plugin1");
        }

        protected void writeFile(ProvisioningRuntime ctx, final String path, final String content) throws IOException {
            IoUtils.writeFile(ctx.getStagedDir().resolve(path), content);
        }
    }

    public static class Plugin2 extends Plugin1 {
        @Override
        protected void writeFile(ProvisioningRuntime ctx) throws IOException {
            writeFile(ctx, "plugin2.txt", "plugin2");
        }
    }

    public static class Plugin3 extends Plugin1 {
        @Override
        protected void writeFile(ProvisioningRuntime ctx) throws IOException {
            writeFile(ctx, "plugin3.txt", "plugin3");
        }
    }

    @Override
    protected void setupRepo(FeaturePackRepositoryManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
            .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                .newPackage("p1", true)
                    .writeContent("fp1/p1.txt", "p1")
                    .getFeaturePack()
                .addPlugin(Plugin1.class)
                .addPlugin(Plugin2.class)
                .addPlugin(Plugin3.class)
                .getInstaller()
            .install();
    }

    @Override
    protected ProvisioningConfig provisioningConfig()
            throws ProvisioningDescriptionException {
        return ProvisioningConfig.builder()
                .addFeaturePack(
                        FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final")))
                .build();
    }

    @Override
    protected ProvisionedState provisionedState() {
        return ProvisionedState.builder()
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                        .addPackage("p1")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/p1.txt", "p1")
                .addFile("plugin1.txt", "plugin1")
                .addFile("plugin2.txt", "plugin2")
                .addFile("plugin3.txt", "plugin3")
                .build();
    }
}
