/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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

import static org.jboss.provisioning.Constants.PM_UNDEFINED;
import static org.jboss.provisioning.plugin.wildfly.WfConstants.ADDR_PARAMS;
import static org.jboss.provisioning.plugin.wildfly.WfConstants.ADDR_PARAMS_MAPPING;
import static org.jboss.provisioning.plugin.wildfly.WfConstants.OP_PARAMS;
import static org.jboss.provisioning.plugin.wildfly.WfConstants.OP_PARAMS_MAPPING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.provisioning.ArtifactCoords.Gav;

import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.ConfigId;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.diff.FileSystemDiff;
import org.jboss.provisioning.plugin.DiffPlugin;
import org.jboss.provisioning.runtime.FeaturePackRuntime;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.runtime.ResolvedFeatureSpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.util.PathFilter;
import org.jboss.provisioning.xml.ConfigXmlWriter;

/**
 * WildFly plugin to compute the model difference between an instance and a clean provisioned instance.
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class WfDiffPlugin implements DiffPlugin {

    private static final String CONFIGURE_SYNC = "/synchronization=simple:add(host=%s, port=%s, protocol=%s, username=%s, password=%s)";
    private static final String EXPORT_DIFF = "attachment save --overwrite --operation=/synchronization=simple:export-diff --file=%s";
    private static final String EXPORT_FEATURE = "attachment save --overwrite --operation=/synchronization=simple:feature-diff --file=%s";

    private static final PathFilter FILTER_FP = PathFilter.Builder.instance()
            .addDirectories("*" + File.separatorChar + "tmp", "*" + File.separatorChar + "log", "*_xml_history", "model_diff")
            .addFiles("standalone.xml", "process-uuid", "logging.properties")
            .build();

    private static final PathFilter FILTER = PathFilter.Builder.instance()
            .addDirectories("*" + File.separatorChar + "tmp", "model_diff")
            .addFiles("standalone.xml", "logging.properties")
            .build();

    @Override
    public void computeDiff(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException {
        final MessageWriter messageWriter = runtime.getMessageWriter();
        messageWriter.verbose("WildFly diff plug-in");
        FileSystemDiff diff = new FileSystemDiff(messageWriter, runtime.getInstallDir(), customizedInstallation);
        String host = getParameter(runtime, "host", "127.0.0.1");
        String port = getParameter(runtime, "port", "9990");
        String protocol = getParameter(runtime, "protocol", "remote+http");
        String username = getParameter(runtime, "username", "admin");
        String password = getParameter(runtime, "password", "passw0rd!");
        String serverConfig = getParameter(runtime, "server-config", "standalone.xml");
        Server server = new Server(customizedInstallation.toAbsolutePath(), serverConfig, messageWriter);
        EmbeddedServer embeddedServer = new EmbeddedServer(runtime.getInstallDir().toAbsolutePath(), messageWriter);
        try {
            Files.createDirectories(target);
            server.startServer();
            embeddedServer.execute(false,
                    String.format(CONFIGURE_SYNC, host, port, protocol, username, password),
                    String.format(EXPORT_DIFF, target.resolve("finalize.cli").toAbsolutePath()),
                    String.format(EXPORT_FEATURE, target.resolve("feature_config.dmr").toAbsolutePath()));
            ConfigModel.Builder configBuilder = ConfigModel.builder().setName("standalone.xml").setModel("standalone");
            Map<Gav, ConfigId> includedConfigs = new HashMap<>();
            createConfiguration(runtime, configBuilder, includedConfigs, target.resolve("feature_config.dmr").toAbsolutePath());
            ConfigModel config = configBuilder.build();
            ConfigXmlWriter.getInstance().write(config, target.resolve("config.xml"));
            WfDiffResult result = new WfDiffResult(
                    includedConfigs,
                    Collections.singletonList(config),
//                    Collections.singletonList(target.resolve("finalize.cli").toAbsolutePath()),
                    Collections.emptyList(),
                    diff.diff(getFilter(runtime)));
            runtime.setDiff(result.merge(runtime.getDiff()));
        } catch (IOException | XMLStreamException ex) {
            messageWriter.error(ex, "Couldn't compute the WildFly Model diff because of %s", ex.getMessage());
            Logger.getLogger(WfDiffPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            server.stopServer();
        }
    }

    private String getDefaultName(Gav gav) {
        StringJoiner buf = new StringJoiner(":");
        if (gav.getGroupId() != null) {
            buf.add(gav.getGroupId());
        }
        if (gav.getArtifactId() != null) {
            buf.add(gav.getArtifactId());
        }
        return buf.toString();
    }

    private PathFilter getFilter(ProvisioningRuntime runtime) {
        if ("diff-to-feature-pack".equals(runtime.getOperation())) {
            return FILTER_FP;
        }
        return FILTER;
    }

    private void createConfiguration(ProvisioningRuntime runtime, ConfigModel.Builder builder,
            Map<Gav, ConfigId> includedConfigBuilders, Path json)
            throws IOException, XMLStreamException, ProvisioningDescriptionException {
        try (InputStream in = Files.newInputStream(json)) {
            ModelNode featureDiff = ModelNode.fromBase64(in);
            for (ModelNode feature : featureDiff.asList()) {
                String specName = feature.require("feature").require("spec").asString();
                DependencySpec dependencySpec = getFeatureSpec(runtime, specName);
                FeatureSpec resolvedSpec = dependencySpec.spec;
                if (resolvedSpec != null && resolvedSpec.hasAnnotations()) {
                    Map<String, String> address = new HashMap<>();
                    for (Property elt : feature.require("feature").require("address").asPropertyList()) {
                        address.put(elt.getName(), elt.getValue().asString());
                    }
                    FeatureConfig featureConfig = FeatureConfig.newConfig(specName).setOrigin(dependencySpec.fpName);
                    resolveAddressParams(featureConfig, address, resolvedSpec.getAnnotations().get(0));
                    Map<String, String> params = new HashMap<>();
                    if (feature.require("feature").hasDefined("params")) {
                        for (Property elt : feature.require("feature").require("params").asPropertyList()) {
                            params.put(elt.getName(), elt.getValue().asString());
                        }
                        resolveParams(featureConfig, params, resolvedSpec.getAnnotations().get(0));
                    }
                    if (feature.require("feature").require("exclude").asBoolean()) {
                        if (!includedConfigBuilders.containsKey(dependencySpec.gav)) {
                            includedConfigBuilders.put(dependencySpec.gav, new ConfigId("standalone", "standalone.xml"));
                        }
                        FeatureId.Builder idBuilder = FeatureId.builder(specName);
                        for(FeatureParameterSpec fparam : resolvedSpec.getIdParams()) {
                            idBuilder.setParam(fparam.getName(), featureConfig.getParam(fparam.getName()));
                        }
                        builder.excludeFeature(dependencySpec.fpName, idBuilder.build());
                    } else {
                        builder.addFeature(featureConfig);
                    }
                }
            }
        }
    }

    private DependencySpec getFeatureSpec(ProvisioningRuntime runtime, String name) throws ProvisioningDescriptionException {
        for (FeaturePackRuntime fp : runtime.getFeaturePacks()) {
            ResolvedFeatureSpec spec = fp.getResolvedFeatureSpec(name);
            if (spec != null) {
                return new DependencySpec(getDefaultName(fp.getSpec().getGav()), fp.getGav(), spec.getSpec());
            }
        }
        for (FeaturePackRuntime fp : runtime.getFeaturePacks()) {
            FeatureSpec spec = fp.getFeatureSpec(name);
            if (spec != null) {
                return new DependencySpec(getDefaultName(fp.getSpec().getGav()), fp.getGav(), spec);
            }
        }
        return null;
    }

    private void resolveAddressParams(FeatureConfig featureConfig, Map<String, String> address, FeatureAnnotation annotation) {
        List<String> addressParams = annotation.getElemAsList(ADDR_PARAMS);
        List<String> addressParamMappings = annotation.getElemAsList(ADDR_PARAMS_MAPPING);
        if (addressParamMappings == null || addressParamMappings.isEmpty()) {
            addressParamMappings = addressParams;
        }
        for (int i = 0; i < addressParams.size(); i++) {
            String value = address.get(addressParams.get(i));
            if (value != null) {
                if("undefined".equals(value)) {
                    value = PM_UNDEFINED;
                }
                featureConfig.putParam(addressParamMappings.get(i), value);
            }
        }
    }

    private void resolveParams(FeatureConfig featureConfig, Map<String, String> params, FeatureAnnotation annotation) {
        List<String> addressParams = annotation.getElemAsList(OP_PARAMS);
        List<String> addressParamMappings = annotation.getElemAsList(OP_PARAMS_MAPPING);
        if (addressParamMappings == null || addressParamMappings.isEmpty()) {
            addressParamMappings = addressParams;
        }
        for (int i = 0; i < addressParams.size(); i++) {
            String value = params.get(addressParams.get(i));
            if (value != null) {
                if("undefined".equals(value)) {
                    value = PM_UNDEFINED;
                }
                featureConfig.putParam(addressParamMappings.get(i), value);
            }
        }
    }

    private static class DependencySpec {

        private final String fpName;
        private final Gav gav;
        private final FeatureSpec spec;

        DependencySpec(String fpName, Gav gav, FeatureSpec spec) {
            this.fpName = fpName;
            this.spec = spec;
            this.gav = gav;
        }
    }
}
