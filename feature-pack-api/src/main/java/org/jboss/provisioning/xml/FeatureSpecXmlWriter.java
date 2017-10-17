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
import java.util.Set;

import org.jboss.provisioning.spec.CapabilitySpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.xml.FeatureSpecXmlParser10.Attribute;
import org.jboss.provisioning.xml.FeatureSpecXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureSpecXmlWriter extends BaseXmlWriter<FeatureSpec> {

    private static final String TRUE = "true";

    private static final FeatureSpecXmlWriter INSTANCE = new FeatureSpecXmlWriter();

    public static FeatureSpecXmlWriter getInstance() {
        return INSTANCE;
    }

    private FeatureSpecXmlWriter() {
    }

    protected ElementNode toElement(FeatureSpec featureSpec) {

        final ElementNode specE = addElement(null, Element.FEATURE_SPEC);
        addAttribute(specE, Attribute.NAME, featureSpec.getName());

        if(featureSpec.hasAnnotations()) {
            for (FeatureAnnotation fa : featureSpec.getAnnotations()) {
                final ElementNode annotationE = addElement(specE, Element.ANNOTATION);
                if (fa.hasAttrs()) {
                    for (Map.Entry<String, String> entry : fa.getAttrs().entrySet()) {
                        final ElementNode elemE = addElement(annotationE, Element.ELEM);
                        addAttribute(elemE, Attribute.NAME, entry.getKey());
                        if (entry.getValue() != null) {
                            addAttribute(elemE, Attribute.VALUE, entry.getValue());
                        }
                    }
                }
            }
        }

        if(featureSpec.providesCapabilities()) {
            writeCaps(addElement(specE, Element.PROVIDES), featureSpec.getProvidedCapabilities());
        }
        if(featureSpec.requiresCapabilities()) {
            writeCaps(addElement(specE, Element.REQUIRES), featureSpec.getRequiredCapabilities());
        }

        if(featureSpec.hasFeatureDeps()) {
            final ElementNode depsE = addElement(specE, Element.DEPENDENCIES);
            for(FeatureDependencySpec dep : featureSpec.getFeatureDeps()) {
                final ElementNode depE = addElement(depsE, Element.DEPENDENCY);
                addAttribute(depE, Attribute.FEATURE_ID, dep.getFeatureId().toString());
                if(dep.getDependency() != null) {
                    addAttribute(depE, Attribute.DEPENDENCY, dep.getDependency());
                }
                if(dep.isInclude()) {
                    addAttribute(depE, Attribute.INCLUDE, TRUE);
                }
            }
        }

        if(featureSpec.hasFeatureRefs()) {
            final ElementNode refsE = addElement(specE, Element.REFERENCES);
            for(FeatureReferenceSpec ref : featureSpec.getFeatureRefs()) {
                final ElementNode refE = addElement(refsE, Element.REFERENCE);
                final String feature = ref.getFeature().toString();
                if(ref.getDependency() != null) {
                    addAttribute(refE, Attribute.DEPENDENCY, ref.getDependency());
                }
                addAttribute(refE, Attribute.FEATURE, feature);
                if(!feature.equals(ref.getName())) {
                    addAttribute(refE, Attribute.NAME, ref.getName());
                }
                if(ref.isNillable()) {
                    addAttribute(refE, Attribute.NILLABLE, TRUE);
                }
                if(ref.isInclude()) {
                    addAttribute(refE, Attribute.INCLUDE, TRUE);
                }
                for(int i = 0; i < ref.getParamsMapped(); ++i) {
                    final ElementNode paramE = addElement(refE, Element.PARAMETER);
                    addAttribute(paramE, Attribute.NAME, ref.getLocalParam(i));
                    addAttribute(paramE, Attribute.MAPS_TO, ref.getTargetParam(i));
                }
            }
        }

        if(featureSpec.hasParams()) {
            final ElementNode paramsE = addElement(specE, Element.PARAMETERS);
            for(FeatureParameterSpec paramSpec : featureSpec.getParams()) {
                final ElementNode paramE = addElement(paramsE, Element.PARAMETER);
                addAttribute(paramE, Attribute.NAME, paramSpec.getName());
                if(paramSpec.isFeatureId()) {
                    addAttribute(paramE, Attribute.FEATURE_ID, TRUE);
                } else if(paramSpec.isNillable()) {
                    addAttribute(paramE, Attribute.NILLABLE, TRUE);
                }
                if(paramSpec.hasDefaultValue()) {
                    addAttribute(paramE, Attribute.DEFAULT, paramSpec.getDefaultValue());
                }
            }
        }

        if(featureSpec.hasPackageDeps()) {
        final ElementNode deps = addElement(specE, Element.PACKAGES);
        if(featureSpec.hasLocalPackageDeps()) {
            for(PackageDependencySpec depSpec : featureSpec.getLocalPackageDeps().getDescriptions()) {
                writePackageDependency(deps, depSpec);
            }
        }
        if(featureSpec.hasExternalPackageDeps()) {
            for(String name : featureSpec.getExternalPackageSources()) {
                writeFeaturePackDependency(deps, featureSpec.getExternalPackageDeps(name));
            }
        }
        }
        return specE;
    }

    private void writeCaps(final ElementNode parent, Set<CapabilitySpec> caps) {
        for(CapabilitySpec cap : caps) {
            final ElementNode capE = addElement(parent, Element.CAPABILITY);
            addAttribute(capE, Attribute.NAME, cap.toString());
            if(cap.isOptional()) {
                addAttribute(capE, Attribute.OPTIONAL, TRUE);
            }
        }
    }

    private static void writeFeaturePackDependency(ElementNode deps, PackageDependencyGroupSpec depGroupSpec) {
        final ElementNode fpElement = addElement(deps, Element.FEATURE_PACK);
        addAttribute(fpElement, Attribute.DEPENDENCY, depGroupSpec.getGroupName());
        for(PackageDependencySpec depSpec : depGroupSpec.getDescriptions()) {
            writePackageDependency(fpElement, depSpec);
        }
    }

    private static void writePackageDependency(ElementNode deps, PackageDependencySpec depSpec) {
        final ElementNode depElement = addElement(deps, Element.PACKAGE);
        addAttribute(depElement, Attribute.NAME, depSpec.getName());
        if(depSpec.isOptional()) {
            addAttribute(depElement, Attribute.OPTIONAL, TRUE);
        }
        if(depSpec.hasParams()) {
            PackageParametersXml.write(depElement, depSpec.getParameters());
        }
    }
}
