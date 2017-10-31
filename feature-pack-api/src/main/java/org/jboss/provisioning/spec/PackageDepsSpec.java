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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PackageDepsSpec {

    protected final PackageDependencyGroupSpec localPkgDeps;
    protected final Map<String, PackageDependencyGroupSpec> externalPkgDeps;

    protected PackageDepsSpec() {
        localPkgDeps = PackageDependencyGroupSpec.EMPTY_LOCAL;
        externalPkgDeps = Collections.emptyMap();
    }

    protected PackageDepsSpec(PackageDepsSpec src) {
        localPkgDeps = src.localPkgDeps;
        externalPkgDeps = src.externalPkgDeps;
    }

    protected PackageDepsSpec(PackageDepsSpecBuilder<?> builder) {
        this.localPkgDeps = builder.localPkgDeps == null ? PackageDependencyGroupSpec.EMPTY_LOCAL : builder.localPkgDeps.build();
        if(builder.externalPkgDeps.isEmpty()) {
            externalPkgDeps = Collections.emptyMap();
        } else {
            final int size = builder.externalPkgDeps.size();
            if(size == 1) {
                final Map.Entry<String, PackageDependencyGroupSpec.Builder> entry = builder.externalPkgDeps.entrySet().iterator().next();
                externalPkgDeps = Collections.singletonMap(entry.getKey(), entry.getValue().build());
            } else {
                final Map<String, PackageDependencyGroupSpec> deps = new LinkedHashMap<>(size);
                for(Map.Entry<String, PackageDependencyGroupSpec.Builder> entry : builder.externalPkgDeps.entrySet()) {
                    deps.put(entry.getKey(), entry.getValue().build());
                }
                externalPkgDeps = Collections.unmodifiableMap(deps);
            }
        }
    }

    public boolean hasPackageDeps() {
        return !(localPkgDeps.isEmpty() && externalPkgDeps.isEmpty());
    }

    public boolean hasLocalPackageDeps() {
        return !localPkgDeps.isEmpty();
    }

    public PackageDependencyGroupSpec getLocalPackageDeps() {
        return localPkgDeps;
    }

    public boolean hasExternalPackageDeps() {
        return !externalPkgDeps.isEmpty();
    }

    public Collection<String> getExternalPackageSources() {
        return externalPkgDeps.keySet();
    }

    public PackageDependencyGroupSpec getExternalPackageDeps(String groupName) {
        return externalPkgDeps.get(groupName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((externalPkgDeps == null) ? 0 : externalPkgDeps.hashCode());
        result = prime * result + ((localPkgDeps == null) ? 0 : localPkgDeps.hashCode());
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
        PackageDepsSpec other = (PackageDepsSpec) obj;
        if (externalPkgDeps == null) {
            if (other.externalPkgDeps != null)
                return false;
        } else if (!externalPkgDeps.equals(other.externalPkgDeps))
            return false;
        if (localPkgDeps == null) {
            if (other.localPkgDeps != null)
                return false;
        } else if (!localPkgDeps.equals(other.localPkgDeps))
            return false;
        return true;
    }
}
