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

package org.jboss.provisioning.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.StringUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpec extends PackageDepsSpec {

    public static class Builder extends PackageDepsSpecBuilder<Builder> {

        private String name;
        private List<FeatureAnnotation> annotations = Collections.emptyList();
        private Map<FeatureId, FeatureDependencySpec> featureDeps = Collections.emptyMap();
        private Map<String, FeatureReferenceSpec> refs = Collections.emptyMap();
        private Map<String, FeatureParameterSpec> params = Collections.emptyMap();
        private List<FeatureParameterSpec> idParams = Collections.emptyList();
        private Set<CapabilitySpec> providedCaps = Collections.emptySet();
        private Set<CapabilitySpec> requiredCaps = Collections.emptySet();

        private Builder() {
        }

        private Builder(String name) {
            this.name = name;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addAnnotation(FeatureAnnotation annotation) {
            switch(annotations.size()) {
                case 0:
                    annotations = Collections.singletonList(annotation);
                    break;
                case 1:
                    final FeatureAnnotation first = annotations.get(0);
                    annotations = new ArrayList<>(2);
                    annotations.add(first);
                default:
                    annotations.add(annotation);
            }
            return this;
        }

        public Builder addFeatureDep(FeatureDependencySpec dep) throws ProvisioningDescriptionException {
            switch(featureDeps.size()) {
                case 0:
                    featureDeps = Collections.singletonMap(dep.getFeatureId(), dep);
                    break;
                case 1:
                    if(featureDeps.containsKey(dep.getFeatureId())) {
                        throw new ProvisioningDescriptionException("Duplicate dependency on " + dep.getFeatureId() + " from feature spec " + name);
                    }
                    featureDeps = new LinkedHashMap<>(featureDeps);
                default:
                    featureDeps.put(dep.getFeatureId(), dep);
            }
            return this;
        }

        public Builder addFeatureRef(FeatureReferenceSpec ref) throws ProvisioningDescriptionException {
            if(refs.isEmpty()) {
                refs = Collections.singletonMap(ref.name, ref);
                return this;
            }
            if(refs.containsKey(ref.name)) {
                throw new ProvisioningDescriptionException("Duplicate reference " + ref.name + " in feature spec " + name);
            }
            if(refs.size() == 1) {
                final Map.Entry<String, FeatureReferenceSpec> entry = refs.entrySet().iterator().next();
                refs = new LinkedHashMap<>(2);
                refs.put(entry.getKey(), entry.getValue());
            }
            refs.put(ref.name, ref);
            return this;
        }

        public Builder addParam(FeatureParameterSpec param) throws ProvisioningDescriptionException {
            if(params.isEmpty()) {
                params = Collections.singletonMap(param.name, param);
            } else if(params.containsKey(param.name)) {
                throw new ProvisioningDescriptionException("Duplicate parameter " + param + " for feature " + name);
            } else {
                if (params.size() == 1) {
                    final Map.Entry<String, FeatureParameterSpec> entry = params.entrySet().iterator().next();
                    params = new HashMap<>();
                    params.put(entry.getKey(), entry.getValue());
                }
                params.put(param.name, param);
            }
            if(param.featureId) {
                switch(idParams.size()) {
                    case 0:
                        idParams = Collections.singletonList(param);
                        break;
                    case 1:
                        idParams = new ArrayList<>(idParams);
                    default:
                        idParams.add(param);
                }
            }
            return this;
        }

        public Builder providesCapability(String name) throws ProvisioningDescriptionException {
            return providesCapability(name, false);
        }

        public Builder providesCapability(String name, boolean optional) throws ProvisioningDescriptionException {
            return providesCapability(CapabilitySpec.fromString(name, optional));
        }

        public Builder providesCapability(CapabilitySpec cap) {
            switch(providedCaps.size()) {
                case 0:
                    providedCaps = Collections.singleton(cap);
                    break;
                case 1:
                    final CapabilitySpec first = providedCaps.iterator().next();
                    providedCaps = new HashSet<>(2);
                    providedCaps.add(first);
                default:
                    providedCaps.add(cap);
            }
            return this;
        }

        public Builder requiresCapability(String name) throws ProvisioningDescriptionException {
            return requiresCapability(name, false);
        }

        public Builder requiresCapability(String name, boolean optional) throws ProvisioningDescriptionException {
            return requiresCapability(CapabilitySpec.fromString(name, optional));
        }

        public Builder requiresCapability(CapabilitySpec cap) {
            switch(requiredCaps.size()) {
                case 0:
                    requiredCaps = Collections.singleton(cap);
                    break;
                case 1:
                    final CapabilitySpec first = requiredCaps.iterator().next();
                    requiredCaps = new HashSet<>(2);
                    requiredCaps.add(first);
                default:
                    requiredCaps.add(cap);
            }
            return this;
        }

        public FeatureSpec build() throws ProvisioningDescriptionException {
            return new FeatureSpec(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    final String name;
    final List<FeatureAnnotation> annotations;
    final Map<FeatureId, FeatureDependencySpec> featureDeps;
    final Map<String, FeatureReferenceSpec> featureRefs;
    final Map<String, FeatureParameterSpec> params;
    final List<FeatureParameterSpec> idParams;
    final Set<CapabilitySpec> providedCaps;
    final Set<CapabilitySpec> requiredCaps;

    private FeatureSpec(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.annotations = builder.annotations.size() > 1 ? Collections.unmodifiableList(builder.annotations) : builder.annotations;
        this.featureDeps = builder.featureDeps.size() > 1 ? Collections.unmodifiableMap(builder.featureDeps) : builder.featureDeps;
        this.featureRefs = builder.refs.size() > 1 ? Collections.unmodifiableMap(builder.refs) : builder.refs;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.idParams = builder.idParams.size() > 1 ? Collections.unmodifiableList(builder.idParams) : builder.idParams;
        this.providedCaps = builder.providedCaps.size() > 1 ? Collections.unmodifiableSet(builder.providedCaps) : builder.providedCaps;
        this.requiredCaps = builder.requiredCaps.size() > 1 ? Collections.unmodifiableSet(builder.requiredCaps) : builder.requiredCaps;
    }

    public String getName() {
        return name;
    }

    public boolean hasAnnotations() {
        return !annotations.isEmpty();
    }

    public List<FeatureAnnotation> getAnnotations() {
        return annotations;
    }

    public boolean hasId() {
        return !idParams.isEmpty();
    }

    public List<FeatureParameterSpec> getIdParams() {
        return idParams;
    }

    public boolean hasFeatureDeps() {
        return !featureDeps.isEmpty();
    }

    public Collection<FeatureDependencySpec> getFeatureDeps() {
        return featureDeps.values();
    }

    public boolean hasFeatureRefs() {
        return !featureRefs.isEmpty();
    }

    public Collection<FeatureReferenceSpec> getFeatureRefs() {
        return featureRefs.values();
    }

    public FeatureReferenceSpec getFeatureRef(String name) throws ProvisioningDescriptionException {
        final FeatureReferenceSpec ref = featureRefs.get(name);
        if(ref == null) {
            throw new ProvisioningDescriptionException(Errors.featureRefNotInSpec(name, this.name));
        }
        return ref;
    }

    public boolean hasParams() {
        return !params.isEmpty();
    }

    public int getParamsTotal() {
        return params.size();
    }

    public Set<String> getParamNames() {
        return params.keySet();
    }

    public Collection<FeatureParameterSpec> getParams() {
        return params.values();
    }

    public boolean hasParam(String name) {
        return params.containsKey(name);
    }

    public FeatureParameterSpec getParam(String name) throws ProvisioningDescriptionException {
        final FeatureParameterSpec paramSpec = params.get(name);
        if(paramSpec == null) {
            throw new ProvisioningDescriptionException("Feature spec " + this.getName() + " does not contain parameter " + name);
        }
        return paramSpec;
    }

    public boolean providesCapabilities() {
        return !providedCaps.isEmpty();
    }

    public Set<CapabilitySpec> getProvidedCapabilities() {
        return providedCaps;
    }

    public boolean requiresCapabilities() {
        return !requiredCaps.isEmpty();
    }

    public Set<CapabilitySpec> getRequiredCapabilities() {
        return requiredCaps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
        result = prime * result + ((featureDeps == null) ? 0 : featureDeps.hashCode());
        result = prime * result + ((featureRefs == null) ? 0 : featureRefs.hashCode());
        result = prime * result + ((idParams == null) ? 0 : idParams.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((providedCaps == null) ? 0 : providedCaps.hashCode());
        result = prime * result + ((requiredCaps == null) ? 0 : requiredCaps.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureSpec other = (FeatureSpec) obj;
        if (annotations == null) {
            if (other.annotations != null)
                return false;
        } else if (!annotations.equals(other.annotations))
            return false;
        if (featureDeps == null) {
            if (other.featureDeps != null)
                return false;
        } else if (!featureDeps.equals(other.featureDeps))
            return false;
        if (featureRefs == null) {
            if (other.featureRefs != null)
                return false;
        } else if (!featureRefs.equals(other.featureRefs))
            return false;
        if (idParams == null) {
            if (other.idParams != null)
                return false;
        } else if (!idParams.equals(other.idParams))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (providedCaps == null) {
            if (other.providedCaps != null)
                return false;
        } else if (!providedCaps.equals(other.providedCaps))
            return false;
        if (requiredCaps == null) {
            if (other.requiredCaps != null)
                return false;
        } else if (!requiredCaps.equals(other.requiredCaps))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(name);
        if(!providedCaps.isEmpty()) {
            buf.append(" provides=");
            StringUtils.append(buf, providedCaps);
        }
        if(!requiredCaps.isEmpty()) {
            buf.append(" requires=");
            StringUtils.append(buf, requiredCaps);
        }
        if(!featureDeps.isEmpty()) {
            buf.append(" deps=");
            StringUtils.append(buf, featureDeps.values());
        }
        if(!featureRefs.isEmpty()) {
            buf.append(" refs=");
            StringUtils.append(buf, featureRefs.values());
        }
        if(!params.isEmpty()) {
            buf.append(" params=");
            StringUtils.append(buf, params.values());
        }
        return buf.append(']').toString();
    }
}
