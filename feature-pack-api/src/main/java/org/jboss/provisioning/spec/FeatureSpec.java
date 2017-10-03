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

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.StringUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpec implements PackageDependencies {

    public static class Builder {

        private String name;
        private List<FeatureAnnotation> annotations = Collections.emptyList();
        private Map<String, FeatureReferenceSpec> refs = Collections.emptyMap();
        private Map<String, FeatureParameterSpec> params = Collections.emptyMap();
        private List<FeatureParameterSpec> idParams = Collections.emptyList();
        private PackageDependencyGroupSpec.Builder localDeps;
        private Map<String, PackageDependencyGroupSpec.Builder> externalDeps = Collections.emptyMap();
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

        public Builder addRef(FeatureReferenceSpec ref) throws ProvisioningDescriptionException {
            if(refs.isEmpty()) {
                refs = Collections.singletonMap(ref.name, ref);
                return this;
            }
            if(refs.containsKey(ref.name)) {
                throw new ProvisioningDescriptionException("Duplicate reference " + ref.name + " for feature " + name);
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

        public Builder addPackageDependency(String packageName) {
            getLocalPackageGroup().addDependency(packageName);
            return this;
        }

        public Builder addPackageDependency(String packageName, boolean optional) {
            getLocalPackageGroup().addDependency(packageName, optional);
            return this;
        }

        public Builder addPackageDependency(PackageDependencySpec dep) {
            getLocalPackageGroup().addDependency(dep);
            return this;
        }

        public Builder addPackageDependency(String groupName, String packageName) {
            getExternalPackageGroup(groupName).addDependency(packageName);
            return this;
        }

        public Builder addPackageDependency(String groupName, String packageName, boolean optional) {
            getExternalPackageGroup(groupName).addDependency(packageName, optional);
            return this;
        }

        public Builder addPackageDependency(String groupName, PackageDependencySpec dep) {
            getExternalPackageGroup(groupName).addDependency(dep);
            return this;
        }

        public boolean hasDependencies() {
            return localDeps != null || !externalDeps.isEmpty();
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

        private PackageDependencyGroupSpec.Builder getLocalPackageGroup() {
            if(localDeps == null) {
                localDeps = PackageDependencyGroupSpec.builder();
            }
            return localDeps;
        }

        private PackageDependencyGroupSpec.Builder getExternalPackageGroup(String groupName) {
            PackageDependencyGroupSpec.Builder groupBuilder = externalDeps.get(groupName);
            if(groupBuilder == null) {
                groupBuilder = PackageDependencyGroupSpec.builder(groupName);
                switch(externalDeps.size()) {
                    case 0:
                        externalDeps = Collections.singletonMap(groupName, groupBuilder);
                        break;
                    case 1:
                        externalDeps = new LinkedHashMap<>(externalDeps);
                    default:
                        externalDeps.put(groupName, groupBuilder);
                }
            }
            return groupBuilder;
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
    final Map<String, FeatureReferenceSpec> refs;
    final Map<String, FeatureParameterSpec> params;
    final List<FeatureParameterSpec> idParams;
    private final PackageDependencyGroupSpec localPkgDeps;
    private final Map<String, PackageDependencyGroupSpec> externalPkgDeps;
    final Set<CapabilitySpec> providedCaps;
    final Set<CapabilitySpec> requiredCaps;

    private FeatureSpec(Builder builder) {
        this.name = builder.name;
        this.annotations = builder.annotations.size() > 1 ? Collections.unmodifiableList(builder.annotations) : builder.annotations;
        this.refs = builder.refs.size() > 1 ? Collections.unmodifiableMap(builder.refs) : builder.refs;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.idParams = builder.idParams.size() > 1 ? Collections.unmodifiableList(builder.idParams) : builder.idParams;
        this.providedCaps = builder.providedCaps.size() > 1 ? Collections.unmodifiableSet(builder.providedCaps) : builder.providedCaps;
        this.requiredCaps = builder.requiredCaps.size() > 1 ? Collections.unmodifiableSet(builder.requiredCaps) : builder.requiredCaps;

        this.localPkgDeps = builder.localDeps == null ? PackageDependencyGroupSpec.EMPTY_LOCAL : builder.localDeps.build();
        if(builder.externalDeps.isEmpty()) {
            externalPkgDeps = Collections.emptyMap();
        } else {
            final int size = builder.externalDeps.size();
            if(size == 1) {
                final Map.Entry<String, PackageDependencyGroupSpec.Builder> entry = builder.externalDeps.entrySet().iterator().next();
                externalPkgDeps = Collections.singletonMap(entry.getKey(), entry.getValue().build());
            } else {
                final Map<String, PackageDependencyGroupSpec> deps = new LinkedHashMap<>(size);
                for(Map.Entry<String, PackageDependencyGroupSpec.Builder> entry : builder.externalDeps.entrySet()) {
                    deps.put(entry.getKey(), entry.getValue().build());
                }
                externalPkgDeps = Collections.unmodifiableMap(deps);
            }
        }
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

    public boolean hasRefs() {
        return !refs.isEmpty();
    }

    public Collection<FeatureReferenceSpec> getRefs() {
        return refs.values();
    }

    public FeatureReferenceSpec getRef(String name) throws ProvisioningDescriptionException {
        final FeatureReferenceSpec ref = refs.get(name);
        if(ref == null) {
            throw new ProvisioningDescriptionException("Feature reference '" + name + "' not found in feature spec " + this.name);
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
    public boolean dependsOnPackages() {
        return !(localPkgDeps.isEmpty() && externalPkgDeps.isEmpty());
    }

    @Override
    public boolean dependsOnLocalPackages() {
        return !localPkgDeps.isEmpty();
    }

    @Override
    public PackageDependencyGroupSpec getLocalPackageDependencies() {
        return localPkgDeps;
    }

    @Override
    public boolean dependsOnExternalPackages() {
        return !externalPkgDeps.isEmpty();
    }

    @Override
    public Collection<String> getPackageDependencySources() {
        return externalPkgDeps.keySet();
    }

    @Override
    public PackageDependencyGroupSpec getExternalPackageDependencies(String groupName) {
        return externalPkgDeps.get(groupName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
        result = prime * result + ((externalPkgDeps == null) ? 0 : externalPkgDeps.hashCode());
        result = prime * result + ((idParams == null) ? 0 : idParams.hashCode());
        result = prime * result + ((localPkgDeps == null) ? 0 : localPkgDeps.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((providedCaps == null) ? 0 : providedCaps.hashCode());
        result = prime * result + ((refs == null) ? 0 : refs.hashCode());
        result = prime * result + ((requiredCaps == null) ? 0 : requiredCaps.hashCode());
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
        FeatureSpec other = (FeatureSpec) obj;
        if (annotations == null) {
            if (other.annotations != null)
                return false;
        } else if (!annotations.equals(other.annotations))
            return false;
        if (externalPkgDeps == null) {
            if (other.externalPkgDeps != null)
                return false;
        } else if (!externalPkgDeps.equals(other.externalPkgDeps))
            return false;
        if (idParams == null) {
            if (other.idParams != null)
                return false;
        } else if (!idParams.equals(other.idParams))
            return false;
        if (localPkgDeps == null) {
            if (other.localPkgDeps != null)
                return false;
        } else if (!localPkgDeps.equals(other.localPkgDeps))
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
        if (refs == null) {
            if (other.refs != null)
                return false;
        } else if (!refs.equals(other.refs))
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
        if(!refs.isEmpty()) {
            buf.append(" refs=");
            StringUtils.append(buf, refs.values());
        }
        if(!params.isEmpty()) {
            buf.append(" params=");
            StringUtils.append(buf, params.values());
        }
        return buf.append(']').toString();
    }
}
