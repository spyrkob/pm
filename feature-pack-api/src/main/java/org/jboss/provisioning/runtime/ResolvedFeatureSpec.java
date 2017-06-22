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

import java.util.Map;

import org.jboss.provisioning.feature.FeatureSpec;

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
