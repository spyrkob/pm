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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDependencyDescription;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.GroupDescription;
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

        final ElementNode fp = newElement(null, Element.FEATURE_PACK);
        addAttribute(fp, Attribute.GROUP_ID, fpDescr.getGAV().getGroupId());
        addAttribute(fp, Attribute.ARTIFACT_ID, fpDescr.getGAV().getArtifactId());
        addAttribute(fp, Attribute.VERSION, fpDescr.getGAV().getVersion());

        if(fpDescr.hasDependencies()) {
            final ElementNode deps = newElement(fp, Element.DEPENDENCIES);
            final GAV[] gavs = fpDescr.getDependencyGAVs().toArray(new GAV[0]);
            Arrays.sort(gavs);
            for(GAV gav : gavs) {
                write(deps, fpDescr.getDependency(gav));
            }
        }

        if(fpDescr.hasTopGroups()) {
            final ElementNode pkgs = newElement(fp, Element.PACKAGES);
            final String[] groupNames = fpDescr.getTopGroupNames().toArray(new String[0]);
            Arrays.sort(groupNames);
            for (String groupName : groupNames) {
                write(pkgs, fpDescr.getGroupDescription(groupName));
            }
        }

        try (FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)))) {
            writer.writeStartDocument();
            fp.marshall(writer);
            writer.writeEndDocument();
        }
    }

    private static void write(ElementNode pkgs, GroupDescription group) {
        addAttribute(newElement(pkgs, Element.PACKAGE), Attribute.NAME, group.getName());
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
