/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.FeaturePackLayoutDescription;
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
     * Description of the feature-pack layout out of which the target
     * installation is provisioned.
     *
     * @return  feature-pack layout description
     */
    FeaturePackLayoutDescription getLayoutDescription();

    /**
     * Feature-pack layout location.
     *
     * @return  feature-pack layout location
     */
    Path getLayoutDir();

    /**
     * The target installation location
     *
     * @return  installation location
     */
    Path getInstallDir();

    /**
     * Feature-pack layout resources location the plug-ins may use as a source
     * of data.
     *
     * @return  resources location
     */
    Path getResourcesDir();

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
