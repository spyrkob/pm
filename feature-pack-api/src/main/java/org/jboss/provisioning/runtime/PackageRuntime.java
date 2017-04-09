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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.state.ProvisionedPackage;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageRuntime implements ProvisionedPackage {

    static class Builder {
        final Path dir;
        final PackageConfig.Builder configBuilder;
        PackageSpec spec;
        private Map<String, PackageParameter> params = Collections.emptyMap();

        private Builder(String name, Path dir) {
            this.dir = dir;
            this.configBuilder = PackageConfig.builder(name);
        }

        void addParameter(PackageParameter param) {
            switch(params.size()) {
                case 0:
                    params = Collections.singletonMap(param.getName(), param);
                    break;
                case 1:
                    params = new HashMap<>(params);
                default:
                    params.put(param.getName(), param);
            }
        }

        PackageRuntime build() {
            return new PackageRuntime(this);
        }
    }

    static Builder builder(String name, Path dir) {
        return new Builder(name, dir);
    }

    private final PackageSpec spec;
    private final Path layoutDir;
    private final Map<String, PackageParameter> params;

    private PackageRuntime(Builder builder) {
        this.spec = builder.spec;
        this.layoutDir = builder.dir;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;
    }

    public PackageSpec getSpec() {
        return spec;
    }

    @Override
    public String getName() {
        return spec.getName();
    }

    @Override
    public boolean hasParameters() {
        return !params.isEmpty();
    }

    @Override
    public Collection<PackageParameter> getParameters() {
        return params.values();
    }

    public PackageParameter getParameter(String name) {
        return params.get(name);
    }

    /**
     * Returns a resource path for a package.
     *
     * @param fpGav  GAV of the feature-pack containing the package
     * @param pkgName  name of the package
     * @param path  path to the resource relative to the package resources directory
     * @return  file-system path for the resource
     * @throws ProvisioningDescriptionException  in case the feature-pack or package were not found in the layout
     */
    public Path getResource(String... path) throws ProvisioningDescriptionException {
        if(path.length == 0) {
            throw new IllegalArgumentException("Resource path is null");
        }
        if(path.length == 1) {
            return layoutDir.resolve(path[0]);
        }
        Path p = layoutDir;
        for(String name : path) {
            p = p.resolve(name);
        }
        return p;
    }

    public Path getContentDir() {
        return layoutDir.resolve(Constants.CONTENT);
    }
}
