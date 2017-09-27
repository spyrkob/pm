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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureReferenceSpec {

    public static class Builder {

        private String dependency;
        private final String featureSpec;
        private String name;
        private boolean nillable;
        private Map<String, String> paramMapping = null;

        private Builder(String spec) {
            this.featureSpec = spec;
            this.name = featureSpec.toString();
        }

        public Builder setDependency(String fpDep) {
            this.dependency = fpDep;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNillable(boolean nillable) {
            this.nillable = nillable;
            return this;
        }

        public Builder mapParam(String localName, String targetName) {
            if(paramMapping == null) {
                paramMapping = Collections.singletonMap(localName, targetName);
            } else {
                if(paramMapping.size() == 1) {
                    paramMapping = new HashMap<>(paramMapping);
                }
                paramMapping.put(localName, targetName);
            }
            return this;
        }

        public FeatureReferenceSpec build() throws ProvisioningDescriptionException {
            return new FeatureReferenceSpec(dependency, name, featureSpec, nillable, paramMapping);
        }
    }

    public static Builder builder(String feature) {
        return new Builder(feature);
    }

    public static FeatureReferenceSpec create(String str) throws ProvisioningDescriptionException {
        return create(str, str, false);
    }

    public static FeatureReferenceSpec create(String str, boolean nillable) throws ProvisioningDescriptionException {
        return create(str, str, nillable);
    }

    public static FeatureReferenceSpec create(String name, String feature, boolean nillable) throws ProvisioningDescriptionException {
        return new FeatureReferenceSpec(null, name, feature, nillable, null);
    }

    private static final String[] EMPTY_ARR = new String[0];

    final String dependency;
    final String name;
    final SpecId feature;
    final boolean nillable;
    final String[] localParams;
    final String[] targetParams;

    private FeatureReferenceSpec(String dependency, String name, String featureSpec, boolean nillable, Map<String, String> paramMapping) throws ProvisioningDescriptionException {
        this.dependency = dependency;
        this.name = name;
        this.feature = SpecId.fromString(featureSpec);
        this.nillable = nillable;
        if(paramMapping == null || paramMapping.isEmpty()) {
            this.localParams = EMPTY_ARR;
            this.targetParams = EMPTY_ARR;
        } else {
            this.localParams = new String[paramMapping.size()];
            this.targetParams = new String[paramMapping.size()];
            int i = 0;
            for (Map.Entry<String, String> mapping : paramMapping.entrySet()) {
                localParams[i] = mapping.getKey();
                targetParams[i++] = mapping.getValue();
            }
        }
    }

    public String getDependency() {
        return dependency;
    }

    public String getName() {
        return name;
    }

    public SpecId getFeature() {
        return feature;
    }

    public boolean isNillable() {
        return nillable;
    }

    public int getParamsMapped() {
        return localParams.length;
    }

    public String getLocalParam(int i) {
        return localParams[i];
    }

    public String getTargetParam(int i) {
        return targetParams[i];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dependency == null) ? 0 : dependency.hashCode());
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        result = prime * result + Arrays.hashCode(localParams);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(targetParams);
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
        FeatureReferenceSpec other = (FeatureReferenceSpec) obj;
        if (dependency == null) {
            if (other.dependency != null)
                return false;
        } else if (!dependency.equals(other.dependency))
            return false;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;
        if (!Arrays.equals(localParams, other.localParams))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nillable != other.nillable)
            return false;
        if (!Arrays.equals(targetParams, other.targetParams))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(name);
        if(dependency != null) {
            buf.append(" fpDep=").append(dependency);
        }
        buf.append(" feature=").append(feature);
        if(nillable) {
            buf.append(" nillable");
        }
        if(localParams.length > 0) {
            buf.append(' ');
            buf.append(localParams[0]).append('=').append(targetParams[0]);
            for (int i = 1; i < localParams.length; ++i) {
                buf.append(',').append(localParams[i]).append('=').append(targetParams[i]);
            }
        }
        return buf.append(']').toString();
    }
}
