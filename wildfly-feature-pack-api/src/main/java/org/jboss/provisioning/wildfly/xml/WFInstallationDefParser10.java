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
package org.jboss.provisioning.wildfly.xml;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.provisioning.wildfly.descr.WFFeaturePackDescription;
import org.jboss.provisioning.wildfly.descr.WFInstallationDescription;
import org.jboss.provisioning.wildfly.descr.WFModulesDescription;
import org.jboss.provisioning.wildfly.descr.WFPackageDescription;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
class WFInstallationDefParser10 implements XMLElementReader<WFInstallationDescription.Builder> {

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
        OPTIONAL("optional"),
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
            attributesMap.put(new QName(OPTIONAL.getLocalName()), OPTIONAL);
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
    public void readElement(XMLExtendedStreamReader reader, WFInstallationDescription.Builder wfBuilder) throws XMLStreamException {
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

    private void parseFeaturePacks(XMLExtendedStreamReader reader, WFInstallationDescription.Builder wfBuilder) throws XMLStreamException {
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

    private void parseFeaturePack(XMLExtendedStreamReader reader, WFInstallationDescription.Builder wfBuilder) throws XMLStreamException {
        String groupId = null;
        String artifactId = null;
        String version = null;
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.GROUP_ID);
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

        final WFFeaturePackDescription.Builder fpBuilder = WFFeaturePackDescription.builder();
        fpBuilder.setGroupId(groupId);
        fpBuilder.setArtifactId(artifactId);
        fpBuilder.setVersion(version);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    wfBuilder.addFeaturePack(fpBuilder.build());
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

    private void parsePackage(XMLExtendedStreamReader reader, WFFeaturePackDescription.Builder fpBuilder) throws XMLStreamException {
        String name = null;
        boolean optional = false;
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case OPTIONAL:
                    optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        final WFPackageDescription.Builder pkgBuilder = WFPackageDescription.builder();
        pkgBuilder.setName(name);
        pkgBuilder.setOptional(optional);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    fpBuilder.addPackage(pkgBuilder.build());
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case MODULES:
                            final WFModulesDescription.Builder modulesBuilder = WFModulesDescription.builder();
                            modulesBuilder.setRelativeDir(parseRelativePath(reader));
                            pkgBuilder.addModule(modulesBuilder.build());
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
