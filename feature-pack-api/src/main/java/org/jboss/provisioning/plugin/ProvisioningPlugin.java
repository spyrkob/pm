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

import org.jboss.provisioning.ProvisioningException;

/**
 * Provisioning plug-in can be referenced from a feature-pack configuration.
 *
 * The corresponding plug-in callback methods are called:
 * - before an installation takes place;
 * - after the installation has been performed;
 * - before a removal of feature-packs;
 * - after the removal of feature-packs has been performed.
 *
 * Examples of such post-provisioning tasks could be:
 * - adjust the configuration;
 * - set file permissions;
 * - create/remove directory structures;
 * - etc.
 *
 * @author Alexey Loubyansky
 */
public interface ProvisioningPlugin {

    default void preInstall(ProvisioningContext ctx) throws ProvisioningException {};

    default void postInstall(ProvisioningContext ctx) throws ProvisioningException {};

    default void preRemove(ProvisioningContext ctx) throws ProvisioningException {};

    default void postRemove(ProvisioningContext ctx) throws ProvisioningException {};
}
