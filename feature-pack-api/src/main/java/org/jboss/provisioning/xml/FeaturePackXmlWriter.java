/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.xml.FeaturePackXmlParser10.Attribute;
import org.jboss.provisioning.xml.FeaturePackXmlParser10.Element;
import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackXmlWriter {

    public static final FeaturePackXmlWriter INSTANCE = new FeaturePackXmlWriter();

    private FeaturePackXmlWriter() {
    }

    public void write(FeaturePackDescription fpDescr, Path outputFile) throws XMLStreamException, IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)) {
            write(fpDescr, writer);
        }
    }

    public void write(FeaturePackDescription fpDescr, Writer writer) throws XMLStreamException {
        final ElementNode fp = newElement(null, Element.FEATURE_PACK);
        final Gav fpGav = fpDescr.getGav();
        addGAV(fp, fpGav);

        if (fpDescr.hasDependencies()) {
            final ElementNode deps = newElement(fp, Element.DEPENDENCIES);
            final Gav[] gavs = fpDescr.getDependencyGAVs().toArray(new Gav[0]);
            Arrays.sort(gavs);
            for (Gav gav : gavs) {
                write(deps, fpDescr.getDependency(gav));
            }
        }

        if (fpDescr.hasTopPackages()) {
            final ElementNode pkgs = newElement(fp, Element.PACKAGES);
            final String[] pkgNames = fpDescr.getTopPackageNames().toArray(new String[0]);
            Arrays.sort(pkgNames);
            for (String name : pkgNames) {
                write(pkgs, fpDescr.getPackageDescription(name));
            }
        }

        if(fpDescr.hasProvisioningPlugins()) {
            final ElementNode plugins = newElement(fp, Element.PROVISIONING_PLUGINS);
            for(Gav gav : fpDescr.getProvisioningPlugins()) {
                addGAV(newElement(plugins, Element.ARTIFACT), gav);
            }
        }

        try (FormattingXmlStreamWriter xmlWriter = new FormattingXmlStreamWriter(XMLOutputFactory.newInstance()
                .createXMLStreamWriter(writer))) {
            xmlWriter.writeStartDocument();
            fp.marshall(xmlWriter);
            xmlWriter.writeEndDocument();
        }
    }

    private void addGAV(final ElementNode fp, final Gav fpGav) {
        addAttribute(fp, Attribute.GROUP_ID, fpGav.getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, fpGav.getArtifactId());
        addAttribute(fp, Attribute.VERSION, fpGav.getVersion());
    }

    private static void write(ElementNode pkgs, PackageDescription pkg) {
        addAttribute(newElement(pkgs, Element.PACKAGE), Attribute.NAME, pkg.getName());
    }

    private static void write(ElementNode deps, ProvisionedFeaturePackDescription dependency) {
        final ElementNode depsElement = newElement(deps, Element.DEPENDENCY);
        final Gav gav = dependency.getGav();
        addAttribute(depsElement, Attribute.GROUP_ID, gav.getGroupId());
        addAttribute(depsElement, Attribute.ARTIFACT_ID, gav.getArtifactId());
        if(gav.getVersion() != null) {
            addAttribute(depsElement, Attribute.VERSION, gav.getVersion());
        }
        if(dependency.hasExcludedPackages()) {
            final ElementNode excludes = newElement(depsElement, Element.EXCLUDES);
            final String[] packageNames = dependency.getExcludedPackages().toArray(new String[0]);
            Arrays.sort(packageNames);
            for (String packageName : packageNames) {
                addAttribute(newElement(excludes, Element.PACKAGE), Attribute.NAME, packageName);
            }
        }
        if(dependency.hasIncludedPackages()) {
            final ElementNode includes = newElement(depsElement, Element.INCLUDES);
            final String[] packageNames = dependency.getIncludedPackages().toArray(new String[0]);
            Arrays.sort(packageNames);
            for (String packageName : packageNames) {
                addAttribute(newElement(includes, Element.PACKAGE), Attribute.NAME, packageName);
            }
        }
    }

    private static ElementNode newElement(ElementNode parent, Element e) {
        final ElementNode eNode = new ElementNode(parent, e.getLocalName(), FeaturePackXmlParser10.NAMESPACE_1_0);
        if(parent != null) {
            parent.addChild(eNode);
        }
        return eNode;
    }

    private static void addAttribute(ElementNode e, Attribute a, String value) {
        e.addAttribute(a.getLocalName(), new AttributeValue(value));
    }
}
