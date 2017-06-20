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
import java.util.List;

import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.parameters.PackageParameter;

/**
 *
 * @author Alexey Loubyansky
 */
class FPConfigStack {
    private List<FeaturePackConfig> stack;
    private FeaturePackConfig blockedPackageInheritance;
    private FeaturePackConfig blockedConfigInheritance;

    FPConfigStack(FeaturePackConfig fpConfig) {
        stack = Collections.singletonList(fpConfig);
    }

    void push(FeaturePackConfig fpConfig) {
        if(stack.isEmpty()) {
            stack = Collections.singletonList(fpConfig);
        } else {
            if(stack.size() == 1) {
                final FeaturePackConfig first = stack.get(0);
                stack = new ArrayList<>(2);
                stack.add(first);
            }
            stack.add(fpConfig);
        }
        if(blockedPackageInheritance == null && !fpConfig.isInheritPackages()) {
            blockedPackageInheritance = fpConfig;
        }
        if(blockedConfigInheritance == null && !fpConfig.isInheritConfigs()) {
            blockedConfigInheritance = fpConfig;
        }
    }

    boolean isInheritPackages() {
        return blockedPackageInheritance == null;
    }

    boolean isInheritConfigs() {
        return blockedConfigInheritance == null;
    }

    FeaturePackConfig pop() {
        final FeaturePackConfig popped;
        if (stack.size() == 1) {
            popped = stack.get(0);
            stack = Collections.emptyList();
        } else {
            popped = stack.remove(stack.size() - 1);
            if (stack.size() == 1) {
                stack = Collections.singletonList(stack.get(0));
            }
        }
        if(popped == blockedPackageInheritance) {
            blockedPackageInheritance = null;
        }
        if(popped == blockedConfigInheritance) {
            blockedConfigInheritance = null;
        }
        return popped;
    }

    boolean isPackageIncluded(String packageName, Collection<PackageParameter> params) {
        int i = stack.size() - 1;
        while(i >= 0) {
            final FeaturePackConfig fpConfig = stack.get(i--);
            final PackageConfig stackedPkg = fpConfig.getIncludedPackage(packageName);
            if(stackedPkg != null) {
                if(!params.isEmpty()) {
                    boolean allParamsOverwritten = true;
                    for(PackageParameter param : params) {
                        if(stackedPkg.getParameter(param.getName()) == null) {
                            allParamsOverwritten = false;
                            break;
                        }
                    }
                    if(allParamsOverwritten) {
                        return true;
                    }
                } else {
                   return true;
                }
            }
        }
        return false;
    }

    boolean isPackageExcluded(String packageName) {
        int i = stack.size() - 1;
        while(i >= 0) {
            if(stack.get(i--).isExcluded(packageName)) {
                return true;
            }
        }
        return false;
    }

    boolean isConfigExcluded(Config config) {
        int i = stack.size() - 1;
        while(i >= 0) {
            final FeaturePackConfig fpConfig = stack.get(i--);
            if (fpConfig.isConfigExcluded(config.getModel(), config.getName())) {
                return true;
            }
            if(fpConfig.isFullModelExcluded(config.getModel())) {
                return !fpConfig.isConfigIncluded(config.getModel(), config.getName());
            }
            if (!fpConfig.isInheritConfigs()) {
                return !fpConfig.isFullModelIncluded(config.getModel()) && !fpConfig.isConfigIncluded(config.getModel(), config.getName());
            }
        }
        return false;
    }

    boolean isConfigIncluded(Config config) {
        int i = stack.size() - 1;
        while(i >= 0) {
            final FeaturePackConfig fpConfig = stack.get(i--);
            if(fpConfig.isConfigIncluded(config.getModel(), config.getName())) {
                return true;
            }
            if(fpConfig.isFullModelIncluded(config.getModel())) {
                return !fpConfig.isConfigExcluded(config.getModel(), config.getName());
            }
            if(fpConfig.isInheritConfigs()) {
                return !fpConfig.isFullModelExcluded(config.getModel()) && !fpConfig.isConfigExcluded(config.getModel(), config.getName());
            }
        }
        return false;
    }
}