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

package org.jboss.provisioning.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
abstract class PackageDepsConfigBuilder<T extends PackageDepsConfigBuilder<T>> {

    protected boolean inheritPackages = true;
    protected Set<String> excludedPackages = Collections.emptySet();
    protected Map<String, PackageConfig> includedPackages = Collections.emptyMap();

    @SuppressWarnings("unchecked")
    public T setInheritPackages(boolean inheritSelectedPackages) {
        this.inheritPackages = inheritSelectedPackages;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T excludePackage(String packageName) throws ProvisioningDescriptionException {
        if(includedPackages.containsKey(packageName)) {
            throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageName));
        }
        excludedPackages = PmCollections.add(excludedPackages, packageName);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T excludeAllPackages(Collection<String> packageNames) throws ProvisioningDescriptionException {
        for(String packageName : packageNames) {
            excludePackage(packageName);
        }
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T includeAllPackages(Collection<PackageConfig> packageConfigs) throws ProvisioningDescriptionException {
        for(PackageConfig packageConfig : packageConfigs) {
            includePackage(packageConfig);
        }
        return (T) this;
    }

    public T includePackage(String packageName) throws ProvisioningDescriptionException {
        return includePackage(PackageConfig.forName(packageName));
    }

    @SuppressWarnings("unchecked")
    private T includePackage(PackageConfig packageConfig) throws ProvisioningDescriptionException {
        if(excludedPackages.contains(packageConfig.getName())) {
            throw new ProvisioningDescriptionException(Errors.packageExcludeInclude(packageConfig.getName()));
        }
        includedPackages = PmCollections.put(includedPackages, packageConfig.getName(), packageConfig);
        return (T) this;
    }

}
