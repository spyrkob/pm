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
import java.util.Map;

import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.PackageConfig;
import org.jboss.provisioning.config.ProvisioningConfig;
import org.jboss.provisioning.config.ConfigModel;
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
    private static final String FALSE = "false";

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
                writeFeaturePackConfig(fpElement, fpElement.getNamespace(), fp);
            }
        }

        return pkg;
    }

    public static void writeFeaturePackConfig(ElementNode fp, String ns, FeaturePackConfig featurePack) {
        addAttribute(fp, Attribute.GROUP_ID, featurePack.getGav().getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, featurePack.getGav().getArtifactId());
        if (featurePack.getGav().getVersion() != null) {
            addAttribute(fp, Attribute.VERSION, featurePack.getGav().getVersion());
        }

        ElementNode defConfigsE = null;
        if(!featurePack.isInheritConfigs()) {
            defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            addAttribute(defConfigsE, Attribute.INHERIT, FALSE);
        }
        if(!featurePack.isInheritModelOnlyConfigs()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            }
            addAttribute(defConfigsE, Attribute.INHERIT_UNNAMED_MODELS, FALSE);
        }
        if(featurePack.hasFullModelsExcluded()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            }
            for (Map.Entry<String, Boolean> excluded : featurePack.getFullModelsExcluded().entrySet()) {
                final ElementNode exclude = addElement(defConfigsE, Element.EXCLUDE.getLocalName(), ns);
                addAttribute(exclude, Attribute.MODEL, excluded.getKey());
                if(!excluded.getValue()) {
                    addAttribute(exclude, Attribute.NAMED_MODELS_ONLY, FALSE);
                }
            }
        }
        if(featurePack.hasFullModelsIncluded()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            }
            final String[] array = featurePack.getFullModelsIncluded().toArray(new String[featurePack.getFullModelsIncluded().size()]);
            Arrays.sort(array);
            for(String name : array) {
                final ElementNode included = addElement(defConfigsE, Element.INCLUDE.getLocalName(), ns);
                addAttribute(included, Attribute.MODEL, name);
            }
        }
        if(featurePack.hasExcludedConfigs()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            }
            String[] models = featurePack.getExcludedModels().toArray(EMPTY_ARRAY);
            Arrays.sort(models);
            for(String modelName : models) {
                String[] configs = featurePack.getExcludedConfigs(modelName).toArray(EMPTY_ARRAY);
                Arrays.sort(configs);
                for(String configName : configs) {
                    final ElementNode excluded = addElement(defConfigsE, Element.EXCLUDE.getLocalName(), ns);
                    if(modelName != null) {
                        addAttribute(excluded, Attribute.MODEL, modelName);
                    }
                    addAttribute(excluded, Attribute.NAME, configName);
                }
            }
        }
        if(featurePack.hasIncludedConfigs()) {
            if(defConfigsE == null) {
                defConfigsE = addElement(fp, Element.DEFAULT_CONFIGS.getLocalName(), ns);
            }
            for (ConfigModel config : featurePack.getIncludedConfigs()) {
                final ElementNode includeElement = addElement(defConfigsE, Element.INCLUDE.getLocalName(), ns);
                if(config.getModel() != null) {
                    addAttribute(includeElement, Attribute.MODEL, config.getModel());
                }
                FeatureGroupXmlWriter.addFeatureGroupDepBody(config, includeElement, ns);
            }
        }

        if(featurePack.hasDefinedConfigs()) {
            for (ConfigModel config : featurePack.getDefinedConfigs()) {
                fp.addChild(ConfigXmlWriter.getInstance().toElement(config, ns));
            }
        }

        ElementNode packages = null;
        if (!featurePack.isInheritPackages()) {
            packages = addElement(fp, Element.PACKAGES.getLocalName(), ns);
            addAttribute(packages, Attribute.INHERIT, FALSE);
        }
        if (featurePack.hasExcludedPackages()) {
            if (packages == null) {
                packages = addElement(fp, Element.PACKAGES.getLocalName(), ns);
            }
            for (String excluded : featurePack.getExcludedPackages()) {
                final ElementNode exclude = addElement(packages, Element.EXCLUDE.getLocalName(), ns);
                addAttribute(exclude, Attribute.NAME, excluded);
            }
        }
        if (featurePack.hasIncludedPackages()) {
            if (packages == null) {
                packages = addElement(fp, Element.PACKAGES.getLocalName(), ns);
            }
            for (PackageConfig included : featurePack.getIncludedPackages()) {
                final ElementNode include = addElement(packages, Element.INCLUDE.getLocalName(), ns);
                addAttribute(include, Attribute.NAME, included.getName());
            }
        }
    }
}
