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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.MessageWriter;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.feature.FeatureAnnotation;
import org.jboss.provisioning.plugin.ProvisionedConfigHandler;
import org.jboss.provisioning.runtime.ResolvedFeatureSpec;
import org.jboss.provisioning.state.ProvisionedFeature;

/**
 *
 * @author Alexey Loubyansky
 */
class WfProvisionedConfigHandler implements ProvisionedConfigHandler {

    private class ManagedOp {
        String line;
        String addrPref;
        String name;
        List<String> addrParams = Collections.emptyList();
        List<String> opParams = Collections.emptyList();

        void reset() {
            line = null;
            addrPref = null;
            name = null;
            addrParams = Collections.emptyList();
            opParams = Collections.emptyList();
        }

        void toCommandLine(ProvisionedFeature feature) throws ProvisioningDescriptionException {
            final String line;
            if (this.line != null) {
                line = this.line;
            } else {
                final StringBuilder buf = new StringBuilder();
                if (addrPref != null) {
                    buf.append(addrPref);
                }
                for (String param : addrParams) {
                    final String value = feature.getParam(param);
                    if (value == null) {
                        continue;
                    }
                    buf.append('/').append(param).append('=').append(value);
                }
                buf.append(':').append(name);
                if (!opParams.isEmpty()) {
                    boolean comma = false;
                    for (String param : opParams) {
                        final String value = feature.getParam(param);
                        if (value == null) {
                            continue;
                        }
                        if (comma) {
                            buf.append(',');
                        } else {
                            comma = true;
                            buf.append('(');
                        }
                        buf.append(param).append("=\"").append(value).append('"');
                    }
                    if (comma) {
                        buf.append(')');
                    }
                }
                line = buf.toString();
            }
            messageWriter.print("      " + line);
        }
    }

    private final MessageWriter messageWriter;

    private int opsTotal;
    private ManagedOp[] ops = new ManagedOp[]{new ManagedOp()};

    WfProvisionedConfigHandler(MessageWriter messageWriter) {
        this.messageWriter = messageWriter;
    }

    public void nextFeaturePack(ArtifactCoords.Gav fpGav) throws ProvisioningException {
        messageWriter.print("  " + fpGav);
    }

    public void nextSpec(ResolvedFeatureSpec spec) throws ProvisioningException {
        messageWriter.print("    SPEC " + spec.getName());
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
            messageWriter.print("      Annotation: " + annotation);
            final ManagedOp mop = ops[i++];
            mop.reset();
            mop.line = annotation.getElem(WfConstants.LINE);
            if(mop.line != null) {
                continue;
            }
            mop.name = annotation.getName();
            mop.addrPref = annotation.getElem(WfConstants.ADDR_PREF);
            String elemValue = annotation.getElem(WfConstants.ADDR_PARAMS);
            if (elemValue == null) {
                throw new ProvisioningException("Required element " + WfConstants.ADDR_PARAMS + " is missing for " + spec.getId());
            }

            try {
                mop.addrParams = parseList(elemValue);
            } catch (ProvisioningDescriptionException e) {
                throw new ProvisioningDescriptionException("Saw empty parameter name in annotation " + WfConstants.ADDR_PARAMS + "="
                        + elemValue + " of " + spec.getId());
            }

            elemValue = annotation.getAttr(WfConstants.OP_PARAMS, WfConstants.PM_UNDEFINED);
            if (elemValue == null) {
                mop.opParams = Collections.emptyList();
            } else if (WfConstants.PM_UNDEFINED.equals(elemValue)) {
                if (spec.hasParams()) {
                    final Set<String> allParams = spec.getParamNames();
                    switch (allParams.size() - mop.addrParams.size()) {
                        case 0:
                            mop.opParams = Collections.emptyList();
                            break;
                        case 1:
                            mop.opParams = null;
                            final Iterator<String> allI = allParams.iterator();
                            while (allI.hasNext() && mop.opParams == null) {
                                final String paramName = allI.next();
                                if (!mop.addrParams.contains(paramName)) {
                                    mop.opParams = Collections.singletonList(paramName);
                                    break;
                                }
                            }
                            break;
                        default:
                            if(mop.name.equals(WfConstants.WRITE_ATTRIBUTE)) {
                                throw new ProvisioningDescriptionException(WfConstants.OP_PARAMS + " element of "
                                        + WfConstants.WRITE_ATTRIBUTE + " annotation of " + spec.getId()
                                        + " contains more than one parameter: " + annotation);
                            }
                            mop.opParams = new ArrayList<>(allParams.size() - mop.addrParams.size());
                            for (String paramName : allParams) {
                                if (!mop.addrParams.contains(paramName)) {
                                    mop.opParams.add(paramName);
                                }
                            }
                    }
                } else {
                    mop.opParams = Collections.emptyList();
                }
            } else {
                try {
                    mop.opParams = parseList(elemValue);
                } catch (ProvisioningDescriptionException e) {
                    throw new ProvisioningDescriptionException("Saw empty parameter name in note " + WfConstants.ADDR_PARAMS
                            + "=" + elemValue + " of " + spec.getId());
                }
                if (mop.opParams.size() > 1 && mop.name.equals(WfConstants.WRITE_ATTRIBUTE)) {
                    throw new ProvisioningDescriptionException(WfConstants.OP_PARAMS + " element of "
                            + WfConstants.WRITE_ATTRIBUTE + " annotation of " + spec.getId()
                            + " contains more than one parameter: " + annotation);
                }
            }
        }
    }

    public void nextFeature(ProvisionedFeature feature) throws ProvisioningException {
        if (opsTotal == 0) {
            messageWriter.print("      " + feature.getParams());
            return;
        }
        for(int i = 0; i < opsTotal; ++i) {
            ops[i].toCommandLine(feature);
        }
    }

    private List<String> parseList(String str) throws ProvisioningDescriptionException {
        if (str.isEmpty()) {
            return Collections.emptyList();
        }
        int comma = str.indexOf(',');
        if (comma < 1) {
            return Collections.singletonList(str);
        }
        List<String> list = new ArrayList<>();
        int start = 0;
        while (comma > 0) {
            final String paramName = str.substring(start, comma);
            if (paramName.isEmpty()) {
                throw new ProvisioningDescriptionException("Saw list item in note '" + str);
            }
            list.add(paramName);
            start = comma + 1;
            comma = str.indexOf(',', start);
        }
        if (start == str.length()) {
            throw new ProvisioningDescriptionException("Saw list item in note '" + str);
        }
        list.add(str.substring(start));
        return list;
    }
}
