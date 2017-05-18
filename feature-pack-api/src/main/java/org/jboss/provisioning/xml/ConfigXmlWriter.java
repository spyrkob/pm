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
package org.jboss.provisioning.xml;

import java.util.Map;

import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.ConfigDependency;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureId;
import org.jboss.provisioning.xml.ConfigXml.Attribute;
import org.jboss.provisioning.xml.ConfigXml.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigXmlWriter extends BaseXmlWriter<Config> {

    private static final String FALSE = "false";

    private static final ConfigXmlWriter INSTANCE = new ConfigXmlWriter();

    public static ConfigXmlWriter getInstance() {
        return INSTANCE;
    }

    private ConfigXmlWriter() {
    }

    protected ElementNode toElement(Config config) {
        final ElementNode configE = addElement(null, Element.CONFIG);
        addAttribute(configE, Attribute.NAME, config.getName());

        if(config.hasDependencies()) {
            final ElementNode depsE = addElement(configE, Element.DEPENDENCIES);
            for(ConfigDependency dep : config.getDependencies()) {
                writeConfigDependency(depsE, dep);
            }
        }

        if(config.hasFeatures()) {
            final ElementNode featuresE = addElement(configE, Element.FEATURES);
            for(FeatureConfig fc : config.getFeatures()) {
                writeFeatureConfig(featuresE, fc);
            }
        }

        return configE;
    }

    private static void writeConfigDependency(ElementNode depsE, ConfigDependency dep) {
        final ElementNode depE = addElement(depsE, Element.DEPENDENCY);
        if(dep.getConfigSource() != null) {
            addAttribute(depE, Attribute.SOURCE, dep.getConfigSource());
        }
        addAttribute(depE, Attribute.CONFIG, dep.getConfigName());
        if(!dep.isInheritFeatures()) {
            addAttribute(depE, Attribute.INHERIT_FEATURES, FALSE);
        }
        if(dep.hasExcludedSpecs()) {
            for(String spec : dep.getExcludedSpecs()) {
                final ElementNode excludeE = addElement(depE, Element.EXCLUDE);
                addAttribute(excludeE, Attribute.SPEC, spec);
            }
        }
        if(dep.hasExcludedFeatures()) {
            for(FeatureId featureId : dep.getExcludedFeatures()) {
                final ElementNode excludeE = addElement(depE, Element.EXCLUDE);
                addAttribute(excludeE, Attribute.FEATURE_ID, featureId.toString());
            }
        }
        if(dep.hasIncludedSpecs()) {
            for(String spec : dep.getIncludedSpecs()) {
                final ElementNode includeE = addElement(depE, Element.INCLUDE);
                addAttribute(includeE, Attribute.SPEC, spec);
            }
        }
        if(dep.hasIncludedFeatures()) {
            for(Map.Entry<FeatureId, FeatureConfig> entry : dep.getIncludedFeatures().entrySet()) {
                final ElementNode includeE = addElement(depE, Element.INCLUDE);
                addAttribute(includeE, Attribute.FEATURE_ID, entry.getKey().toString());
                final FeatureConfig featureConfig = entry.getValue();
                if(featureConfig != null) {
                    writeFeatureConfigBody(includeE, featureConfig);
                }
            }
        }
    }

    private static void writeFeatureConfigBody(ElementNode fcE, FeatureConfig fc) {
        if(fc.hasDependencies()) {
            for(FeatureId featureId : fc.getDependencies()) {
                final ElementNode depE = addElement(fcE, Element.DEPENDENCY);
                addAttribute(depE, Attribute.FEATURE_ID, featureId.toString());
            }
        }
        if(fc.hasParams()) {
            for(Map.Entry<String, String> param : fc.getParams().entrySet()) {
                final ElementNode paramE = addElement(fcE, Element.PARAMETER);
                addAttribute(paramE, Attribute.NAME, param.getKey());
                addAttribute(paramE, Attribute.VALUE, param.getValue());
            }
        }
        if(fc.hasNested()) {
            for(FeatureConfig nested : fc.getNested()) {
                writeFeatureConfig(fcE, nested);
            }
        }
    }

    private static void writeFeatureConfig(ElementNode parentE, FeatureConfig fc) {
        final ElementNode fcE = addElement(parentE, Element.FEATURE);
        writeFeatureConfigBody(fcE, fc);
    }
}
