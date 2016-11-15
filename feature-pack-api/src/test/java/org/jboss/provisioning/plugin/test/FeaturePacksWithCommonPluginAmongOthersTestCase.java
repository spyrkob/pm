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

package org.jboss.provisioning.plugin.test;

import java.io.IOException;
import java.nio.file.Path;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.state.ProvisionedFeaturePack;
import org.jboss.provisioning.state.ProvisionedState;
import org.jboss.provisioning.test.PmProvisionConfigTestBase;
import org.jboss.provisioning.test.util.fs.state.DirState;
import org.jboss.provisioning.test.util.fs.state.DirState.DirBuilder;
import org.jboss.provisioning.test.util.repomanager.FeaturePackRepoManager;
import org.jboss.provisioning.test.util.repomanager.ProvisioningPluginInstaller;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePacksWithCommonPluginAmongOthersTestCase extends PmProvisionConfigTestBase {

    public static class PluginA extends BasicFileWritingPlugin {
        @Override
        protected String getBasePath() {
            return "pluginA";
        }

        @Override
        protected String getContent() {
            return "pluginA";
        }
    }

    public static class PluginB extends BasicFileWritingPlugin {
        @Override
        protected String getBasePath() {
            return "pluginB";
        }

        @Override
        protected String getContent() {
            return "pluginB";
        }
    }

    public static class PluginC extends BasicFileWritingPlugin {
        @Override
        protected String getBasePath() {
            return "pluginC";
        }

        @Override
        protected String getContent() {
            return "pluginC";
        }
    }

    @Override
    protected void setupRepo(FeaturePackRepoManager repoManager) throws ProvisioningDescriptionException {
        repoManager.installer()
                .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp1", "1.0.0.Final"))
                    .addDependency(FeaturePackConfig.forGav(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final")))
                    .newPackage("p1", true)
                        .writeContent("fp1/p1.txt", "p1")
                        .getFeaturePack()
                    .addPlugIn("org.jboss.pm.plugin.test:plugin1:1.0")
                    .getInstaller()
                .newFeaturePack(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                    .newPackage("p1", true)
                        .writeContent("fp2/p1.txt", "p1")
                        .getFeaturePack()
                    .addPlugIn("org.jboss.pm.plugin.test:plugin2:1.0")
                    .getInstaller()
                .install();
    }

    @Override
    protected void installPlugins(Path repoHome) throws IOException {
        ProvisioningPluginInstaller.forCoords("org.jboss.pm.plugin.test:plugin1:1.0")
            .addClass(BasicFileWritingPlugin.class)
            .addPlugin(PluginA.class)
            .addPlugin(PluginB.class)
            .install(repoHome);
        ProvisioningPluginInstaller.forCoords("org.jboss.pm.plugin.test:plugin2:1.0")
            .addClass(BasicFileWritingPlugin.class)
            .addPlugin(PluginC.class)
            .addPlugin(PluginB.class)
            .install(repoHome);

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
                .addFeaturePack(ProvisionedFeaturePack.builder(ArtifactCoords.newGav("org.jboss.pm.test", "fp2", "2.0.0.Final"))
                        .addPackage("p1")
                        .build())
                .build();
    }

    @Override
    protected DirState provisionedHomeDir(DirBuilder builder) {
        return builder
                .addFile("fp1/p1.txt", "p1")
                .addFile("fp2/p1.txt", "p1")
                .addFile("pluginA", "pluginA")
                .addFile("pluginB", "pluginB")
                .addFile("pluginC", "pluginC")
                .build();
    }
}
