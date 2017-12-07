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

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.spec.FeaturePackDependencySpec;
import org.jboss.provisioning.spec.FeaturePackSpec;
import org.jboss.provisioning.xml.FeaturePackXmlParser10.Attribute;
import org.jboss.provisioning.xml.FeaturePackXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.TextNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackXmlWriter extends BaseXmlWriter<FeaturePackSpec> {

    private static final FeaturePackXmlWriter INSTANCE = new FeaturePackXmlWriter();

    public static FeaturePackXmlWriter getInstance() {
        return INSTANCE;
    }

    private FeaturePackXmlWriter() {
    }

    protected ElementNode toElement(FeaturePackSpec fpSpec) {
        final ElementNode fp = addElement(null, Element.FEATURE_PACK);
        final ArtifactCoords.Gav fpGav = fpSpec.getGav();
        addGAV(fp, fpGav);

        if (fpSpec.hasDependencies()) {
            final ElementNode deps = addElement(fp, Element.DEPENDENCIES);
            for (FeaturePackDependencySpec dep : fpSpec.getDependencies()) {
                write(deps, dep);
            }
        }

        if(fpSpec.hasConfigs()) {
            for(ConfigModel config : fpSpec.getConfigs()) {
                fp.addChild(ConfigXmlWriter.getInstance().toElement(config, FeaturePackXmlParser10.NAMESPACE_1_0));
            }
        }

        if (fpSpec.hasDefaultPackages()) {
            final ElementNode pkgs = addElement(fp, Element.DEFAULT_PACKAGES);
            final String[] pkgNames = fpSpec.getDefaultPackageNames().toArray(new String[0]);
            Arrays.sort(pkgNames);
            for (String name : pkgNames) {
                writeDefaultPackage(pkgs, name);
            }
        }

        return fp;
    }

    private void addGAV(final ElementNode fp, final ArtifactCoords.Gav fpGav) {
        addAttribute(fp, Attribute.GROUP_ID, fpGav.getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, fpGav.getArtifactId());
        addAttribute(fp, Attribute.VERSION, fpGav.getVersion());
    }

    private static void writeDefaultPackage(ElementNode pkgs, String name) {
        addAttribute(addElement(pkgs, Element.PACKAGE), Attribute.NAME, name);
    }

    private static void write(ElementNode deps, FeaturePackDependencySpec dependency) {
        final ElementNode depElement = addElement(deps, Element.DEPENDENCY);
        if(dependency.getName() != null) {
            addElement(depElement, Element.NAME).addChild(new TextNode(dependency.getName()));
        }
        ProvisioningXmlWriter.writeFeaturePackConfig(depElement, depElement.getNamespace(), dependency.getTarget());
    }
}
