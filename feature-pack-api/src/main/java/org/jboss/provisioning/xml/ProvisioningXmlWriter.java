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

import java.util.Arrays;

import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.IncludedConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.spec.ConfigSpec;
import org.jboss.provisioning.xml.ProvisioningXmlParser10.Attribute;
import org.jboss.provisioning.xml.ProvisioningXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisioningXmlWriter extends BaseXmlWriter<ProvisioningConfig> {

    private static final ProvisioningXmlWriter INSTANCE = new ProvisioningXmlWriter();

    private static final String[] EMPTY_ARRAY = new String[0];

    public static ProvisioningXmlWriter getInstance() {
        return INSTANCE;
    }

    private ProvisioningXmlWriter() {
    }

    protected ElementNode toElement(ProvisioningConfig provisioningConfig) {

        final ElementNode pkg = addElement(null, Element.INSTALLATION);

        if (provisioningConfig.hasFeaturePacks()) {
            for(FeaturePackConfig fp : provisioningConfig.getFeaturePacks()) {
                final ElementNode fpElement = addElement(pkg, Element.FEATURE_PACK);
                writeFeaturePack(fpElement, fp);
            }
        }

        return pkg;
    }

    private void writeFeaturePack(ElementNode fp, FeaturePackConfig featurePack) {
        addAttribute(fp, Attribute.GROUP_ID, featurePack.getGav().getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, featurePack.getGav().getArtifactId());
        if (featurePack.getGav().getVersion() != null) {
            addAttribute(fp, Attribute.VERSION, featurePack.getGav().getVersion());
        }

        ElementNode defConfigsE = null;
        if(!featurePack.isInheritConfigs()) {
            defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS);
            addAttribute(defConfigsE, Attribute.INHERIT, "false");
        }
        if(featurePack.hasFullModelsExcluded()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS);
            }
            final String[] array = featurePack.getFullModelsExcluded().toArray(new String[featurePack.getFullModelsExcluded().size()]);
            Arrays.sort(array);
            for(String name : array) {
                final ElementNode excluded = addElement(defConfigsE, Element.EXCLUDE);
                addAttribute(excluded, Attribute.MODEL, name);
            }
        }
        if(featurePack.hasFullModelsIncluded()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS);
            }
            final String[] array = featurePack.getFullModelsIncluded().toArray(new String[featurePack.getFullModelsIncluded().size()]);
            Arrays.sort(array);
            for(String name : array) {
                final ElementNode included = addElement(defConfigsE, Element.INCLUDE);
                addAttribute(included, Attribute.MODEL, name);
            }
        }
        if(featurePack.hasExcludedConfigs()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS);
            }
            String[] models = featurePack.getExcludedModels().toArray(EMPTY_ARRAY);
            Arrays.sort(models);
            for(String modelName : models) {
                String[] configs = featurePack.getExcludedConfigs(modelName).toArray(EMPTY_ARRAY);
                Arrays.sort(configs);
                for(String configName : configs) {
                    final ElementNode excluded = addElement(defConfigsE, Element.EXCLUDE);
                    if(modelName != null) {
                        addAttribute(excluded, Attribute.MODEL, modelName);
                    }
                    addAttribute(excluded, Attribute.NAME, configName);
                }
            }
        }
        if(featurePack.hasIncludedConfigs()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS);
            }
            for (IncludedConfig config : featurePack.getIncludedConfigs()) {
                final ElementNode includeElement = addElement(defConfigsE, Element.INCLUDE);
                if(config.getModel() != null) {
                    addAttribute(includeElement, Attribute.MODEL, config.getModel());
                }
                FeatureGroupXmlWriter.addFeatureGroupDepBody(config, Element.INCLUDE.getNamespace(), includeElement);
            }
        }

        if(featurePack.hasDefinedConfigs()) {
            String[] models = featurePack.getDefinedConfigModels().toArray(EMPTY_ARRAY);
            Arrays.sort(models);
            for(String model : models) {
                for(ConfigSpec config : featurePack.getDefinedConfigs(model)) {
                    fp.addChild(ConfigXmlWriter.getInstance().toElement(config, ProvisioningXmlParser10.NAMESPACE_1_0));
                }
            }
        }

        ElementNode packages = null;
        if (!featurePack.isInheritPackages()) {
            packages = addElement(fp, Element.PACKAGES);
            addAttribute(packages, Attribute.INHERIT, "false");
        }
        if (featurePack.hasExcludedPackages()) {
            if (packages == null) {
                packages = addElement(fp, Element.PACKAGES);
            }
            for (String excluded : featurePack.getExcludedPackages()) {
                final ElementNode exclude = addElement(packages, Element.EXCLUDE);
                addAttribute(exclude, Attribute.NAME, excluded);
            }
        }
        if (featurePack.hasIncludedPackages()) {
            if (packages == null) {
                packages = addElement(fp, Element.PACKAGES);
            }
            for (PackageConfig included : featurePack.getIncludedPackages()) {
                final ElementNode include = addElement(packages, Element.INCLUDE);
                addAttribute(include, Attribute.NAME, included.getName());
                if(included.hasParams()) {
                    PackageParametersXml.write(include, included.getParameters());
                }
            }
        }
    }
}
