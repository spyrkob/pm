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

package org.jboss.provisioning.state;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.parameters.BuilderWithParameters;
import org.jboss.provisioning.parameters.PackageParameter;

/**
 *
 * @author Alexey Loubyansky
 */
public interface ProvisionedPackage {

    class Builder implements BuilderWithParameters<Builder> {

        private final String name;
        private Map<String, PackageParameter> params = Collections.emptyMap();

        private Builder(String name) {
            this.name = name;
        }

        @Override
        public Builder addParameter(PackageParameter param) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(param.getName(), param);
                    break;
                case 1:
                    params = new HashMap<>(params);
                default:
                    params.put(param.getName(), param);
            }
            return this;
        }

        public ProvisionedPackage build() {
            return new ProvisionedPackageImpl(name, params.size() > 1 ? Collections.unmodifiableMap(params) : params);
        }
    }

    static Builder builder(String name) {
        return new Builder(name);
    }

    static ProvisionedPackage newInstance(String name) {
        return new ProvisionedPackageImpl(name);
    }

    String getName();

    boolean hasParameters();

    Collection<PackageParameter> getParameters();
}
