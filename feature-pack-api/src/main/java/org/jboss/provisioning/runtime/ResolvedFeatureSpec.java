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
import java.util.Iterator;
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
import org.jboss.provisioning.type.FeatureParameterType;
import org.jboss.provisioning.type.ParameterTypeNotFoundException;
import org.jboss.provisioning.type.ParameterTypeProvider;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.util.StringUtils;


/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeatureSpec extends CapabilityProvider {

    final ResolvedSpecId id;
    final FeatureSpec xmlSpec;
    private final ParameterTypeProvider typeProvider;
    private Map<String, ResolvedFeatureSpec> resolvedRefTargets;
    private Map<ResolvedFeatureId, FeatureDependencySpec> resolvedDeps;


    public ResolvedFeatureSpec(ResolvedSpecId specId, ParameterTypeProvider typeProvider, FeatureSpec spec) {
        this.id = specId;
        this.typeProvider = typeProvider;
        this.xmlSpec = spec;
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

    Map<String, Object> resolveNonIdParams(ResolvedFeatureId parentId, String parentRef, Map<String, String> params) throws ProvisioningException {
        Map<String, Object> resolvedParams = Collections.emptyMap();
        if (!params.isEmpty()) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                if(xmlSpec.getParam(param.getKey()).isFeatureId()) {
                    continue;
                }
                resolvedParams = PmCollections.put(resolvedParams, param.getKey(), resolveParameter(param.getKey(), param.getValue()));
            }
        }

        if(parentId == null) {
            return resolvedParams;
        }

        if(parentRef == null) {
            parentRef = parentId.specId.name;
        }

        final FeatureReferenceSpec refSpec = xmlSpec.getFeatureRef(parentRef);
        if (refSpec.hasMappedParams()) {
            for (Map.Entry<String, String> mapping : refSpec.getMappedParams().entrySet()) {
                if (xmlSpec.getParam(mapping.getKey()).isFeatureId()) {
                    continue;
                }
                final Object idValue = parentId.params.get(mapping.getValue());
                if (idValue != null) {
                    resolvedParams = PmCollections.put(resolvedParams, mapping.getKey(), idValue);
                }
            }
        } else {
            for (Map.Entry<String, Object> parentEntry : parentId.params.entrySet()) {
                if (xmlSpec.getParam(parentEntry.getKey()).isFeatureId()) {
                    continue;
                }
                resolvedParams = PmCollections.put(resolvedParams, parentEntry.getKey(), parentEntry.getValue());
            }
        }

        return resolvedParams;
    }

    private Object resolveParameter(String name, String value) throws ProvisioningException {
        final FeatureParameterSpec paramSpec = xmlSpec.getParam(name);
        return typeProvider.getType(id.gav.toGa(), paramSpec.getType()).fromString(value);
    }

    ResolvedFeatureId resolveIdFromForeignKey(ResolvedFeatureId parentId, String parentRef, Map<String, String> params) throws ProvisioningException {
        if(!xmlSpec.hasId()) {
            return null;
        }
        if(parentId == null) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to initialize foreign key parameters of ").append(id).append(": the referenced feature has not ID ");
            throw new ProvisioningException(buf.toString());
        }
        if(parentRef == null) {
            parentRef = parentId.specId.name;
        }

        try {
            final List<FeatureParameterSpec> idParamSpecs = xmlSpec.getIdParams();
            final Map<String, Object> resolvedParams = new HashMap<>(idParamSpecs.size());
            final FeatureReferenceSpec refSpec = xmlSpec.getFeatureRef(parentRef);

            if (refSpec.hasMappedParams()) {
                for (Map.Entry<String, String> mapping : refSpec.getMappedParams().entrySet()) {
                    final FeatureParameterSpec param = xmlSpec.getParam(mapping.getKey());
                    if(!param.isFeatureId()) {
                        continue;
                    }
                    final Object idValue = parentId.params.get(mapping.getValue());
                    if (idValue == null) {
                        throw new ProvisioningDescriptionException(id + " expects ID parameter '" + mapping.getValue() + "' in " + parentId);
                    }
                    resolvedParams.put(mapping.getKey(), idValue);
                }
                for(FeatureParameterSpec idParamSpec : idParamSpecs) {
                    String configValue = params.get(idParamSpec.getName());
                    if(configValue != null) {
                        final Object childValue = getTypeForName(idParamSpec.getType()).fromString(configValue);
                        final Object idValue = resolvedParams.put(idParamSpec.getName(), childValue);
                        if(idValue != null && !idValue.equals(childValue)) {
                            throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(id, idParamSpec.getName(), childValue, idValue));
                        }
                        continue;
                    }

                    if(resolvedParams.containsKey(idParamSpec.getName())) {
                        continue;
                    }

                    final Object childValue = idParamSpec.hasDefaultValue() ? getTypeForName(idParamSpec.getType()).fromString(idParamSpec.getDefaultValue()) : getTypeForName(idParamSpec.getType()).getDefaultValue();
                    if(childValue == null) {
                        throw new ProvisioningDescriptionException(Errors.nonNillableParameterIsNull(id, idParamSpec.getName()));
                    }
                    resolvedParams.put(idParamSpec.getName(), childValue);
                }
            } else {
                for (FeatureParameterSpec idParamSpec : idParamSpecs) {
                    final Object parentValue = parentId.params.get(idParamSpec.getName());
                    String configValue = params.get(idParamSpec.getName());
                    if(configValue != null) {
                        final Object childValue = getTypeForName(idParamSpec.getType()).fromString(configValue);
                        if(parentValue != null && !parentValue.equals(childValue)) {
                            throw new ProvisioningDescriptionException(Errors.idParamForeignKeyInitConflict(id, idParamSpec.getName(), childValue, parentValue));
                        }
                        resolvedParams.put(idParamSpec.getName(), childValue);
                        continue;
                    }

                    if(parentValue != null) {
                        resolvedParams.put(idParamSpec.getName(), parentValue);
                        continue;
                    }

                    final Object childValue = idParamSpec.hasDefaultValue() ? getTypeForName(idParamSpec.getType()).fromString(idParamSpec.getDefaultValue()) : getTypeForName(idParamSpec.getType()).getDefaultValue();
                    if(childValue == null) {
                        throw new ProvisioningDescriptionException(Errors.nonNillableParameterIsNull(id, idParamSpec.getName()));
                    }
                    resolvedParams.put(idParamSpec.getName(), childValue);
                }
            }

            return new ResolvedFeatureId(id, resolvedParams);
        } catch(ProvisioningException e) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to initialize foreign key parameters of ").append(id).append(" spec referencing feature ").append(parentId).append(" with parameters ");
            StringUtils.append(buf, params.entrySet());
            throw new ProvisioningException(Errors.failedToInitializeForeignKeyParams(id, parentId, params), e);
        }
    }

    ResolvedFeatureId resolveFeatureId(Map<String, String> params) throws ProvisioningException {
        if(!xmlSpec.hasId()) {
            return null;
        }
        final List<FeatureParameterSpec> idSpecs = xmlSpec.getIdParams();
        if(idSpecs.size() == 1) {
            final FeatureParameterSpec idSpec = idSpecs.get(0);
            return new ResolvedFeatureId(id, Collections.singletonMap(idSpec.getName(), resolveIdParamValue(params, idSpec)));
        }
        final Map<String, Object> resolvedParams = new HashMap<>(idSpecs.size());
        for(FeatureParameterSpec param : idSpecs) {
            resolvedParams.put(param.getName(), resolveIdParamValue(params, param));
        }
        return new ResolvedFeatureId(id, resolvedParams);
    }

    private Object resolveIdParamValue(Map<String, String> params, final FeatureParameterSpec param) throws ProvisioningException {
        String strValue = params.get(param.getName());
        if(strValue == null) {
            strValue = param.getDefaultValue();
            if(strValue == null) {
                final Object value = getTypeForName(param.getType()).getDefaultValue();
                if(value == null) {
                    throw new ProvisioningDescriptionException(Errors.nonNillableParameterIsNull(id, param.getName()));
                }
            }
        }
        if(strValue == null) {
            throw new ProvisioningDescriptionException(Errors.nonNillableParameterIsNull(id, param.getName()));
        }
        return getTypeForName(param.getType()).fromString(strValue);
    }

    FeatureParameterType getTypeForParameter(String paramName) throws ParameterTypeNotFoundException, ProvisioningDescriptionException {
        return getTypeForName(xmlSpec.getParam(paramName).getType());
    }

    FeatureParameterType getTypeForName(String typeName) throws ParameterTypeNotFoundException {
        return typeProvider.getType(id.gav.toGa(), typeName);
    }

    Map<ResolvedFeatureId, FeatureDependencySpec> resolveSpecDeps(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        if(resolvedDeps != null) {
            return resolvedDeps;
        }
        resolvedDeps = Collections.emptyMap();
        if(xmlSpec.hasFeatureDeps()) {
            resolvedDeps = resolveFeatureDeps(rt, xmlSpec.getFeatureDeps());
        }
        return resolvedDeps;
    }

    Map<ResolvedFeatureId, FeatureDependencySpec> resolveFeatureDeps(ProvisioningRuntimeBuilder rt, final Collection<FeatureDependencySpec> depSpecs) throws ProvisioningException {
        if(depSpecs.isEmpty()) {
            return resolveSpecDeps(rt);
        }
        final Map<ResolvedFeatureId, FeatureDependencySpec> resolvedSpecDeps = resolveSpecDeps(rt);
        final FeaturePackRuntime.Builder ownFp = rt.getFpBuilder(id.gav);
        final Map<ResolvedFeatureId, FeatureDependencySpec> result;
        if(resolvedSpecDeps.isEmpty()) {
            if(depSpecs.size() == 1) {
                final FeatureDependencySpec depSpec = depSpecs.iterator().next();
                final FeaturePackRuntime.Builder depFp = depSpec.getDependency() == null ? ownFp : rt.getFpDependency(ownFp, depSpec.getDependency());
                final ResolvedFeatureSpec depResolvedSpec = depFp.getFeatureSpec(depSpec.getFeatureId().getSpec().getName());
                return Collections.singletonMap(depResolvedSpec.resolveFeatureId(depSpec.getFeatureId().getParams()), depSpec);
            }
            result = new LinkedHashMap<>(depSpecs.size());
        } else {
            result = new LinkedHashMap<>(resolvedSpecDeps.size() + depSpecs.size());
            result.putAll(resolvedSpecDeps);
        }
        for (FeatureDependencySpec userDep : depSpecs) {
            final FeaturePackRuntime.Builder depFp = userDep.getDependency() == null ? ownFp : rt.getFpDependency(ownFp, userDep.getDependency());
            final ResolvedFeatureSpec depResolvedSpec = depFp.getFeatureSpec(userDep.getFeatureId().getSpec().getName());
            final ResolvedFeatureId depId = depResolvedSpec.resolveFeatureId(userDep.getFeatureId().getParams());
            final FeatureDependencySpec specDep = result.put(depId, userDep);
            if(specDep != null) {
                if(!userDep.isInclude() && specDep.isInclude()) {
                    result.put(depId, specDep);
                }
            }
        }
        return result;
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
        if(!refSpec.hasMappedParams()) {
            for(FeatureParameterSpec targetIdParam : targetSpec.xmlSpec.getIdParams()) {
                if(!xmlSpec.hasParam(targetIdParam.getName())) {
                    throw new ProvisioningDescriptionException(Errors.nonExistingForeignKeyParam(refSpec.getName(), id, targetIdParam.getName()));
                }
            }
            return;
        }
        if (targetSpec.xmlSpec.getIdParams().size() != refSpec.getParamsMapped()) {
            throw new ProvisioningDescriptionException("The number of foreign key parameters of reference " + refSpec.getName() +
                    " in feature spec " + id + " does not match the number of the ID parameters of the referenced feature spec "
                    + targetSpec.id);
        }
        for(Map.Entry<String, String> mapping : refSpec.getMappedParams().entrySet()) {
            if (!xmlSpec.hasParam(mapping.getKey())) {
                throw new ProvisioningDescriptionException(Errors.nonExistingForeignKeyParam(refSpec.getName(), id, mapping.getKey()));
            }
            if (!targetSpec.xmlSpec.hasParam(mapping.getValue())) {
                throw new ProvisioningDescriptionException(
                        Errors.nonExistingForeignKeyTarget(mapping.getKey(), refSpec.getName(), id, mapping.getValue(), targetSpec.id));
            }
        }
    }

    List<ResolvedFeatureId> resolveRefs(ResolvedFeature feature) throws ProvisioningException {
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
            throws ProvisioningException {
        return resolveRefId(feature, refSpec, targetSpec, true);
    }

    private ResolvedFeatureId resolveRefId(final ResolvedFeature feature, final FeatureReferenceSpec refSpec, final ResolvedFeatureSpec targetSpec, boolean assertRefMapping)
            throws ProvisioningException {
        if(assertRefMapping) {
            assertRefParamMapping(refSpec, targetSpec);
        }
        if(!refSpec.hasMappedParams()) {
            final List<FeatureParameterSpec> targetIdParams = targetSpec.xmlSpec.getIdParams();
            if(targetIdParams.size() == 1) {
                final String paramName = targetIdParams.get(0).getName();
                final Object paramValue = feature.getParamOrDefault(paramName);
                if(paramValue == null || paramValue.equals(Constants.PM_UNDEFINED)) {
                    assertRefNotNillable(feature, refSpec);
                    return null;
                }
                return new ResolvedFeatureId(targetSpec.id, Collections.singletonMap(paramName, paramValue));
            }
            final Map<String, Object> params = new HashMap<>(targetIdParams.size());
            for(FeatureParameterSpec targetIdParam : targetIdParams) {
                final Object paramValue = feature.getParamOrDefault(targetIdParam.getName());
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

        final Iterator<Map.Entry<String, String>> i = refSpec.getMappedParams().entrySet().iterator();
        Map.Entry<String, String> mapping = i.next();

        Object paramValue = feature.getParamOrDefault(mapping.getKey());
        if(paramValue == null) {
            assertRefNotNillable(feature, refSpec);
            return null;
        }

        if(!i.hasNext()) {
            if(paramValue.equals(Constants.PM_UNDEFINED)) {
                assertRefNotNillable(feature, refSpec);
                return null;
            }
            return new ResolvedFeatureId(targetSpec.id, Collections.singletonMap(mapping.getValue(), paramValue));
        }

        Map<String, Object> params = new HashMap<>(refSpec.getParamsMapped());
        if(!paramValue.equals(Constants.PM_UNDEFINED)) {
            params.put(mapping.getValue(), paramValue);
        }
        while(i.hasNext()) {
            mapping = i.next();
            paramValue = feature.getParamOrDefault(mapping.getKey());
            if(paramValue == null) {
                assertRefNotNillable(feature, refSpec);
                return null;
            } else if(!paramValue.equals(Constants.PM_UNDEFINED)) {
                params.put(mapping.getValue(), paramValue);
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
