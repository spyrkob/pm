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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.feature.FeatureConfig;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolvedFeatureGroupConfig {

    boolean inheritFeatures = true;
    Set<ResolvedSpecId> includedSpecs = Collections.emptySet();
    Map<ResolvedFeatureId, FeatureConfig> includedFeatures = Collections.emptyMap();
    Set<ResolvedSpecId> excludedSpecs = Collections.emptySet();
    Set<ResolvedFeatureId> excludedFeatures = Collections.emptySet();

    ResolvedFeatureGroupConfig() {
    }

    ResolvedFeatureGroupConfig setInheritFeatures(boolean inheritFeatures) {
        this.inheritFeatures = inheritFeatures;
        return this;
    }

    public boolean hasExcludedSpecs() {
        return !excludedSpecs.isEmpty();
    }

    public boolean hasExcludedFeatures() {
        return !excludedFeatures.isEmpty();
    }

}
