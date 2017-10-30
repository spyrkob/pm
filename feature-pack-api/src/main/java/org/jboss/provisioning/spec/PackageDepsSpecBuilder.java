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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PackageDepsSpecBuilder<T extends PackageDepsSpecBuilder<T>> {

    protected PackageDependencyGroupSpec.Builder localPkgDeps;
    protected Map<String, PackageDependencyGroupSpec.Builder> externalPkgDeps = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    public T addPackageDep(String packageName) {
        getLocalGroupBuilder().addDependency(packageName);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addPackageDep(String packageName, boolean optional) {
        getLocalGroupBuilder().addDependency(packageName, optional);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addPackageDep(PackageDependencySpec dep) {
        getLocalGroupBuilder().addDependency(dep);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addPackageDep(String fpDep, String packageName) {
        getExternalGroupBuilder(fpDep).addDependency(packageName);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addPackageDep(String fpDep, String packageName, boolean optional) {
        getExternalGroupBuilder(fpDep).addDependency(packageName, optional);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T addPackageDep(String fpDep, PackageDependencySpec dep) {
        getExternalGroupBuilder(fpDep).addDependency(dep);
        return (T) this;
    }

    public boolean hasPackageDeps() {
        return localPkgDeps != null || !externalPkgDeps.isEmpty();
    }

    private PackageDependencyGroupSpec.Builder getLocalGroupBuilder() {
        if(localPkgDeps == null) {
            localPkgDeps = PackageDependencyGroupSpec.builder();
        }
        return localPkgDeps;
    }

    private PackageDependencyGroupSpec.Builder getExternalGroupBuilder(String groupName) {
        PackageDependencyGroupSpec.Builder groupBuilder = externalPkgDeps.get(groupName);
        if(groupBuilder == null) {
            groupBuilder = PackageDependencyGroupSpec.builder(groupName);
            switch(externalPkgDeps.size()) {
                case 0:
                    externalPkgDeps = Collections.singletonMap(groupName, groupBuilder);
                    break;
                case 1:
                    externalPkgDeps = new LinkedHashMap<>(externalPkgDeps);
                default:
                    externalPkgDeps.put(groupName, groupBuilder);
            }
        }
        return groupBuilder;
    }
}