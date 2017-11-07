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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;


/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeatureSpec extends CapabilityProvider {

    final ResolvedSpecId id;
    final FeatureSpec xmlSpec;
    private Map<String, ResolvedFeatureSpec> resolvedRefTargets;
    private Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps;

    public ResolvedFeatureSpec(ResolvedSpecId specId, FeatureSpec spec) {
        this.id = specId;
        this.xmlSpec = spec;
    }

    Map<ResolvedFeatureId, FeatureDependencySpec> resolveFeatureDeps(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        if(resolvedDeps != null) {
            return resolvedDeps;
        }
        if(!xmlSpec.hasFeatureDeps()) {
            resolvedDeps = Collections.emptyMap();
            return resolvedDeps;
        }
        final FeaturePackRuntime.Builder ownFp = rt.getFpBuilder(id.gav);
        final Collection<FeatureDependencySpec> depSpecs = xmlSpec.getFeatureDeps();
        if(depSpecs.size() == 1) {
            final FeatureDependencySpec depSpec = depSpecs.iterator().next();
            final ResolvedFeatureId depId = rt.resolveFeatureId(depSpec.getDependency() == null ? ownFp : rt.getFpDependency(ownFp, depSpec.getDependency()), depSpec.getFeatureId());
            resolvedDeps = Collections.singletonMap(depId, depSpec);
        } else {
            resolvedDeps = new LinkedHashMap<>(depSpecs.size());
            for(FeatureDependencySpec depSpec : depSpecs) {
                final ResolvedFeatureId depId = rt.resolveFeatureId(ownFp, depSpec.getFeatureId());
                resolvedDeps.put(depId, depSpec);
            }
        }
        return resolvedDeps;
    }

    void resolveRefMappings(ProvisioningRuntimeBuilder rt) throws ProvisioningDescriptionException {
        if(!xmlSpec.hasFeatureRefs()) {
            resolvedRefTargets = Collections.emptyMap();
            return;
        }
        final FeaturePackRuntime.Builder ownFp = rt.getFpBuilder(id.gav);

        Collection<FeatureReferenceSpec> refs = xmlSpec.getFeatureRefs();
        if (refs.size() == 1) {
            resolvedRefTargets = Collections.singletonMap(refs.iterator().next().getName(), resolveRefMapping(rt, ownFp, refs.iterator().next()));
            return;
        }

        final Map<String, ResolvedFeatureSpec> tmp = new HashMap<>(refs.size());
        for (FeatureReferenceSpec refSpec : refs) {
            tmp.put(refSpec.getName(), resolveRefMapping(rt, ownFp, refSpec));
        }
        this.resolvedRefTargets = Collections.unmodifiableMap(tmp);
    }

    private ResolvedFeatureSpec resolveRefMapping(ProvisioningRuntimeBuilder rt, final FeaturePackRuntime.Builder ownFp,
            FeatureReferenceSpec refSpec) throws ProvisioningDescriptionException {
        try {
            final ResolvedFeatureSpec resolvedRefSpec;
            if (refSpec.getDependency() == null) {
                resolvedRefSpec = ownFp.getFeatureSpec(refSpec.getFeature().getName());
            } else {
                final FeaturePackRuntime.Builder refFp = rt
                        .getFpBuilder(ownFp.spec.getDependency(refSpec.getDependency()).getTarget().getGav());
                resolvedRefSpec = refFp.getFeatureSpec(refSpec.getFeature().getName());
            }
            assertRefParamMapping(refSpec, resolvedRefSpec);
            return resolvedRefSpec;
        } catch (ProvisioningDescriptionException e) {
            throw new ProvisioningDescriptionException(Errors.failedToResolveFeatureReference(refSpec, id), e);
        }
    }

    private void assertRefParamMapping(final FeatureReferenceSpec refSpec, final ResolvedFeatureSpec targetSpec)
            throws ProvisioningDescriptionException {
        if (!targetSpec.xmlSpec.hasId()) {
            throw new ProvisioningDescriptionException(id + " feature spec declares reference "
                    + refSpec.getName() + " to feature spec " + targetSpec.id
                    + " that has no ID parameters");
        }
        if(refSpec.getParamsMapped() == 0) {
            for(FeatureParameterSpec targetIdParam : targetSpec.xmlSpec.getIdParams()) {
                if(!xmlSpec.hasParam(targetIdParam.getName())) {
                    throw new ProvisioningDescriptionException(Errors.nonExistingTargetIdParamInFkDefaultMapping(refSpec.getName(), id, targetIdParam.getName()));
                }
            }
            return;
        }
        if (targetSpec.xmlSpec.getIdParams().size() != refSpec.getParamsMapped()) {
            throw new ProvisioningDescriptionException("The number of foreign key parameters of reference " + refSpec.getName() +
                    " in feature spec " + id + " does not match the number of the ID parameters of the referenced feature spec "
                    + targetSpec.id);
        }
        for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
            if (!xmlSpec.hasParam(refSpec.getLocalParam(i))) {
                throw new ProvisioningDescriptionException(Errors.invalidLocalParamInFkMapping(refSpec.getLocalParam(i), refSpec.getName(), id));
            }
            if (!targetSpec.xmlSpec.hasParam(refSpec.getTargetParam(i))) {
                throw new ProvisioningDescriptionException(
                        Errors.invalidTargetIdParamInFkMapping(refSpec.getLocalParam(i), refSpec.getName(), id, refSpec.getTargetParam(i), targetSpec.id));
            }
        }
    }

    List<ResolvedFeatureId> resolveRefs(ResolvedFeature feature) throws ProvisioningDescriptionException {
        if(resolvedRefTargets.isEmpty()) {
            return Collections.emptyList();
        }
        if(resolvedRefTargets.size() == 1) {
            final Entry<String, ResolvedFeatureSpec> refEntry = resolvedRefTargets.entrySet().iterator().next();
            final ResolvedFeatureId refId = resolveRefId(feature, xmlSpec.getFeatureRef(refEntry.getKey()), refEntry.getValue(), false);
            return refId == null ? Collections.emptyList() : Collections.singletonList(refId);
        }
        final List<ResolvedFeatureId> refIds = new ArrayList<>(resolvedRefTargets.size());
        for(Map.Entry<String, ResolvedFeatureSpec> refEntry : resolvedRefTargets.entrySet()) {
            final ResolvedFeatureId refId = resolveRefId(feature, xmlSpec.getFeatureRef(refEntry.getKey()), refEntry.getValue(), false);
            if(refId != null) {
                refIds.add(refId);
            }
        }
        return refIds;
    }

    ResolvedFeatureId resolveRefId(final ResolvedFeature feature, final FeatureReferenceSpec refSpec, final ResolvedFeatureSpec targetSpec)
            throws ProvisioningDescriptionException {
        return resolveRefId(feature, refSpec, targetSpec, true);
    }

    private ResolvedFeatureId resolveRefId(final ResolvedFeature feature, final FeatureReferenceSpec refSpec, final ResolvedFeatureSpec targetSpec, boolean assertRefMapping)
            throws ProvisioningDescriptionException {
        if(assertRefMapping) {
            assertRefParamMapping(refSpec, targetSpec);
        }
        if(refSpec.getParamsMapped() == 0) {
            final List<FeatureParameterSpec> targetIdParams = targetSpec.xmlSpec.getIdParams();
            if(targetIdParams.size() == 1) {
                final String paramName = targetIdParams.get(0).getName();
                final String paramValue = feature.getParam(paramName);
                if(paramValue == null || paramValue.equals(Constants.PM_UNDEFINED)) {
                    assertRefNotNillable(feature, refSpec);
                    return null;
                }
                return new ResolvedFeatureId(targetSpec.id, Collections.singletonMap(paramName, paramValue));
            }
            final Map<String, String> params = new HashMap<>(targetIdParams.size());
            for(FeatureParameterSpec targetIdParam : targetIdParams) {
                final String paramValue = feature.getParam(targetIdParam.getName());
                if(paramValue == null) {
                    assertRefNotNillable(feature, refSpec);
                    return null;
                } else if(!paramValue.equals(Constants.PM_UNDEFINED)) {
                    params.put(targetIdParam.getName(), paramValue);
                }
            }
            if(params.isEmpty()) {
                assertRefNotNillable(feature, refSpec);
                return null;
            }
            return new ResolvedFeatureId(targetSpec.id, params);
        }
        if(refSpec.getParamsMapped() == 1) {
            final String paramValue = feature.getParam(refSpec.getLocalParam(0));
            if(paramValue == null || paramValue.equals(Constants.PM_UNDEFINED)) {
                assertRefNotNillable(feature, refSpec);
                return null;
            }
            return new ResolvedFeatureId(targetSpec.id, Collections.singletonMap(refSpec.getTargetParam(0), paramValue));
        }
        Map<String, String> params = new HashMap<>(refSpec.getParamsMapped());
        for(int i = 0; i < refSpec.getParamsMapped(); ++i) {
            final String paramValue = feature.getParam(refSpec.getLocalParam(i));
            if(paramValue == null) {
                assertRefNotNillable(feature, refSpec);
                return null;
            } else if(!paramValue.equals(Constants.PM_UNDEFINED)) {
                params.put(refSpec.getTargetParam(i), paramValue);
            }
        }
        if(params.isEmpty()) {
            assertRefNotNillable(feature, refSpec);
            return null;
        }
        return new ResolvedFeatureId(targetSpec.id, params);
    }

    private void assertRefNotNillable(final ResolvedFeature feature, final FeatureReferenceSpec refSpec)
            throws ProvisioningDescriptionException {
        if (!refSpec.isNillable()) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Reference ").append(refSpec).append(" of ");
            if (feature.id != null) {
                buf.append(feature.id);
            } else {
                buf.append(id).append(" configuration ");
            }
            buf.append(" cannot be null");
            throw new ProvisioningDescriptionException(buf.toString());
        }
    }

    public ResolvedSpecId getId() {
        return id;
    }

    public String getName() {
        return id.name;
    }

    public boolean hasAnnotations() {
        return xmlSpec.hasAnnotations();
    }

    public List<FeatureAnnotation> getAnnotations() {
        return xmlSpec.getAnnotations();
    }

    public boolean hasParams() {
        return xmlSpec.hasParams();
    }

    public Set<String> getParamNames() {
        return xmlSpec.getParamNames();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((resolvedRefTargets == null) ? 0 : resolvedRefTargets.hashCode());
        result = prime * result + ((xmlSpec == null) ? 0 : xmlSpec.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResolvedFeatureSpec other = (ResolvedFeatureSpec) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (resolvedRefTargets == null) {
            if (other.resolvedRefTargets != null)
                return false;
        } else if (!resolvedRefTargets.equals(other.resolvedRefTargets))
            return false;
        if (xmlSpec == null) {
            if (other.xmlSpec != null)
                return false;
        } else if (!xmlSpec.equals(other.xmlSpec))
            return false;
        return true;
    }
}
