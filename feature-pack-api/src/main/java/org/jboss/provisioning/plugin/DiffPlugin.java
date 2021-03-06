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

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.runtime.ProvisioningRuntime;

/**
 * Diff plug-in can be referenced from a feature-pack configuration.
 *
 * Examples of such diff-provisioning tasks could be:
 * - adjust the configuration;
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public interface DiffPlugin {

    void computeDiff(ProvisioningRuntime runtime, Path customizedInstallation, Path target) throws ProvisioningException;

    default String getParameter(ProvisioningRuntime runtime, String name, String defaultValue) {
        String value = runtime.getParameter(name);
        if (value != null && ! value.isEmpty()) {
            return runtime.getParameter(name);
        }
        return defaultValue;
    }
}
