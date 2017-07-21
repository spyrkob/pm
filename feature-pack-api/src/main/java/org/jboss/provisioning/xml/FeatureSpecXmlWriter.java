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

import org.jboss.provisioning.feature.FeatureParameterSpec;
import org.jboss.provisioning.feature.FeatureReferenceSpec;
import org.jboss.provisioning.feature.FeatureSpec;
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

        if(featureSpec.hasNotes()) {
            final ElementNode notesE = addElement(specE, Element.NOTES);
            for(Map.Entry<String, String> entry : featureSpec.getNotes().entrySet()) {
                final ElementNode noteE = addElement(notesE, Element.NOTE);
                addAttribute(noteE, Attribute.ID, entry.getKey());
                if(entry.getValue() != null) {
                    addAttribute(noteE, Attribute.VALUE, entry.getValue());
                }
            }
        }

        if(featureSpec.hasRefs()) {
            final ElementNode refsE = addElement(specE, Element.REFERENCES);
            for(FeatureReferenceSpec ref : featureSpec.getRefs()) {
                final ElementNode refE = addElement(refsE, Element.REFERENCE);
                final String feature = ref.getFeature().toString();
                addAttribute(refE, Attribute.FEATURE, feature);
                if(!feature.equals(ref.getName())) {
                    addAttribute(refE, Attribute.NAME, ref.getName());
                }
                if(ref.isNillable()) {
                    addAttribute(refE, Attribute.NILLABLE, TRUE);
                }
                if(ref.getParamsMapped() == 1) {
                    if(!(ref.getLocalParam(0).equals(ref.getFeature()) && ref.getTargetParam(0).equals("name"))) {
                        final ElementNode paramE = addElement(refE, Element.PARAMETER);
                        addAttribute(paramE, Attribute.NAME, ref.getLocalParam(0));
                        addAttribute(paramE, Attribute.MAPS_TO, ref.getTargetParam(0));
                    }
                } else {
                    for(int i = 0; i < ref.getParamsMapped(); ++i) {
                        final ElementNode paramE = addElement(refE, Element.PARAMETER);
                        addAttribute(paramE, Attribute.NAME, ref.getLocalParam(i));
                        addAttribute(paramE, Attribute.MAPS_TO, ref.getTargetParam(i));
                    }
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

        if(featureSpec.dependsOnPackages()) {
        final ElementNode deps = addElement(specE, Element.PACKAGES);
        if(featureSpec.dependsOnLocalPackages()) {
            for(PackageDependencySpec depSpec : featureSpec.getLocalPackageDependencies().getDescriptions()) {
                writePackageDependency(deps, depSpec);
            }
        }
        if(featureSpec.dependsOnExternalPackages()) {
            for(String name : featureSpec.getPackageDependencySources()) {
                writeFeaturePackDependency(deps, featureSpec.getExternalPackageDependencies(name));
            }
        }
        }
        return specE;
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
