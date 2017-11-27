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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.runtime.ProvisioningRuntime;
import org.jboss.provisioning.runtime.ResolvedFeatureSpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.state.ProvisionedConfig;
import org.jboss.provisioning.state.ProvisionedFeature;
import org.jboss.provisioning.util.IoUtils;

/**
 *
 * @author Alexey Loubyansky
 */
class WfProvisionedConfigHandler implements ProvisionedConfigHandler {

    private static final String DOMAIN = "domain";
    private static final String HOST = "host";
    private static final String STANDALONE = "standalone";

    private static final String CONFIG_NAME = "config-name";
    private static final String DOMAIN_CONFIG_NAME = "domain-config-name";
    private static final String HOST_CONFIG_NAME = "host-config-name";

    private static final String TMP_DOMAIN_XML = "pm-tmp-domain.xml";
    private static final String TMP_HOST_XML = "pm-tmp-host.xml";

    private static final int OP = 0;
    private static final int WRITE_ATTR = 1;
    private static final int LIST_ADD = 2;

    private static final ArtifactCoords.Ga WF_CORE_GA = ArtifactCoords.newGa("org.wildfly.core", "wildfly-core-feature-pack-new");
    private static final byte LOOK_FOR_HOST = 1;
    private static final byte LOOK_FOR_HOST_IN_SPEC = 1;

    private interface NameFilter {
        boolean accepts(String name);
    }

    private class ManagedOp {
        String line;
        String addrPref;
        String name;
        List<String> addrParams = Collections.emptyList();
        List<String> opParams = Collections.emptyList();
        int op;

        void reset() {
            line = null;
            addrPref = null;
            name = null;
            addrParams = Collections.emptyList();
            opParams = Collections.emptyList();
            op = OP;
        }

        String toCommandLine(ProvisionedFeature feature) throws ProvisioningException {
            final String line;
            if (this.line != null) {
                line = this.line;
            } else {
                final StringBuilder buf = new StringBuilder();
                if (addrPref != null) {
                    buf.append(addrPref);
                }
                int i = 0;
                while(i < addrParams.size()) {
                    final String value = feature.getConfigParam(addrParams.get(i++));
                    if (value == null) {
                        continue;
                    }
                    buf.append('/').append(addrParams.get(i++)).append('=').append(value);

                }
                buf.append(':').append(name);
                switch(op) {
                    case OP: {
                        if (!opParams.isEmpty()) {
                            boolean comma = false;
                            i = 0;
                            while(i < opParams.size()) {
                                final String value = feature.getConfigParam(opParams.get(i++));
                                if (value == null) {
                                    continue;
                                }
                                if (comma) {
                                    buf.append(',');
                                } else {
                                    comma = true;
                                    buf.append('(');
                                }
                                buf.append(opParams.get(i++)).append('=');
                                if(value.trim().isEmpty()) {
                                    buf.append('\"').append(value).append('\"');
                                } else {
                                    buf.append(value);
                                }
                            }
                            if (comma) {
                                buf.append(')');
                            }
                        }
                        break;
                    }
                    case WRITE_ATTR: {
                        final String value = feature.getConfigParam(opParams.get(0));
                        if (value == null) {
                            throw new ProvisioningDescriptionException(opParams.get(0) + " parameter is null: " + feature);
                        }
                        buf.append("(name=").append(opParams.get(1)).append(",value=").append(value).append(')');
                        break;
                    }
                    case LIST_ADD: {
                        final String value = feature.getConfigParam(opParams.get(0));
                        if (value == null) {
                            throw new ProvisioningDescriptionException(opParams.get(0) + " parameter is null: " + feature);
                        }
                        buf.append("(name=").append(opParams.get(1)).append(",value=").append(value).append(')');
                        break;
                    }
                    default:

                }
                line = buf.toString();
            }
            return line;
        }
    }

    private final ProvisioningRuntime runtime;
    private final MessageWriter messageWriter;

    private int opsTotal;
    private ManagedOp[] ops = new ManagedOp[]{new ManagedOp()};
    private NameFilter paramFilter;

    private StringBuilder embedBuf = new StringBuilder();
    private String scriptName;
    private String tmpConfig;
    private List<String> opList = new ArrayList<>();

    private String hostName;
    private byte lookForHost;

    WfProvisionedConfigHandler(ProvisioningRuntime runtime) {
        this.runtime = runtime;
        this.messageWriter = runtime.getMessageWriter();
    }

    private void reset() {
        embedBuf.setLength(0);
        scriptName = null;
        opList.clear();
        hostName = null;
        lookForHost = 0;
    }

    private void writeOp(String op) throws ProvisioningException {
        opList.add(op);
    }

    @Override
    public void prepare(ProvisionedConfig config) throws ProvisioningException {
        reset();
        final String logFile;
        if(STANDALONE.equals(config.getModel())) {
            logFile = config.getProperties().get(CONFIG_NAME);
            if(logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel() + " is missing property config-name");
            }

            embedBuf.append("embed-server --admin-only=true --empty-config --remove-existing --server-config=")
            .append(logFile).append(" --jboss-home=").append(runtime.getStagedDir());

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name) {
                    return !("profile".equals(name) || HOST.equals(name));
                }
            };
        } else if(DOMAIN.equals(config.getModel())) {
            logFile = config.getProperties().get(DOMAIN_CONFIG_NAME);
            if (logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel()
                        + " is missing property domain-config-name");
            }

            String hostConfig = config.getProperties().get(HOST_CONFIG_NAME);
            if(hostConfig == null) {
                tmpConfig = TMP_HOST_XML;
                hostConfig = TMP_HOST_XML;
            }

            embedBuf.append(
                    "embed-host-controller --empty-host-config --remove-existing-host-config --empty-domain-config --remove-existing-domain-config --host-config=")
                    .append(hostConfig).append(" --domain-config=").append(logFile).append(" --jboss-home=")
                    .append(runtime.getStagedDir());

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name) {
                    return !HOST.equals(name);
                }
            };
        } else if (HOST.equals(config.getModel())) {
            logFile = config.getProperties().get(HOST_CONFIG_NAME);
            if (logFile == null) {
                throw new ProvisioningException("Config " + config.getName() + " of model " + config.getModel()
                        + " is missing property host-config-name");
            }
            lookForHost = LOOK_FOR_HOST;

            embedBuf.append("embed-host-controller --empty-host-config --remove-existing-host-config --host-config=")
                    .append(logFile);
            final String domainConfig = config.getProperties().get(DOMAIN_CONFIG_NAME);
            if (domainConfig == null) {
                tmpConfig = TMP_DOMAIN_XML;
                embedBuf.append(" --empty-domain-config --remove-existing-domain-config --domain-config=")
                        .append(TMP_DOMAIN_XML);
            } else {
                embedBuf.append(" --domain-config=").append(domainConfig);
            }
            embedBuf.append(" --jboss-home=").append(runtime.getStagedDir());

            paramFilter = new NameFilter() {
                @Override
                public boolean accepts(String name) {
                    return !"profile".equals(name);
                }
            };
        } else {
            throw new ProvisioningException("Unsupported config model " + config.getModel());
        }
        scriptName = logFile;
    }

    @Override
    public void nextFeaturePack(ArtifactCoords.Gav fpGav) throws ProvisioningException {
        messageWriter.verbose("  " + fpGav);
    }

    @Override
    public void nextSpec(ResolvedFeatureSpec spec) throws ProvisioningException {
        messageWriter.verbose("    SPEC " + spec.getName());
        if(lookForHost == LOOK_FOR_HOST && HOST.equals(spec.getName()) && spec.getId().getGav().toGa().equals(WF_CORE_GA)) {
            lookForHost = LOOK_FOR_HOST_IN_SPEC;
        }
        if(!spec.hasAnnotations()) {
            opsTotal = 0;
            return;
        }

        final List<FeatureAnnotation> annotations = spec.getAnnotations();
        opsTotal = annotations.size();
        if(annotations.size() > 1) {
            if(ops.length < opsTotal) {
                final ManagedOp[] tmp = ops;
                ops = new ManagedOp[opsTotal];
                System.arraycopy(tmp, 0, ops, 0, tmp.length);
                for(int i = tmp.length; i < ops.length; ++i) {
                    ops[i] = new ManagedOp();
                }
            }
        }

        int i = 0;
        while (i < opsTotal) {
            final FeatureAnnotation annotation = annotations.get(i);
            messageWriter.verbose("      Annotation: " + annotation);
            final ManagedOp mop = ops[i++];
            mop.reset();
            mop.line = annotation.getElem(WfConstants.LINE);
            if(mop.line != null) {
                continue;
            }
            mop.name = annotation.getName();
            if(mop.name.equals(WfConstants.ADD)) {
                mop.op = OP;
            } else if(mop.name.equals(WfConstants.WRITE_ATTRIBUTE)) {
                mop.op = WRITE_ATTR;
            } else if(mop.name.equals(WfConstants.LIST_ADD)) {
                mop.op = LIST_ADD;
            } else {
                mop.op = OP;
            }
            mop.addrPref = annotation.getElem(WfConstants.ADDR_PREF);

            String elemValue = annotation.getElem(WfConstants.SKIP_IF_FILTERED);
            final Set<String> skipIfFiltered;
            if (elemValue != null) {
                skipIfFiltered = parseSet(elemValue);
            } else {
                skipIfFiltered = Collections.emptySet();
            }

            final String addrParamMapping = annotation.getElem(WfConstants.ADDR_PARAMS_MAPPING);
            elemValue = annotation.getElem(WfConstants.ADDR_PARAMS);
            if (elemValue == null) {
                throw new ProvisioningException("Required element " + WfConstants.ADDR_PARAMS + " is missing for " + spec.getId());
            }

            try {
                mop.addrParams = parseList(elemValue, paramFilter, skipIfFiltered, addrParamMapping);
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningDescriptionException("Saw an empty parameter name in annotation " + WfConstants.ADDR_PARAMS + "="
                        + elemValue + " of " + spec.getId());
            }
            if(mop.addrParams == null) {
                // skip
                mop.reset();
                --opsTotal;
                --i;
                continue;
            }

            final String paramsMapping = annotation.getElem(WfConstants.OP_PARAMS_MAPPING);

            elemValue = annotation.getElem(WfConstants.OP_PARAMS, Constants.PM_UNDEFINED);
            if (elemValue == null) {
                mop.opParams = Collections.emptyList();
            } else if (Constants.PM_UNDEFINED.equals(elemValue)) {
                if (spec.hasParams()) {
                    final Set<String> allParams = spec.getParamNames();
                    final int opParams = allParams.size() - mop.addrParams.size() / 2;
                    if(opParams == 0) {
                        mop.opParams = Collections.emptyList();
                    } else {
                        mop.opParams = new ArrayList<>(opParams*2);
                        for (String paramName : allParams) {
                            boolean inAddr = false;
                            int j = 0;
                            while(!inAddr && j < mop.addrParams.size()) {
                                if(mop.addrParams.get(j).equals(paramName)) {
                                    inAddr = true;
                                }
                                j += 2;
                            }
                            if (!inAddr) {
                                if(paramFilter.accepts(paramName)) {
                                    mop.opParams.add(paramName);
                                    mop.opParams.add(paramName);
                                } else if(skipIfFiltered.contains(paramName)) {
                                    // skip
                                    mop.reset();
                                    --opsTotal;
                                    --i;
                                    continue;
                                }
                            }
                        }
                    }
                } else {
                    mop.opParams = Collections.emptyList();
                }
            } else {
                try {
                    mop.opParams = parseList(elemValue, paramFilter, skipIfFiltered, paramsMapping);
                } catch (ProvisioningDescriptionException e) {
                    throw new ProvisioningDescriptionException("Saw empty parameter name in note " + WfConstants.ADDR_PARAMS
                            + "=" + elemValue + " of " + spec.getId());
                }
            }

            if(mop.op == WRITE_ATTR && mop.opParams.size() != 2) {
                throw new ProvisioningDescriptionException(WfConstants.OP_PARAMS + " element of "
                        + WfConstants.WRITE_ATTRIBUTE + " annotation of " + spec.getId()
                        + " accepts only one parameter: " + annotation);
            }
        }
    }

    @Override
    public void nextFeature(ProvisionedFeature feature) throws ProvisioningException {
        if(lookForHost == LOOK_FOR_HOST_IN_SPEC) {
            lookForHost = 0;
            hostName = feature.getConfigParam(HOST);
        }
        if (opsTotal == 0) {
            messageWriter.verbose("      " + feature.getResolvedParams());
            return;
        }
        for(int i = 0; i < opsTotal; ++i) {
            final String line = ops[i].toCommandLine(feature);
            messageWriter.verbose("      " + line);
            writeOp(line);
        }
    }

    @Override
    public void startBatch() throws ProvisioningException {
        messageWriter.verbose("      START BATCH");
        writeOp("batch");
    }

    @Override
    public void endBatch() throws ProvisioningException {
        messageWriter.verbose("      END BATCH");
        writeOp("run-batch");
    }

    @Override
    public void done() throws ProvisioningException {
        if(opList.isEmpty()) {
            messageWriter.verbose(" %s configuration is empty", scriptName);
            reset();
            return;
        }
        if(hostName != null) {
            embedBuf.append(" --temp-host-controller-name=").append(hostName);
        }
        final Path script = runtime.getTmpPath("cli", scriptName);
        try {
            Files.createDirectories(script.getParent());
            try(BufferedWriter opsWriter = Files.newBufferedWriter(script)) {
                opsWriter.write(embedBuf.toString());
                opsWriter.newLine();
                for(String op : opList) {
                    opsWriter.write(op);
                    opsWriter.newLine();
                }
            }
        } catch (IOException e) {
            throw new ProvisioningException(Errors.writeFile(script), e);
        }

        messageWriter.verbose(" Generating %s configuration", script.getFileName().toString());
        try {
            CliScriptRunner.runCliScript(runtime.getStagedDir(), script, messageWriter);
        } catch(ProvisioningException e) {
            throw new ProvisioningException("Failed to generate " + script.getFileName() + " configuration", e);
        }
        if(tmpConfig != null) {
            final Path tmpPath = runtime.getStagedDir().resolve(DOMAIN).resolve("configuration").resolve(tmpConfig);
            if(Files.exists(tmpPath)) {
                IoUtils.recursiveDelete(tmpPath);
            } else {
                messageWriter.error("Expected path does not exist " + tmpPath);
            }
            tmpConfig = null;
        }
        reset();
    }

    private static Set<String> parseSet(String str) throws ProvisioningDescriptionException {
        if (str.isEmpty()) {
            return Collections.emptySet();
        }
        int comma = str.indexOf(',');
        if (comma < 1) {
            return Collections.singleton(str.trim());
        }
        final Set<String> set = new HashSet<>();
        int start = 0;
        while (comma > 0) {
            final String paramName = str.substring(start, comma).trim();
            if (paramName.isEmpty()) {
                throw new ProvisioningDescriptionException("Saw an empty list item in note '" + str);
            }
            set.add(paramName);
            start = comma + 1;
            comma = str.indexOf(',', start);
        }
        if (start == str.length()) {
            throw new ProvisioningDescriptionException("Saw an empty list item in note '" + str);
        }
        set.add(str.substring(start).trim());
        return set;
    }

    private static List<String> parseList(String str, NameFilter filter, Set<String> skipIfFiltered, String mappingStr) throws ProvisioningDescriptionException {
        if (str.isEmpty()) {
            return Collections.emptyList();
        }
        int strComma = str.indexOf(',');
        List<String> list = new ArrayList<>();
        if (strComma < 1) {
            str = str.trim();
            final String mapped = mappingStr == null ? str : mappingStr.trim();
            if (filter.accepts(str)) {
                list.add(str);
                list.add(mapped);
            } else if(skipIfFiltered.contains(str)) {
                return null;
            }
            return list;
        }
        int mappingComma = mappingStr == null ? -1 : mappingStr.indexOf(',');
        int mappingStart = mappingComma > 0 ? 0 : -1;
        int start = 0;
        while (strComma > 0) {
            final String paramName = str.substring(start, strComma).trim();
            if (paramName.isEmpty()) {
                throw new ProvisioningDescriptionException("Saw en empty list item in note '" + str + "'");
            }
            final String mappedName;
            if(mappingComma < 0) {
                mappedName = paramName;
            } else {
                mappedName = mappingStr.substring(mappingStart, mappingComma).trim();
                if (mappedName.isEmpty()) {
                    throw new ProvisioningDescriptionException("Saw en empty list item in note '" + mappingStr + "'");
                }
            }

            if (filter.accepts(paramName)) {
                list.add(paramName);
                list.add(mappedName);
            } else if(skipIfFiltered.contains(paramName)) {
                return null;
            }
            start = strComma + 1;
            strComma = str.indexOf(',', start);
            if(mappingComma > 0) {
                mappingStart = mappingComma + 1;
                mappingComma = mappingStr.indexOf(',', mappingStart);
            }
        }
        if (start == str.length()) {
            throw new ProvisioningDescriptionException("Saw an empty list item in note '" + str);
        }
        final String paramName = str.substring(start).trim();
        final String mappedName = mappingStart < 0 ? paramName : mappingStr.substring(mappingStart).trim();
        if(filter.accepts(paramName)) {
            list.add(paramName);
            list.add(mappedName);
        } else if(skipIfFiltered.contains(paramName)) {
            return null;
        }
        return list;
    }
}
