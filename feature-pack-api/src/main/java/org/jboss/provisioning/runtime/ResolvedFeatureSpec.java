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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;


/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeatureSpec {

    final ResolvedSpecId id;
    final FeatureSpec xmlSpec;
    final Map<String, ResolvedSpecId> resolvedRefTargets;

    public ResolvedFeatureSpec(ResolvedSpecId specId, FeatureSpec spec, Map<String, ResolvedSpecId> resolvedRefs) {
        this.id = specId;
        this.xmlSpec = spec;
        this.resolvedRefTargets = resolvedRefs;
    }

    void resolveRefMappings(ConfigModelBuilder configModelBuilder) throws ProvisioningDescriptionException {
        if (resolvedRefTargets.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ResolvedSpecId> entry : resolvedRefTargets.entrySet()) {
            final FeatureReferenceSpec refSpec = xmlSpec.getRef(entry.getKey());
            final ResolvedFeatureSpec targetSpec;
            try {
                targetSpec = configModelBuilder.getResolvedSpec(entry.getValue(), !refSpec.isNillable());
                if(targetSpec == null) {
                    continue;
                }
            } catch(ProvisioningDescriptionException e) {
                throw new ProvisioningDescriptionException("Failed to resolve reference " + refSpec.getName() + " of " + getId(), e);
            }
            if (!targetSpec.xmlSpec.hasId()) {
                throw new ProvisioningDescriptionException(getName() + " feature declares reference "
                        + refSpec.getName() + " which targets feature " + targetSpec.getName()
                        + " that has no ID parameters");
            }
            if(refSpec.getParamsMapped() == 0) {
                for(FeatureParameterSpec targetIdParam : targetSpec.xmlSpec.getIdParams()) {
                    if(!getParamNames().contains(targetIdParam.getName())) {
                        throw new ProvisioningDescriptionException(getName() + " feature does not include parameter "
                                + targetIdParam.getName() + " implied by reference " + refSpec.getName());
                    }
                }
                continue;
            }
            if (targetSpec.xmlSpec.getIdParams().size() != refSpec.getParamsMapped()) {
                throw new ProvisioningDescriptionException("Parameters mapped in reference " + refSpec.getName() + " of feature "
                        + getName() + " must correspond to the ID parameters of the target feature "
                        + targetSpec.getName());
            }
            for (int i = 0; i < refSpec.getParamsMapped(); ++i) {
                if (!xmlSpec.hasParam(refSpec.getLocalParam(i))) {
                    throw new ProvisioningDescriptionException(getName() + " feature does not include parameter "
                            + refSpec.getLocalParam(i) + " mapped in reference " + refSpec.getName());
                }
                if (!targetSpec.xmlSpec.hasParam(refSpec.getTargetParam(i))) {
                    throw new ProvisioningDescriptionException(targetSpec.getName()
                            + " feature does not include parameter '" + refSpec.getTargetParam(i) + "' referenced from "
                            + getName() + " through reference " + refSpec.getName());
                }
            }
        }
    }

    List<ResolvedFeatureId> resolveRefs(ResolvedFeature feature, ConfigModelBuilder configModelBuilder) throws ProvisioningDescriptionException {
        if(resolvedRefTargets.isEmpty()) {
            return Collections.emptyList();
        }
        if(resolvedRefTargets.size() == 1) {
            final Entry<String, ResolvedSpecId> refEntry = resolvedRefTargets.entrySet().iterator().next();
            final ResolvedFeatureId refId = getRefTarget(feature, refEntry.getValue(), xmlSpec.getRef(refEntry.getKey()), configModelBuilder);
            return refId == null ? Collections.emptyList() : Collections.singletonList(refId);
        }
        final List<ResolvedFeatureId> refIds = new ArrayList<>(resolvedRefTargets.size());
        for(Map.Entry<String, ResolvedSpecId> refEntry : resolvedRefTargets.entrySet()) {
            final ResolvedFeatureId refId = getRefTarget(feature, refEntry.getValue(), xmlSpec.getRef(refEntry.getKey()), configModelBuilder);
            if(refId != null) {
                refIds.add(refId);
            }
        }
        return refIds;
    }

    private ResolvedFeatureId getRefTarget(final ResolvedFeature feature, final ResolvedSpecId targetSpecId, final FeatureReferenceSpec refSpec, ConfigModelBuilder configModelBuilder)
            throws ProvisioningDescriptionException {
        if(refSpec.getParamsMapped() == 0) {
            final ResolvedFeatureSpec targetSpec = configModelBuilder.getResolvedSpec(targetSpecId, !refSpec.isNillable());
            if(targetSpec == null) {
                return null;
            }
            final List<FeatureParameterSpec> targetIdParams = targetSpec.xmlSpec.getIdParams();
            if(targetIdParams.size() == 1) {
                final String paramName = targetIdParams.get(0).getName();
                final String paramValue = feature.getParam(paramName);
                if(paramValue == null || paramValue.equals(Constants.PM_UNDEFINED)) {
                    assertRefNotNillable(feature, refSpec);
                    return null;
                }
                return new ResolvedFeatureId(targetSpecId, Collections.singletonMap(paramName, paramValue));
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
            return new ResolvedFeatureId(targetSpecId, params);
        }
        if(refSpec.getParamsMapped() == 1) {
            final String paramValue = feature.getParam(refSpec.getLocalParam(0));
            if(paramValue == null || paramValue.equals(Constants.PM_UNDEFINED)) {
                assertRefNotNillable(feature, refSpec);
                return null;
            }
            return new ResolvedFeatureId(targetSpecId, Collections.singletonMap(refSpec.getTargetParam(0), paramValue));
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
        return new ResolvedFeatureId(targetSpecId, params);
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
