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
package org.jboss.provisioning.util.plugin;

import java.nio.file.Path;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.FeaturePackLayoutDescription;

/**
 *
 * @author Alexey Loubyansky
 */
public interface ProvisioningContext {

    String getEncoding();

    FeaturePackLayoutDescription getInstallationDescription();

    Path getLayoutDir();

    Path getInstallDir();

    Path getResourcesDir();

    Path resolveArtifact(ArtifactCoords coords) throws ProvisioningException;
}
