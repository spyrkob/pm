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
import org.jboss.provisioning.config.FeatureGroupConfigSupport;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureGroup;
import org.jboss.provisioning.spec.FeatureGroupSpec;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.spec.SpecId;
import org.jboss.provisioning.xml.FeatureGroupXml.Attribute;
import org.jboss.provisioning.xml.FeatureGroupXml.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupXmlWriter extends BaseXmlWriter<FeatureGroupSpec> {

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private static final FeatureGroupXmlWriter INSTANCE = new FeatureGroupXmlWriter();

    public static FeatureGroupXmlWriter getInstance() {
        return INSTANCE;
    }

    private FeatureGroupXmlWriter() {
    }

    protected ElementNode toElement(FeatureGroupSpec config) {
        return toElement(config, FeatureGroupXml.NAMESPACE_1_0);
    }

    protected ElementNode toElement(FeatureGroupSpec featureGroup, String ns) {
        final ElementNode configE = addElement(null, Element.FEATURE_GROUP_SPEC.getLocalName(), ns);
        if(featureGroup.getName() != null) {
            addAttribute(configE, Attribute.NAME, featureGroup.getName());
        }

        writeFeatureGroupSpecBody(configE, featureGroup, ns);

        return configE;
    }

    private static void writeFeatureGroupSpecBody(final ElementNode configE, FeatureGroup featureGroup, String ns) {
        if(featureGroup.hasExternalGroupDeps()) {
            for(Map.Entry<String, FeatureGroupSpec> entry : featureGroup.getExternalGroupDeps().entrySet()) {
                writeExternalGroupDependency(configE, entry.getKey(), entry.getValue(), ns);
            }
        }

        if(featureGroup.hasLocalGroupDeps()) {
            for(FeatureGroupConfig dep : featureGroup.getLocalGroupDeps()) {
                writeFeatureGroupDependency(configE, dep, ns);
            }
        }

        if(featureGroup.hasFeatures()) {
            for(FeatureConfig fc : featureGroup.getFeatures()) {
                addFeatureConfig(configE, fc, ns);
            }
        }
    }

    private static void writeExternalGroupDependency(ElementNode depsE, String fpDep, FeatureGroupSpec group, String ns) {
        final ElementNode depE = addElement(depsE, Element.FEATURE_PACK.getLocalName(), ns);
        addAttribute(depE, Attribute.DEPENDENCY, fpDep);
        writeFeatureGroupSpecBody(depE, group, ns);
    }

    private static void writeFeatureGroupDependency(ElementNode depsE, FeatureGroupConfig dep, String ns) {
        final ElementNode depE = addElement(depsE, Element.FEATURE_GROUP.getLocalName(), ns);
        addFeatureGroupDepBody(dep, ns, depE);
    }

    public static void addFeatureGroupDepBody(FeatureGroupConfigSupport dep, String ns, final ElementNode depE) {
        addAttribute(depE, Attribute.NAME, dep.getName());
        if(!dep.isInheritFeatures()) {
            addAttribute(depE, Attribute.INHERIT_FEATURES, FALSE);
        }
        addFeatureGroupIncludeExclude(dep, ns, depE);
        if(dep.hasExternalFeatureGroups()) {
            for(Map.Entry<String, FeatureGroupConfig> entry : dep.getExternalFeatureGroups().entrySet()) {
                final ElementNode fpE = addElement(depE, Element.FEATURE_PACK.getLocalName(), ns);
                addAttribute(fpE, Attribute.DEPENDENCY, entry.getKey());
                addFeatureGroupIncludeExclude(entry.getValue(), ns, fpE);
            }
        }
    }

    private static void addFeatureGroupIncludeExclude(FeatureGroupConfigSupport dep, String ns, final ElementNode depE) {
        if(dep.hasExcludedSpecs()) {
            for(SpecId spec : dep.getExcludedSpecs()) {
                final ElementNode excludeE = addElement(depE, Element.EXCLUDE.getLocalName(), ns);
                addAttribute(excludeE, Attribute.SPEC, spec.toString());
            }
        }
        if(dep.hasExcludedFeatures()) {
            for(FeatureId featureId : dep.getExcludedFeatures()) {
                final ElementNode excludeE = addElement(depE, Element.EXCLUDE.getLocalName(), ns);
                addAttribute(excludeE, Attribute.FEATURE_ID, featureId.toString());
            }
        }
        if(dep.hasIncludedSpecs()) {
            for(SpecId spec : dep.getIncludedSpecs()) {
                final ElementNode includeE = addElement(depE, Element.INCLUDE.getLocalName(), ns);
                addAttribute(includeE, Attribute.SPEC, spec.toString());
            }
        }
        if(dep.hasIncludedFeatures()) {
            for(Map.Entry<FeatureId, FeatureConfig> entry : dep.getIncludedFeatures().entrySet()) {
                final ElementNode includeE = addElement(depE, Element.INCLUDE.getLocalName(), ns);
                addAttribute(includeE, Attribute.FEATURE_ID, entry.getKey().toString());
                final FeatureConfig featureConfig = entry.getValue();
                if(featureConfig != null) {
                    addFeatureConfigBody(includeE, featureConfig, ns);
                }
            }
        }
    }

    private static void addFeatureConfigBody(ElementNode fcE, FeatureConfig fc, String ns) {
        if(fc.hasFeatureDeps()) {
            for(FeatureDependencySpec depSpec : fc.getFeatureDeps()) {
                final ElementNode depE = addElement(fcE, Element.DEPENDS.getLocalName(), ns);
                if(depSpec.getDependency() != null) {
                    addAttribute(depE, Attribute.DEPENDENCY, depSpec.getDependency());
                }
                addAttribute(depE, Attribute.FEATURE_ID, depSpec.getFeatureId().toString());
                if(depSpec.isInclude()) {
                    addAttribute(depE, Attribute.INCLUDE, TRUE);
                }
            }
        }
        if(fc.hasParams()) {
            for(Map.Entry<String, String> param : fc.getParams().entrySet()) {
                final ElementNode paramE = addElement(fcE, Element.PARAMETER.getLocalName(), ns);
                addAttribute(paramE, Attribute.NAME, param.getKey());
                addAttribute(paramE, Attribute.VALUE, param.getValue());
            }
        }
        writeFeatureGroupSpecBody(fcE, fc, ns);
    }

    public static void addFeatureConfig(ElementNode parentE, FeatureConfig fc, String ns) {
        final ElementNode fcE = addElement(parentE, Element.FEATURE.getLocalName(), ns);
        addAttribute(fcE, Attribute.SPEC, fc.getSpecId().toString());
        if(fc.getParentRef() != null) {
            addAttribute(fcE, Attribute.PARENT_REF, fc.getParentRef());
        }
        addFeatureConfigBody(fcE, fc, ns);
    }
}
