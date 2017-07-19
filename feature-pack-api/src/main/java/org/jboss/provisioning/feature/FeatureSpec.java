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

package org.jboss.provisioning.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.PackageDependencies;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpec implements PackageDependencies {

    public static class Builder {

        private String name;
        private Map<String, FeatureReferenceSpec> refs = Collections.emptyMap();
        private Map<String, FeatureParameterSpec> params = Collections.emptyMap();
        private List<FeatureParameterSpec> idParams = Collections.emptyList();
        private PackageDependencyGroupSpec.Builder localDeps;
        private Map<String, PackageDependencyGroupSpec.Builder> externalDeps = Collections.emptyMap();

        private Builder() {
        }

        private Builder(String name) {
            this.name = name;
        }

        public Builder setId(String name) {
            this.name = name;
            return this;
        }

        public Builder addRef(FeatureReferenceSpec ref) {
            switch(refs.size()) {
                case 0:
                    refs = Collections.singletonMap(ref.name, ref);
                    break;
                case 1:
                    refs = new LinkedHashMap<>(refs);
                default:
                    refs.put(ref.name, ref);
            }
            return this;
        }

        public Builder addParam(FeatureParameterSpec param) throws ProvisioningDescriptionException {
            if(params.isEmpty()) {
                params = Collections.singletonMap(param.name, param);
            } else if(params.containsKey(param.name)) {
                throw new ProvisioningDescriptionException("Duplicate parameter " + param + " for feature " + name);
            } else {
                if(params.size() == 1) {
                    params = new HashMap<>(params);
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
    final Map<String, FeatureReferenceSpec> refs;
    final Map<String, FeatureParameterSpec> params;
    final List<FeatureParameterSpec> idParams;
    private final PackageDependencyGroupSpec localDeps;
    private final Map<String, PackageDependencyGroupSpec> externalDeps;


    private FeatureSpec(Builder builder) {
        this.name = builder.name;
        this.refs = builder.refs.size() > 1 ? Collections.unmodifiableMap(builder.refs) : builder.refs;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
        this.idParams = builder.idParams.size() > 1 ? Collections.unmodifiableList(builder.idParams) : builder.idParams;

        this.localDeps = builder.localDeps == null ? PackageDependencyGroupSpec.EMPTY_LOCAL : builder.localDeps.build();
        if(builder.externalDeps.isEmpty()) {
            externalDeps = Collections.emptyMap();
        } else {
            final int size = builder.externalDeps.size();
            if(size == 1) {
                final Map.Entry<String, PackageDependencyGroupSpec.Builder> entry = builder.externalDeps.entrySet().iterator().next();
                externalDeps = Collections.singletonMap(entry.getKey(), entry.getValue().build());
            } else {
                final Map<String, PackageDependencyGroupSpec> deps = new LinkedHashMap<>(size);
                for(Map.Entry<String, PackageDependencyGroupSpec.Builder> entry : builder.externalDeps.entrySet()) {
                    deps.put(entry.getKey(), entry.getValue().build());
                }
                externalDeps = Collections.unmodifiableMap(deps);
            }
        }
    }

    public String getName() {
        return name;
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

    @Override
    public boolean dependsOnPackages() {
        return !(localDeps.isEmpty() && externalDeps.isEmpty());
    }

    @Override
    public boolean dependsOnLocalPackages() {
        return !localDeps.isEmpty();
    }

    @Override
    public PackageDependencyGroupSpec getLocalPackageDependencies() {
        return localDeps;
    }

    @Override
    public boolean dependsOnExternalPackages() {
        return !externalDeps.isEmpty();
    }

    @Override
    public Collection<String> getPackageDependencySources() {
        return externalDeps.keySet();
    }

    @Override
    public PackageDependencyGroupSpec getExternalPackageDependencies(String groupName) {
        return externalDeps.get(groupName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalDeps == null) ? 0 : externalDeps.hashCode());
        result = prime * result + ((idParams == null) ? 0 : idParams.hashCode());
        result = prime * result + ((localDeps == null) ? 0 : localDeps.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((refs == null) ? 0 : refs.hashCode());
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
        if (externalDeps == null) {
            if (other.externalDeps != null)
                return false;
        } else if (!externalDeps.equals(other.externalDeps))
            return false;
        if (idParams == null) {
            if (other.idParams != null)
                return false;
        } else if (!idParams.equals(other.idParams))
            return false;
        if (localDeps == null) {
            if (other.localDeps != null)
                return false;
        } else if (!localDeps.equals(other.localDeps))
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
        if (refs == null) {
            if (other.refs != null)
                return false;
        } else if (!refs.equals(other.refs))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(name);
        if(!refs.isEmpty()) {
            buf.append(" refs=");
            final Iterator<FeatureReferenceSpec> i = refs.values().iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        if(!params.isEmpty()) {
            buf.append(" params=");
            final Iterator<FeatureParameterSpec> i = params.values().iterator();
            buf.append(i.next());
            while(i.hasNext()) {
                buf.append(',').append(i.next());
            }
        }
        return buf.append(']').toString();
    }
}
