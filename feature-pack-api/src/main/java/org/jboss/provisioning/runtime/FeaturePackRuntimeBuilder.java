/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.type.ParameterTypeProvider;
import org.jboss.provisioning.type.builtin.BuiltInParameterTypeProvider;
import org.jboss.provisioning.util.PmCollections;
import org.jboss.provisioning.xml.FeatureGroupXmlParser;
import org.jboss.provisioning.xml.FeatureSpecXmlParser;

/**
 *
 * @author Alexey Loubyansky
 */
class FeaturePackRuntimeBuilder {
    final ArtifactCoords.Gav gav;
    final Path dir;
    final FeaturePackSpec spec;
    boolean ordered;
    Map<String, ResolvedFeatureSpec> featureSpecs = null;
    private Map<String, FeatureGroup> fgSpecs = null;

    Map<String, PackageRuntime.Builder> pkgBuilders = Collections.emptyMap();
    List<String> pkgOrder = new ArrayList<>();

    private ParameterTypeProvider featureParamTypeProvider = BuiltInParameterTypeProvider.getInstance();

    FeaturePackRuntimeBuilder(ArtifactCoords.Gav gav, FeaturePackSpec spec, Path dir) {
        this.gav = gav;
        this.dir = dir;
        this.spec = spec;
    }

    PackageRuntime.Builder newPackage(String name, Path dir) {
        final PackageRuntime.Builder pkgBuilder = PackageRuntime.builder(name, dir);
        pkgBuilders = PmCollections.put(pkgBuilders, name, pkgBuilder);
        return pkgBuilder;
    }

    void addPackage(String name) {
        pkgOrder.add(name);
    }

    FeatureGroup getFeatureGroupSpec(String name) throws ProvisioningException {
        FeatureGroup fgSpec = null;
        if(fgSpecs == null) {
            fgSpecs = new HashMap<>();
        } else {
            fgSpec = fgSpecs.get(name);
        }
        if(fgSpec == null) {
            final Path specXml = dir.resolve(Constants.FEATURE_GROUPS).resolve(name + ".xml");
            if (Files.exists(specXml)) {
                try (BufferedReader reader = Files.newBufferedReader(specXml)) {
                    fgSpec = FeatureGroupXmlParser.getInstance().parse(reader);
                } catch (Exception e) {
                    throw new ProvisioningException(Errors.parseXml(specXml), e);
                }
                fgSpecs.put(name, fgSpec);
            } else {
                // TODO look for the group in the fp deps
                throw new ProvisioningDescriptionException(Errors.pathDoesNotExist(specXml));
            }
        }
        return fgSpec;
    }

    ResolvedFeatureSpec getFeatureSpec(String name) throws ProvisioningException {
        ResolvedFeatureSpec resolvedSpec = null;
        if(featureSpecs == null) {
            featureSpecs = new HashMap<>();
        } else {
            resolvedSpec = featureSpecs.get(name);
        }
        if(resolvedSpec == null) {
            final Path specXml = dir.resolve(Constants.FEATURES).resolve(name).resolve(Constants.SPEC_XML);
            if (Files.exists(specXml)) {
                final FeatureSpec xmlSpec;
                try (BufferedReader reader = Files.newBufferedReader(specXml)) {
                    xmlSpec = FeatureSpecXmlParser.getInstance().parse(reader);
                } catch (Exception e) {
                    throw new ProvisioningDescriptionException(Errors.parseXml(specXml), e);
                }
                resolvedSpec = new ResolvedFeatureSpec(new ResolvedSpecId(gav, xmlSpec.getName()), featureParamTypeProvider, xmlSpec);
                featureSpecs.put(name, resolvedSpec);
            } else {
                // TODO look for the spec in the fp deps
                throw new ProvisioningDescriptionException("Failed to locate feature spec '" + name + "' in " + gav);
            }
        }
        return resolvedSpec;
    }

    FeaturePackRuntime build() throws ProvisioningException {
        return new FeaturePackRuntime(this);
    }
}