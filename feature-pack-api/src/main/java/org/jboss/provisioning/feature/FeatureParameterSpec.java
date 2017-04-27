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

import org.jboss.provisioning.ProvisioningDescriptionException;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureParameterSpec {

    public static FeatureParameterSpec create(String name) throws ProvisioningDescriptionException {
        return new FeatureParameterSpec(name, false, true, null);
    }

    public static FeatureParameterSpec create(String name, String value) throws ProvisioningDescriptionException {
        return new FeatureParameterSpec(name, false, false, value);
    }

    public static FeatureParameterSpec create(String name, boolean nillable) throws ProvisioningDescriptionException {
        return new FeatureParameterSpec(name, false, nillable, null);
    }

    public static FeatureParameterSpec createId(String name) throws ProvisioningDescriptionException {
        return new FeatureParameterSpec(name, true, false, null);
    }

    final String name;
    final boolean featureId;
    final boolean nillable;
    final String defaultValue;

    private FeatureParameterSpec(String name, boolean featureId, boolean nillable, String defaultValue) throws ProvisioningDescriptionException {
        if(featureId && nillable) {
            throw new ProvisioningDescriptionException("ID parameter " + name + " cannot be nillable.");
        }
        this.name = name;
        this.featureId = featureId;
        this.nillable = nillable;
        this.defaultValue = defaultValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result + (featureId ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (nillable ? 1231 : 1237);
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
        FeatureParameterSpec other = (FeatureParameterSpec) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null)
                return false;
        } else if (!defaultValue.equals(other.defaultValue))
            return false;
        if (featureId != other.featureId)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nillable != other.nillable)
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[').append(name);
        if(defaultValue != null) {
            buf.append('=').append(defaultValue);
        }
        if(featureId) {
            buf.append(" featureId");
        }
        if(nillable) {
            buf.append(" nillable");
        }
        return buf.append(']').toString();
    }
}
