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

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureDependencySpec {

    public static FeatureDependencySpec create(FeatureId featureId) throws ProvisioningDescriptionException {
        return create(featureId, null, false);
    }

    public static FeatureDependencySpec create(FeatureId featureId, String fpDep) throws ProvisioningDescriptionException {
        return create(featureId, fpDep, false);
    }

    public static FeatureDependencySpec create(FeatureId featureId, boolean include) throws ProvisioningDescriptionException {
        return create(featureId, null, include);
    }

    public static FeatureDependencySpec create(FeatureId featureId, String fpDep, boolean include) throws ProvisioningDescriptionException {
        return new FeatureDependencySpec(featureId, fpDep, include);
    }

    final String dependency;
    final FeatureId featureId;
    final boolean include;

    private FeatureDependencySpec(FeatureId featureId, String fpDep, boolean include) throws ProvisioningDescriptionException {
        this.featureId = featureId;
        this.dependency = fpDep;
        this.include = include;
    }

    public String getDependency() {
        return dependency;
    }

    public FeatureId getFeatureId() {
        return featureId;
    }

    public boolean isInclude() {
        return include;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependency == null) ? 0 : dependency.hashCode());
        result = prime * result + ((featureId == null) ? 0 : featureId.hashCode());
        result = prime * result + (include ? 1231 : 1237);
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
        FeatureDependencySpec other = (FeatureDependencySpec) obj;
        if (dependency == null) {
            if (other.dependency != null)
                return false;
        } else if (!dependency.equals(other.dependency))
            return false;
        if (featureId == null) {
            if (other.featureId != null)
                return false;
        } else if (!featureId.equals(other.featureId))
            return false;
        if (include != other.include)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if(dependency != null) {
            buf.append(" fpDep=").append(dependency);
        }
        buf.append(" feature=").append(featureId);
        if(include) {
            buf.append(" auto-includes");
        }
        return buf.append(']').toString();
    }
}
