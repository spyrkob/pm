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

import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.spec.FeatureGroupSpec;
import org.jboss.provisioning.xml.ConfigXml.Attribute;
import org.jboss.provisioning.xml.ConfigXml.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigXmlWriter extends BaseXmlWriter<ConfigSpec> {

    private static final ConfigXmlWriter INSTANCE = new ConfigXmlWriter();

    public static ConfigXmlWriter getInstance() {
        return INSTANCE;
    }

    private ConfigXmlWriter() {
    }

    protected ElementNode toElement(ConfigSpec config) {
        return toElement(config, ConfigXml.NAMESPACE_1_0);
    }

    protected ElementNode toElement(ConfigSpec config, String ns) {
        final ElementNode configE = addElement(null, Element.CONFIG.getLocalName(), ns);
        if(config.getName() != null) {
            addAttribute(configE, Attribute.NAME, config.getName());
        }
        if(config.getModel() != null) {
            addAttribute(configE, Attribute.MODEL, config.getModel());
        }

        if(config.hasProperties()) {
            final ElementNode propsE = addElement(configE, Element.PROPS.getLocalName(), ns);
            for(Map.Entry<String, String> entry : config.getProperties().entrySet()) {
                final ElementNode propE = addElement(propsE, Element.PROP.getLocalName(), ns);
                addAttribute(propE, Attribute.NAME, entry.getKey());
                addAttribute(propE, Attribute.VALUE, entry.getValue());
            }
        }

        if(config.hasExternalGroupDeps()) {
            for(Map.Entry<String, FeatureGroupSpec> entry : config.getExternalGroupDeps().entrySet()) {
                final ElementNode fpE = addElement(configE, Element.FEATURE_PACK.getLocalName(), ns);
                addAttribute(fpE, Attribute.DEPENDENCY, entry.getKey());
                final FeatureGroupSpec externalDep = entry.getValue();
                if (externalDep.hasLocalGroupDeps()) {
                    for (FeatureGroupConfig fg : externalDep.getLocalGroupDeps()) {
                        FeatureGroupXmlWriter.addFeatureGroupDepBody(fg, ns, addElement(fpE, Element.FEATURE_GROUP.getLocalName(), ns));
                    }
                }
                if (externalDep.hasFeatures()) {
                    for (FeatureConfig fc : externalDep.getFeatures()) {
                        FeatureGroupXmlWriter.addFeatureConfig(fpE, fc, ns);
                    }
                }
            }
        }

        if(config.hasLocalGroupDeps()) {
            for(FeatureGroupConfig fg : config.getLocalGroupDeps()) {
                FeatureGroupXmlWriter.addFeatureGroupDepBody(fg, ns, addElement(configE, Element.FEATURE_GROUP.getLocalName(), ns));
            }
        }

        if(config.hasFeatures()) {
            for(FeatureConfig fc : config.getFeatures()) {
                FeatureGroupXmlWriter.addFeatureConfig(configE, fc, ns);
            }
        }

        return configE;
    }
}
