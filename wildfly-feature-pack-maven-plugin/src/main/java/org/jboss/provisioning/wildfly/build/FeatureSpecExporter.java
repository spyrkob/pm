/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.wildfly.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.xml.FeatureSpecXmlWriter;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class FeatureSpecExporter {

    public static void export(ModelNode node, Path directory) throws IOException, ProvisioningDescriptionException, XMLStreamException {
            List<FeatureSpec> specs = new ArrayList<>();
            ModelNode rootNode = node.require("result").require("feature");
            List<Property> rootFeatures;
            if(rootNode.hasDefined("name")) {
                rootFeatures = Collections.singletonList(new Property(rootNode.require("name").asString(), rootNode));
            } else {
                rootFeatures = rootNode.get("children").asPropertyList();
            }
            for (Property childFeature : rootFeatures) {
                toFeatureSpec(childFeature, specs);
            }
            if(Files.notExists(directory)) {
                Files.createDirectory(directory);
            }
            for (FeatureSpec spec : specs) {
            Path specDir = directory.resolve(spec.getName());
            if (Files.notExists(specDir)) {
                Files.createDirectory(specDir);
            }
            FeatureSpecXmlWriter.getInstance().write(spec, specDir.resolve("spec.xml"));
        }
    }

    private static void toFeatureSpec(Property featureProperty, List<FeatureSpec> specs) throws ProvisioningDescriptionException {
        ModelNode feature = featureProperty.getValue();
        FeatureSpec.Builder builder = FeatureSpec.builder(featureProperty.getName());
        FeatureAnnotation annotation = toFeatureAnnotation(feature);
        if (annotation != null) {
            builder.addAnnotation(annotation);
        }
        if (feature.hasDefined("requires")) {
            for (ModelNode capability : feature.require("requires").asList()) {
                boolean optional = capability.hasDefined("optional") &&  capability.get("optional").asBoolean();
                builder.requiresCapability(capability.get("name").asString(), optional);
            }
        }
        if (feature.hasDefined("provides")) {
            for (ModelNode capability : feature.require("provides").asList()) {
                builder.providesCapability(capability.asString());
            }
        }
        if (feature.hasDefined("params")) {
            for (ModelNode param : feature.require("params").asList()) {
                FeatureParameterSpec.Builder featureParamSpecBuilder = FeatureParameterSpec.builder(param.get("name").asString());
                if(param.hasDefined("feature-id") && param.get("feature-id").asBoolean()) {
                    featureParamSpecBuilder.setFeatureId();
                }
                if( param.hasDefined("nillable") && param.get("nillable").asBoolean()) {
                    featureParamSpecBuilder.setNillable();
                }
                featureParamSpecBuilder.setDefaultValue(param.hasDefined("default") ? convertToCli(param.get("default").asString()) : null);
                if( param.hasDefined("type")) {
                    featureParamSpecBuilder.setType(param.get("type").asString());
                }
                builder.addParam(featureParamSpecBuilder.build());
            }
        }
        if(feature.hasDefined("refs")) {
            for(ModelNode ref : feature.get("refs").asList()) {
                boolean isInclude = ref.hasDefined("include") && ref.get("include").asBoolean();
                FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder(ref.get("feature").asString()).setInclude(isInclude);
                if(ref.hasDefined("mappings")) {
                    for(Property mapping : ref.require("mappings").asPropertyList()) {
                        refBuilder.mapParam(mapping.getName(), mapping.getValue().asString());
                    }
                }
                builder.addFeatureRef(refBuilder.build());
            }
        }
        if(feature.hasDefined("packages")) {
            for(ModelNode packageDep : feature.get("packages").asList()) {
                if(packageDep.hasDefined("package")){
                    builder.addPackageDep(packageDep.require("package").asString());
                }
            }
        }
        specs.add(builder.build());
        if (feature.hasDefined("children")) {
            for (Property childFeature : feature.get("children").asPropertyList()) {
                toFeatureSpec(childFeature, specs);
            }
        }
    }

    private static String convertToCli(String value) {
        if(value!= null
                && !value.isEmpty()
                && value.indexOf(',') >=0
                && !(value.startsWith("\"") && value.endsWith("\""))
                && !(value.startsWith("[") && value.endsWith("]"))) {
            return '\"' + value +'\"';
        }
        return value;
    }

    private static FeatureAnnotation toFeatureAnnotation(ModelNode feature) {
        if (feature.hasDefined("annotation")) {
            ModelNode annotationNode = feature.require("annotation");
            FeatureAnnotation annotation = new FeatureAnnotation(annotationNode.require("name").asString());
            for (Property property : annotationNode.asPropertyList()) {
                annotation.setAttr(property.getName(), property.getValue().asString());
            }
            return annotation;
        }
        return null;
    }
}
