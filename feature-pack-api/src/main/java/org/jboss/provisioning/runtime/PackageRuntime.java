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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;
import org.jboss.provisioning.spec.PackageSpec;
import org.jboss.provisioning.state.ProvisionedPackage;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.xml.ParameterSetXmlParser;

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
        private Map<String, ParameterSet> configs = Collections.emptyMap();

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

        void addConfig(ParameterSet config) {
            switch(configs.size()) {
                case 0:
                    configs = Collections.singletonMap(config.getName(), config);
                    break;
                case 1:
                    if(configs.containsKey(config.getName())) {
                        configs = Collections.singletonMap(config.getName(),config);
                        break;
                    }
                    configs = new HashMap<>(configs);
                default:
                    configs.put(config.getName(), config);

            }
        }

        PackageRuntime build() throws ProvisioningException {
            return new PackageRuntime(this);
        }
    }

    static Builder builder(String name, Path dir) {
        return new Builder(name, dir);
    }

    private final PackageSpec spec;
    private final Path layoutDir;
    private final Map<String, PackageParameter> params;
    private final Map<String, ParameterSet> configs;


    private PackageRuntime(Builder builder) throws ProvisioningException {
        this.spec = builder.spec;
        this.layoutDir = builder.dir;
        this.params = builder.params.size() > 1 ? Collections.unmodifiableMap(builder.params) : builder.params;

        if(builder.configs.isEmpty()) {
            this.configs = builder.configs;
        }  else if(builder.configs.size() == 1) {
            final ParameterSet config = normalize(builder.configs.values().iterator().next());
            this.configs = Collections.singletonMap(config.getName(), config);
        } else {
            final Map<String, ParameterSet> tmp = new HashMap<>(builder.configs.size());
            for(ParameterSet config : builder.configs.values()) {
                tmp.put(config.getName(), normalize(config));
            }
            this.configs = Collections.unmodifiableMap(tmp);
        }
    }

    private ParameterSet normalize(ParameterSet config) throws ProvisioningException {
        if(!config.containsAll(params.keySet())) {
            ParameterSet originalConfig = null;
            final Path configXml = LayoutUtils.getPackageConfigsDir(layoutDir).resolve(config.getName() + ".xml");
            if(Files.exists(configXml)) {
                try (BufferedReader input = Files.newBufferedReader(configXml)) {
                    originalConfig = ParameterSetXmlParser.getInstance().parse(input);
                } catch (IOException e) {
                    throw new ProvisioningException(Errors.readFile(configXml), e);
                } catch (XMLStreamException e) {
                    throw new ProvisioningException(Errors.parseXml(configXml), e);
                }
            }
            final ParameterSet.Builder configBuilder = ParameterSet.builder(config.getName());
            for(PackageParameter param : params.values()) {
                final PackageParameter override = config.getParameter(param.getName());
                if(override == null) {
                    if(originalConfig != null && originalConfig.hasParameter(param.getName())) {
                        configBuilder.addParameter(originalConfig.getParameter(param.getName()));
                    } else {
                        configBuilder.addParameter(param);
                    }
                } else {
                    configBuilder.addParameter(override);
                }
            }
            return configBuilder.build();
        }
        return config;
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

    @Override
    public boolean hasConfigs() {
        return !configs.isEmpty();
    }

    @Override
    public Collection<ParameterSet> getConfigs() {
        return configs.values();
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
