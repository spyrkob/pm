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

package org.jboss.provisioning.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.parameters.ParameterResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class MapParameterResolver implements ParameterResolver {

    public static class Builder {
        private Map<String, String> params = Collections.emptyMap();
        private Builder() {
        }
        public Builder addParameter(String name, String value) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(name, value);
                    break;
                case 1:
                    params = new HashMap<>(params);
                default:
                    params.put(name, value);
            }
            return this;
        }
        public MapParameterResolver build() {
            return new MapParameterResolver(params.size() > 1 ? Collections.unmodifiableMap(params) : params);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, String> params;

    private MapParameterResolver(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String resolve(String paramName) {
        return params.get(paramName);
    }
}
