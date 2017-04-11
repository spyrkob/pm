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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigDescription {

    final String spot;
    final String parentSpot;
    final boolean required;
    final boolean maxOccursUnbounded;
    final List<FeatureConfigDescription> features;
    final Map<String, FeatureParameter> params;
    final String idParam;
    final String parentRefParam;
    final Map<String, String> refParams;

    FeatureConfigDescription(String spot, String parentSpot, List<FeatureConfigDescription> features, XmlFeatureSpec xmlSpec, XmlFeatureOccurence occurence) {
        this.spot = spot;
        this.parentSpot = parentSpot;
        this.required = occurence.required;
        this.maxOccursUnbounded = occurence.maxOccursUnbounded;
        this.features = features;

        params = xmlSpec.parameters.size() > 1 ? Collections.unmodifiableMap(xmlSpec.parameters) : xmlSpec.parameters;
        idParam = xmlSpec.idParam;
        parentRefParam = xmlSpec.parentRefParam;
        refParams = xmlSpec.refParams.size() > 1 ? Collections.unmodifiableMap(xmlSpec.refParams) : xmlSpec.refParams;
    }
}
