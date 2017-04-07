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

package org.jboss.provisioning.config.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.provisioning.ProvisioningException;

/**
 *
 * @author Alexey Loubyansky
 */
public class LineupUtility {

    public static List<FeatureConfig> lineup(FeatureConfig config, FeatureConfigResolver resolver) throws ProvisioningException {
        final LineupUtility utility = new LineupUtility(resolver);
        utility.lineup(config);
        return utility.line;
    }

    private final FeatureConfigResolver resolver;
    private List<FeatureConfig> line = new ArrayList<>();
    private Set<ConfigRef> linedUp = new HashSet<>();

    private LineupUtility(FeatureConfigResolver resolver) {
        this.resolver = resolver;
    }

    private void lineup(FeatureConfig config) throws ProvisioningException {
        if(config.ref != null) {
            if(linedUp.contains(config.ref)) {
                return;
            }
            linedUp.add(config.ref);
        }
        if(!config.dependencies.isEmpty()) {
            for (ConfigRef depId : config.dependencies.values()) {
                lineup(resolver.resolve(depId));
            }
        }
        line.add(config);
        for(List<FeatureConfig> specConfigs : config.subconfigs.values()) {
            for(FeatureConfig subconfig : specConfigs) {
                lineup(subconfig);
            }
        }
    }
}
