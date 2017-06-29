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

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.runtime.ResolvedSpecId;
import org.jboss.provisioning.state.ProvisionedFeature;


/**
 *
 * @author Alexey Loubyansky
 */
public interface ProvisionedConfigHandler {

    default void prepare() throws ProvisioningException {};

    default void nextFeaturePack(ArtifactCoords.Gav fpGav) throws ProvisioningException {};

    default void nextSpec(ResolvedSpecId specId) throws ProvisioningException {};

    default void nextFeature(ProvisionedFeature feature) throws ProvisioningException {};

    default void done() throws ProvisioningException {};
}