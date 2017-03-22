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
package org.jboss.provisioning.plugin;

import java.nio.file.Path;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.state.ProvisionedState;

/**
 * Provisioning context available for a provisioning plug-in.
 *
 * @author Alexey Loubyansky
 */
public interface ProvisioningContext {

    /**
     * The configured encoding.
     *
     * @return  character encoding
     */
    String getEncoding();

    /**
     * The target installation location
     *
     * @return  installation location
     */
    Path getInstallDir();

    /**
     * Configuration of the installation to be provisioned.
     *
     * @return  installation configuration
     */
    ProvisioningConfig getProvisioningConfig();

    /**
     * Resolved provisioned state.
     *
     * @return  resolved provisioned state
     */
    ProvisionedState getProvisionedState();

    /**
     * Returns feature-pack specification for the given GAV.
     *
     * @return  feature-pack specification
     */
    FeaturePackSpec getFeaturePackSpec(ArtifactCoords.Gav fpGav);

    /**
     * Returns a resource path for the provisioning setup.
     *
     * @return  file-system path for the resource
     */
    Path getResource(String... path);

    /**
     * Returns a resource path for a feature-pack.
     *
     * @param fpGav  GAV of the feature-pack
     * @param path  path to the resource relative to the feature-pack resources directory
     * @return  file-system path for the resource
     * @throws ProvisioningDescriptionException  in case the feature-pack was not found in the layout
     */
    Path getFeaturePackResource(ArtifactCoords.Gav fpGav, String... path) throws ProvisioningDescriptionException;

    /**
     * Returns a resource path for a package.
     *
     * @param fpGav  GAV of the feature-pack containing the package
     * @param pkgName  name of the package
     * @param path  path to the resource relative to the package resources directory
     * @return  file-system path for the resource
     * @throws ProvisioningDescriptionException  in case the feature-pack or package were not found in the layout
     */
    Path getPackageResource(ArtifactCoords.Gav fpGav, String pkgName, String... path) throws ProvisioningDescriptionException;

    /**
     * Returns a path for a temporary file-system resource.
     *
     * @return  temporary file-system path
     */
    Path getTmpPath(String... path);

    /**
     * Resolves the location of the artifact given its coordinates.
     *
     * @param coords  artifact coordinates
     * @return  location of the artifact
     * @throws ArtifactResolutionException  in case the artifact could not be
     * resolved for any reason
     */
    Path resolveArtifact(ArtifactCoords coords) throws ArtifactResolutionException;
}
