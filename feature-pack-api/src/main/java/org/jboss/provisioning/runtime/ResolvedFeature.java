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
package org.jboss.provisioning.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.state.ProvisionedFeature;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeature implements ProvisionedFeature {

    /*
     * These states are used when the features are being ordered in the config
     */
    private static final byte FREE = 0;
    private static final byte SCHEDULED = 1;
    private static final byte ORDERED = 2;

    private static final byte BATCH_START = 1;
    private static final byte BATCH_END = 2;

    final int includeNo;
    final ResolvedFeatureId id;
    final ResolvedFeatureSpec spec;
    Map<String, String> params;
    Set<ResolvedFeatureId> dependencies;

    private byte orderingState = FREE;
    private byte batchControl;

    ResolvedFeature(ResolvedFeatureId id, ResolvedFeatureSpec spec, Map<String, String> params, Set<ResolvedFeatureId> resolvedDeps, int includeNo) throws ProvisioningDescriptionException {
        this.includeNo = includeNo;
        this.id = id;
        this.spec = spec;
        this.dependencies = resolvedDeps;
        if (!params.isEmpty()) {
            if (!spec.xmlSpec.hasParams()) {
                throw new ProvisioningDescriptionException("Features of type " + spec.id + " don't accept any parameters: " + params);
            }
            if(params.size() > spec.xmlSpec.getParamsTotal()) {
                throw new ProvisioningDescriptionException("Provided parameters " + params.keySet() + " do not match " + spec.id + " parameters " + spec.xmlSpec.getParamNames());
            }
            if(spec.xmlSpec.getParamsTotal() == 1) {
                final Entry<String, String> param = params.entrySet().iterator().next();
                final FeatureParameterSpec paramSpec = spec.xmlSpec.getParam(param.getKey());
                if(paramSpec == null) {
                    throw new ProvisioningDescriptionException("Provided parameters " + params.keySet() + " do not match " + spec.id + " parameters " + spec.xmlSpec.getParamNames());
                }
                this.params = Collections.singletonMap(param.getKey(), param.getValue());
            } else {
                this.params = new HashMap<>(spec.xmlSpec.getParamsTotal());
                if(params.size() != spec.xmlSpec.getParamsTotal()) {
                    for(FeatureParameterSpec pSpec : spec.xmlSpec.getParams()) {
                        if(pSpec.hasDefaultValue()) {
                            this.params.put(pSpec.getName(), pSpec.getDefaultValue());
                        }
                    }
                }
                for(Map.Entry<String, String> param : params.entrySet()) {
                    this.params.put(param.getKey(), param.getValue());
                }
            }
        } else if(spec.xmlSpec.hasParams()) {
            this.params = new HashMap<>(spec.xmlSpec.getParamsTotal());
            for(FeatureParameterSpec pSpec : spec.xmlSpec.getParams()) {
                if(pSpec.hasDefaultValue()) {
                    this.params.put(pSpec.getName(), pSpec.getDefaultValue());
                }
            }
        } else {
            this.params = Collections.emptyMap();
        }
    }

    boolean isFree() {
        return orderingState == FREE;
    }

    boolean isOrdered() {
        return orderingState == ORDERED;
    }

    void schedule() {
        if(orderingState != FREE) {
            throw new IllegalStateException();
        }
        orderingState = SCHEDULED;
    }

    void ordered() {
        if(orderingState != SCHEDULED) {
            throw new IllegalStateException();
        }
        orderingState = ORDERED;
    }

    void free() {
        orderingState = FREE;
    }

    void startBatch() {
        batchControl = BATCH_START;
    }

    void endBatch() {
        batchControl = BATCH_END;
    }

    boolean isBatchStart() {
        return batchControl == BATCH_START;
    }

    boolean isBatchEnd() {
        return batchControl == BATCH_END;
    }

    public void addDependency(ResolvedFeatureId id) {
        if(dependencies.isEmpty()) {
            dependencies = Collections.singleton(id);
            return;
        }
        if(dependencies.contains(id)) {
            return;
        }
        if(dependencies.size() == 1) {
            final ResolvedFeatureId first = dependencies.iterator().next();
            dependencies = new LinkedHashSet<>();
            dependencies.add(first);
        }
        dependencies.add(id);
    }

    @Override
    public boolean hasId() {
        return id != null;
    }

    @Override
    public ResolvedFeatureId getId() {
        return id;
    }

    @Override
    public ResolvedSpecId getSpecId() {
        return spec.id;
    }

    @Override
    public boolean hasParams() {
        return !params.isEmpty();
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public String getParam(String name) throws ProvisioningDescriptionException {
        return params.get(name);
    }

    public void setParam(String name, String value) throws ProvisioningDescriptionException {
        if(id != null) {
            final String idValue = id.params.get(name);
            if(idValue != null) {
                if(!idValue.equals(value)) {
                    throw new ProvisioningDescriptionException("ID parameter " + name + "=" + idValue + " can't be reset to " + value);
                }
                return;
            }
        }
        switch(params.size()) {
            case 0:
                params = Collections.singletonMap(name, value);
                break;
            case 1:
                if(params.containsKey(name)) {
                    params = Collections.singletonMap(name, value);
                    return;
                }
                final Map.Entry<String, String> first = params.entrySet().iterator().next();
                params = new HashMap<>(2);
                params.put(first.getKey(), first.getValue());
            default:
                params.put(name, value);
        }
    }

    List<ResolvedFeatureId> resolveRefs(ConfigModelBuilder configModelBuilder) throws ProvisioningDescriptionException {
        return spec.resolveRefs(this, configModelBuilder);
    }
}