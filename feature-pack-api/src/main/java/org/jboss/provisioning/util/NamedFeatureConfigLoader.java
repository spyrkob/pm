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

import java.nio.file.Path;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.FeatureId;

/**
 *
 * @author Alexey Loubyansky
 */
public class NamedFeatureConfigLoader extends FeatureConfigLoaderBase {

    private static final String XML = ".xml";
    private static final String NAME = "name";

    public NamedFeatureConfigLoader(Path baseDir) {
        super(baseDir);
    }

    @Override
    protected Path resolvePath(FeatureId featureId) throws ProvisioningDescriptionException {
        final String name = featureId.getParam(NAME);
        if(name == null) {
            throw new ProvisioningDescriptionException("name parameter is not found in " + featureId);
        }
        return getBaseDir(featureId).resolve(name + XML);
    }

}
