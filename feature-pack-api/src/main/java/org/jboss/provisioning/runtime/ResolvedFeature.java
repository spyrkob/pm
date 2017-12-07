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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.state.ProvisionedFeature;
import org.jboss.provisioning.type.FeatureParameterType;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeature extends CapabilityProvider implements ProvisionedFeature {

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
    Map<String, Object> params;
    Map<ResolvedFeatureId, FeatureDependencySpec> deps;

    private byte orderingState = FREE;
    private byte batchControl;

    ResolvedFeature(ResolvedFeatureId id, ResolvedFeatureSpec spec, int includeNo) {
        this.includeNo = includeNo;
        this.id = id;
        this.spec = spec;
        initParamsFromId();
    }

    ResolvedFeature(ResolvedFeatureId id, ResolvedFeatureSpec spec, Map<String, Object> params, Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps, int includeNo)
            throws ProvisioningException {
        this.includeNo = includeNo;
        this.id = id;
        this.spec = spec;
        this.deps = resolvedDeps;
        initParamsFromId();
        if (!params.isEmpty()) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                setParam(entry.getKey(), entry.getValue(), true);
            }
        }
    }

    ResolvedFeature copy(int includeNo) throws ProvisioningException {
        return new ResolvedFeature(id, spec, params.size() > 1 ? new HashMap<>(params) : params, deps.size() > 1 ? new LinkedHashMap<>(deps) : deps, includeNo);
    }

    private void initParamsFromId() {
        if(id != null) {
            if(id.params.size() == 1) {
                this.params = id.params;
            } else {
                this.params = new HashMap<>(id.params);
            }
        } else {
            this.params = Collections.emptyMap();
        }
    }

    void validate() throws ProvisioningDescriptionException {
        for(FeatureParameterSpec param : spec.xmlSpec.getParams().values()) {
            if(!param.isNillable()) {
                if(!params.containsKey(param.getName())) {
                    if(param.hasDefaultValue()) {
                        params = PmCollections.put(params, param.getName(), param.getDefaultValue());
                    } else {
                        throw new ProvisioningDescriptionException(Errors.nonNillableParameterIsNull(this, param.getName()));
                    }
                }
            } else if(param.hasDefaultValue() && !params.containsKey(param.getName())) {
                params = PmCollections.put(params, param.getName(), param.getDefaultValue());
            }
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

    void ordered() throws ProvisioningDescriptionException {
        validate(); // may not be the best place for this
        if(orderingState != SCHEDULED) {
            throw new IllegalStateException();
        }
        orderingState = ORDERED;
        provided();
        spec.provided();
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

    public void addDependency(ResolvedFeatureId id, FeatureDependencySpec depSpec) throws ProvisioningDescriptionException {
        if(deps.containsKey(id)) {
            throw new ProvisioningDescriptionException("Duplicate dependency on " + id + " from " + this.id); // TODO
        }
        deps = PmCollections.putLinked(deps, id, depSpec);
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
    public Collection<String> getParamNames() {
        return params.keySet();
    }

    @Override
    public Object getResolvedParam(String name) {
        return params.get(name);
    }

    @Override
    public String getConfigParam(String name) throws ProvisioningException {
        return spec.paramToString(name, params.get(name));
    }

    @Override
    public Map<String, Object> getResolvedParams() {
        return params;
    }

    void setParam(String name, Object value, boolean overwrite) throws ProvisioningException {
        if(id != null) {
            final Object idValue = id.params.get(name);
            if(idValue != null) {
                if(!idValue.equals(value)) {
                    throw new ProvisioningDescriptionException("ID parameter " + name + "=" + idValue + " can't be reset to " + value);
                }
                return;
            }
        }
        if(!spec.xmlSpec.hasParam(name)) {
            throw new ProvisioningDescriptionException(Errors.unknownFeatureParameter(spec.id, name));
        }
        final Object prevValue = params.get(name);
        if(prevValue == null) {
            params = PmCollections.put(params, name, value);
            return;
        }
        final FeatureParameterType valueType = spec.getTypeForParameter(name);
        if(valueType.isMergeable()) {
            params = PmCollections.put(params, name, overwrite ? valueType.merge(prevValue, value) : valueType.merge(value, prevValue));
        } else if(overwrite) {
            params = PmCollections.put(params, name, value);
        }
    }

    void merge(ResolvedFeature other, boolean overwriteParams) throws ProvisioningException {
        merge(other.deps, other.getResolvedParams(), overwriteParams);
    }

    void merge(Map<ResolvedFeatureId, FeatureDependencySpec> deps, Map<String, Object> resolvedParams, boolean overwriteParams) throws ProvisioningException {
        if(!resolvedParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : resolvedParams.entrySet()) {
                setParam(entry.getKey(), entry.getValue(), overwriteParams);
            }
        }
        if(!deps.isEmpty()) {
            for(Map.Entry<ResolvedFeatureId, FeatureDependencySpec> dep : deps.entrySet()) {
                addDependency(dep.getKey(), dep.getValue());
            }
        }
    }

    List<ResolvedFeatureId> resolveRefs() throws ProvisioningException {
        return spec.resolveRefs(this);
    }
}