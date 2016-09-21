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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.ProvisionedFeaturePackDescription;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
class ProvisioningXmlParser10 implements XMLElementReader<ProvisionedInstallationDescription.Builder> {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-provisioning:1.0";

    enum Element implements LocalNameProvider {

        FEATURE_PACK("feature-pack"),
        INSTALLATION("installation"),
        UNIVERSE("universe"),

        // default unknown element
        UNKNOWN(null);


        private static final Map<QName, Element> elements;

        static {
            final Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            elementsMap.put(new QName(NAMESPACE_1_0, Element.FEATURE_PACK.getLocalName()), Element.FEATURE_PACK);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.INSTALLATION.getLocalName()), Element.INSTALLATION);
            elementsMap.put(new QName(NAMESPACE_1_0, Element.UNIVERSE.getLocalName()), Element.UNIVERSE);
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
        @Override
        public String getLocalName() {
            return name;
        }
    }

    enum Attribute implements LocalNameProvider {

        NAME("name"),
        VERSION("version"),

        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
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
        @Override
        public String getLocalName() {
            return name;
        }
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ProvisionedInstallationDescription.Builder builder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        boolean hasUniverse = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (!hasUniverse) {
                        throw ParsingUtils.expectedAtLeastOneChild(Element.INSTALLATION, Element.UNIVERSE);
                    }
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case UNIVERSE:
                            readUniverse(reader, builder);
                            hasUniverse = true;
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

    private void readUniverse(XMLExtendedStreamReader reader, ProvisionedInstallationDescription.Builder builder) throws XMLStreamException {

        boolean emptyUniverse = true;
        final String group = readName(reader, false);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (emptyUniverse) {
                        throw ParsingUtils.expectedAtLeastOneChild(Element.UNIVERSE, Element.FEATURE_PACK);
                    }
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE_PACK:
                            emptyUniverse = false;
                            builder.addFeaturePack(readFeaturePack(reader, group));
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

    private ProvisionedFeaturePackDescription readFeaturePack(XMLExtendedStreamReader reader, String group) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        String version = "LATEST";
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case VERSION:
                    version = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);

        final ProvisionedFeaturePackDescription.Builder fpBuilder = ProvisionedFeaturePackDescription.builder();
        fpBuilder.setGAV(new GAV(group, name, version));
        return fpBuilder.build();
    }

    private String readName(final XMLExtendedStreamReader reader, boolean exclusive) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String path = null;
        boolean hasName = false;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    path = reader.getAttributeValue(i);
                    hasName = true;
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!hasName) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        if(exclusive) {
            ParsingUtils.parseNoContent(reader);
        }
        return path;
    }
}
