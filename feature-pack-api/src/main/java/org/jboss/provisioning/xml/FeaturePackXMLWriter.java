/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.FeaturePackXMLParser10.Attribute;
import org.jboss.provisioning.xml.FeaturePackXMLParser10.Element;
import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXMLStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackXMLWriter {

    public static final FeaturePackXMLWriter INSTANCE = new FeaturePackXMLWriter();

    private FeaturePackXMLWriter() {
    }

    public void write(FeaturePackDescription fpDescr, Path outputFile) throws XMLStreamException, IOException {
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)) {
            write(fpDescr, writer);
        }
    }

    public void write(FeaturePackDescription fpDescr, Writer writer) throws XMLStreamException {
        final ElementNode fp = newElement(null, Element.FEATURE_PACK);
        final GAV fpGav = fpDescr.getGAV();
        addGAV(fp, fpGav);

        if (fpDescr.hasDependencies()) {
            final ElementNode deps = newElement(fp, Element.DEPENDENCIES);
            final GAV[] gavs = fpDescr.getDependencyGAVs().toArray(new GAV[0]);
            Arrays.sort(gavs);
            for (GAV gav : gavs) {
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
            for(GAV gav : fpDescr.getProvisioningPlugins()) {
                addGAV(newElement(plugins, Element.ARTIFACT), gav);
            }
        }

        try (FormattingXMLStreamWriter xmlWriter = new FormattingXMLStreamWriter(XMLOutputFactory.newInstance()
                .createXMLStreamWriter(writer))) {
            xmlWriter.writeStartDocument();
            fp.marshall(xmlWriter);
            xmlWriter.writeEndDocument();
        }
    }

    private void addGAV(final ElementNode fp, final GAV fpGav) {
        addAttribute(fp, Attribute.GROUP_ID, fpGav.getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, fpGav.getArtifactId());
        addAttribute(fp, Attribute.VERSION, fpGav.getVersion());
    }

    private static void write(ElementNode pkgs, PackageDescription pkg) {
        addAttribute(newElement(pkgs, Element.PACKAGE), Attribute.NAME, pkg.getName());
    }

    private static void write(ElementNode deps, FeaturePackDependencyDescription dependency) {
        final ElementNode depsElement = newElement(deps, Element.DEPENDENCY);
        final GAV gav = dependency.getGAV();
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
    }

    private static ElementNode newElement(ElementNode parent, Element e) {
        return new ElementNode(parent, e.getLocalName(), FeaturePackXMLParser10.NAMESPACE_1_0);
    }

    private static void addAttribute(ElementNode e, Attribute a, String value) {
        e.addAttribute(a.getLocalName(), new AttributeValue(value));
    }
}
