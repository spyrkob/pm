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
import java.util.Map.Entry;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.parameters.PackageParameterResolver;
import org.jboss.provisioning.parameters.ParameterResolver;

/**
 * Global installation parameter resolver. Parameter can be set on any of the
 * following scopes: package, feature-pack, installation.
 *
 * The resolver follows the following algorithm:
 * 1) check whether the parameter was set on the package level and if it was
 * return the value;
 * 2) check whether the parameter was set on the feature-pack level and if it
 * was return the value;
 * 3) check whether the parameter was set on the installation level and if it
 * was return the value;
 * 4) return the default package value.
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningParameters implements PackageParameterResolver, ParameterResolver {

    public static class Builder {

        private Map<ArtifactCoords.Gav, Map<String, MapParameterResolver.Builder>> pkgParams = Collections.emptyMap();
        private Map<ArtifactCoords.Gav, MapParameterResolver.Builder> fpParams = Collections.emptyMap();
        private MapParameterResolver.Builder installationParams;

        private Builder() {
        }

        public Builder add(ArtifactCoords.Gav fpGav, String pkgName, String paramName, String value) {
            Map<String, MapParameterResolver.Builder> map = pkgParams.get(fpGav);
            if(map == null) {
                map = Collections.singletonMap(pkgName, MapParameterResolver.builder().addParameter(paramName, value));
                putPackageBuilders(fpGav, map);
            } else {
                final MapParameterResolver.Builder builder = map.get(pkgName);
                if(builder == null) {
                    if(map.size() == 1) {
                        map = new HashMap<>(map);
                        putPackageBuilders(fpGav, map);
                    }
                    map.put(pkgName, MapParameterResolver.builder().addParameter(paramName, value));
                } else {
                    builder.addParameter(paramName, value);
                }
            }
            return this;
        }

        public Builder add(ArtifactCoords.Gav fpGav, String paramName, String value) {
            MapParameterResolver.Builder builder = fpParams.get(fpGav);
            if(builder == null) {
                builder = MapParameterResolver.builder().addParameter(paramName, value);
                switch(fpParams.size()) {
                    case 0:
                        fpParams = Collections.singletonMap(fpGav, builder);
                        break;
                    case 1:
                        fpParams = new HashMap<>(fpParams);
                    default:
                        fpParams.put(fpGav, builder);
                }
            } else {
                builder.addParameter(paramName, value);
            }
            return this;
        }

        public Builder add(String paramName, String value) {
            if(installationParams == null) {
                installationParams = MapParameterResolver.builder();
            }
            installationParams.addParameter(paramName, value);
            return this;
        }

        private void putPackageBuilders(ArtifactCoords.Gav fpGav, Map<String, MapParameterResolver.Builder> map) {
            switch(pkgParams.size()) {
                case 0:
                    pkgParams = Collections.singletonMap(fpGav, map);
                    break;
                case 1:
                    pkgParams = new HashMap<>(pkgParams);
                default:
                    pkgParams.put(fpGav, map);
            }
        }

        public ProvisioningParameters build() {
            return new ProvisioningParameters(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<ArtifactCoords.Gav, Map<String, ParameterResolver>> pkgParams;
    private final Map<ArtifactCoords.Gav, ParameterResolver> fpParams;
    private final ParameterResolver installationParams;

    private ArtifactCoords.Gav targetGav;
    private String targetPkg;

    private ProvisioningParameters(Builder builder) {
        if(builder.pkgParams.isEmpty()) {
            pkgParams = Collections.emptyMap();
        } else {
            if(builder.pkgParams.size() == 1) {
                final Map.Entry<ArtifactCoords.Gav, Map<String, MapParameterResolver.Builder>> entry = builder.pkgParams.entrySet().iterator().next();
                pkgParams = Collections.singletonMap(entry.getKey(), build(entry.getValue()));
            } else {
                pkgParams = new HashMap<>(builder.pkgParams.size());
                for(Map.Entry<ArtifactCoords.Gav, Map<String, MapParameterResolver.Builder>> entry : builder.pkgParams.entrySet()) {
                    pkgParams.put(entry.getKey(), build(entry.getValue()));
                }
            }
        }
        fpParams = build(builder.fpParams);
        installationParams = builder.installationParams == null ? null : builder.installationParams.build();
    }

    private <T> Map<T, ParameterResolver> build(Map<T, MapParameterResolver.Builder> builders) {
        if(builders.isEmpty()) {
            return Collections.emptyMap();
        }
        if(builders.size() == 1) {
            final Entry<T, MapParameterResolver.Builder> entry = builders.entrySet().iterator().next();
            return Collections.singletonMap(entry.getKey(), entry.getValue().build());
        }
        final Map<T, ParameterResolver> result = new HashMap<>(builders.size());
        for(Map.Entry<T, MapParameterResolver.Builder> entry : builders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().build());
        }
        return result;
    }

    @Override
    public ParameterResolver getResolver(ArtifactCoords.Gav fpGav, String pkgName) throws ProvisioningException {
        targetGav = fpGav;
        targetPkg = pkgName;
        return this;
    }

    @Override
    public String resolve(String paramName) {
        assert targetGav != null : "Feature-pack GAV is null";
        assert targetPkg != null : "Package name is null";
        ParameterResolver resolver = pkgParams.get(targetGav).get(targetPkg);
        if(resolver != null) {
            String value = resolver.resolve(paramName);
            if(value != null) {
                return value;
            }
        }
        resolver = fpParams.get(targetGav);
        if(resolver != null) {
            String value = resolver.resolve(paramName);
            if(value != null) {
                return value;
            }
        }
        if(installationParams != null) {
            return installationParams.resolve(paramName);
        }
        return null;
    }
}
