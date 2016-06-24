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

package org.jboss.pm.wildfly.xml;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.pm.GAV;
import org.jboss.pm.wildfly.def.WFFeaturePackDefBuilder;
import org.jboss.pm.wildfly.def.WFInstallationDefBuilder;
import org.jboss.pm.wildfly.def.WFModulesDefBuilder;
import org.jboss.pm.wildfly.def.WFPackageDefBuilder;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
class WFInstallationDefParser10 implements XMLElementReader<WFInstallationDefBuilder> {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-install-def:1.0";

    enum Element {

        FEATURE_PACK("feature-pack"),
        FEATURE_PACKS("feature-packs"),
        INSTALLATION("installation"),
        MODULES("modules"),
        MODULE("module"),
        PACKAGE("package"),
        PACKAGE_REF("package-ref"),
        PATH("path"),

        // default unknown element
        UNKNOWN(null);


        private static final Map<QName, Element> elements;

        static {
            final Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACK.getLocalName()), Element.FEATURE_PACK);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACKS.getLocalName()), Element.FEATURE_PACKS);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.INSTALLATION.getLocalName()), Element.INSTALLATION);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.MODULES.getLocalName()), Element.MODULES);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.MODULE.getLocalName()), Element.MODULE);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PACKAGE.getLocalName()), Element.PACKAGE);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PACKAGE_REF.getLocalName()), Element.PACKAGE_REF);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.PATH.getLocalName()), Element.PATH);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    enum Attribute {

        ARTIFACT_ID("artifact-id"),
        GROUP_ID("group-id"),
        NAME("name"),
        RELATIVE("relative"),
        VERSION("version"),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
            attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
            attributesMap.put(new QName(RELATIVE.getLocalName()), RELATIVE);
            attributesMap.put(new QName(VERSION.getLocalName()), VERSION);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }

        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, WFInstallationDefBuilder wfBuilder) throws XMLStreamException {
        if(reader.getAttributeCount() != 0) {
            throw ParsingUtils.unexpectedContent(reader);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULES:
                            wfBuilder.setModulesPath(parseRelativePath(reader));
                            break;
                        case FEATURE_PACKS:
                            parseFeaturePacks(reader, wfBuilder);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private String parseRelativePath(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String path = null;
        boolean parsedTarget = false;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case RELATIVE:
                    path = reader.getAttributeValue(i);
                    parsedTarget = true;
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!parsedTarget) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.RELATIVE));
        }
        ParsingUtils.parseNoContent(reader);
        return path;
    }

    private void parseFeaturePacks(XMLExtendedStreamReader reader, WFInstallationDefBuilder wfBuilder) throws XMLStreamException {
        if(reader.getAttributeCount() != 0) {
            throw ParsingUtils.unexpectedContent(reader);
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE_PACK:
                            parseFeaturePack(reader, wfBuilder);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
    }

    private void parseFeaturePack(XMLExtendedStreamReader reader, WFInstallationDefBuilder wfBuilder) throws XMLStreamException {
        String groupId = null;
        String artifactId = null;
        String version = null;
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.GROUP_ID, Attribute.ARTIFACT_ID, Attribute.VERSION);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case GROUP_ID:
                    groupId = reader.getAttributeValue(i);
                    break;
                case ARTIFACT_ID:
                    artifactId = reader.getAttributeValue(i);
                    break;
                case VERSION:
                    version = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }

        final WFFeaturePackDefBuilder fpBuilder = new WFFeaturePackDefBuilder(new GAV(groupId, artifactId, version), wfBuilder);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    wfBuilder.addFeaturePack(fpBuilder);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PACKAGE:
                            parsePackage(reader, fpBuilder);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
    }

    private void parsePackage(XMLExtendedStreamReader reader, WFFeaturePackDefBuilder fpBuilder) throws XMLStreamException {
        String name = null;
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        final WFPackageDefBuilder pkgBuilder = new WFPackageDefBuilder(name);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    fpBuilder.addPackage(pkgBuilder);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULES:
                            pkgBuilder.addModule(new WFModulesDefBuilder(parseRelativePath(reader)));
                            break;
                        case PATH:
                            pkgBuilder.addRelativePath(parseRelativePath(reader));
                            break;
                        case PACKAGE_REF:
                            pkgBuilder.addPackageRef(parseName(reader));
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
    }

    private String parseName(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String path = null;
        boolean parsedTarget = false;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    path = reader.getAttributeValue(i);
                    parsedTarget = true;
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!parsedTarget) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);
        return path;
    }
}
