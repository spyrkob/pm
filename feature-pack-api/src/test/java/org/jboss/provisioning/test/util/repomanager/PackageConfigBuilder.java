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

package org.jboss.provisioning.test.util.repomanager;

import org.jboss.provisioning.parameters.ParameterSet;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageConfigBuilder {

    private final PackageBuilder pkgBuilder;
    final ParameterSet.Builder configBuilder;

    PackageConfigBuilder(PackageBuilder pkgBuilder, String configName) {
        this.pkgBuilder = pkgBuilder;
        this.configBuilder = ParameterSet.builder(configName);
    }

    public PackageConfigBuilder addParameter(String name, String value) {
        configBuilder.addParameter(name, value);
        return this;
    }

    public PackageBuilder getPackage() {
        return pkgBuilder;
    }
}
