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

package org.jboss.provisioning.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.runtime.ResolvedFeatureId;
import org.jboss.provisioning.runtime.ResolvedSpecId;
import org.jboss.provisioning.state.ProvisionedFeature;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedFeatureBuilder implements ProvisionedFeature {

    public static ProvisionedFeatureBuilder builder(ResolvedFeatureId id) {
        return new ProvisionedFeatureBuilder(id, id.getSpecId());
    }

    public static ProvisionedFeatureBuilder builder(ResolvedSpecId id) {
        return new ProvisionedFeatureBuilder(null, id);
    }

    public static ProvisionedFeatureBuilder builder(ResolvedFeatureId id, ResolvedSpecId specId) {
        return new ProvisionedFeatureBuilder(id, specId);
    }

    private final ResolvedFeatureId id;
    private final ResolvedSpecId specId;
    private Map<String, String> params = Collections.emptyMap();

    private ProvisionedFeatureBuilder(ResolvedFeatureId id, ResolvedSpecId specId) {
        this.id = id;
        this.specId = specId;
        if(id != null) {
            params = id.getParams();
            if(params.size() > 1) {
                params = new HashMap<>(params);
            }
        }
    }

    public ProvisionedFeatureBuilder setParam(String name, String value) {
        if(params.isEmpty()) {
            params = Collections.singletonMap(name, value);
            return this;
        }
        if(params.size() == 1) {
            if(params.containsKey(name)) {
                params = Collections.singletonMap(name, value);
                return this;
            }
            final Map.Entry<String, String> entry = params.entrySet().iterator().next();
            params = new HashMap<>(2);
            params.put(entry.getKey(), entry.getValue());
        }
        params.put(name, value);
        return this;
    }

    public ProvisionedFeature build() {
        if(params.size() > 1) {
            params = Collections.unmodifiableMap(params);
        }
        return this;
    }

    @Override
    public boolean hasId() {
        return id != null;
    }

    @Override
    public ResolvedFeatureId getId() {
        return id;
    }

    @Override
    public ResolvedSpecId getSpecId() {
        return specId;
    }

    @Override
    public boolean hasParams() {
        return !params.isEmpty();
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        result = prime * result + ((specId == null) ? 0 : specId.hashCode());
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
        ProvisionedFeatureBuilder other = (ProvisionedFeatureBuilder) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        if (specId == null) {
            if (other.specId != null)
                return false;
        } else if (!specId.equals(other.specId))
            return false;
        return true;
    }
}
