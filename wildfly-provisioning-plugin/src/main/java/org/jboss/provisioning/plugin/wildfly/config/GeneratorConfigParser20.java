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
package org.jboss.provisioning.plugin.wildfly.config;



import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.plugin.wildfly.BuildPropertyHandler;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.provisioning.xml.XmlNameProvider;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class GeneratorConfigParser20 {

    public static final String ELEMENT_LOCAL_NAME = "config-generator";

    enum Attribute implements XmlNameProvider {

        NAME("name"),
        PROFILE("profile"),
        SERVER_CONFIG("server-config"),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(NAME.getLocalName(), NAME);
            attributesMap.put(PROFILE.getLocalName(), PROFILE);
            attributesMap.put(SERVER_CONFIG.getLocalName(), SERVER_CONFIG);
            attributes = attributesMap;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName.getLocalPart());
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

        @Override
        public String getNamespace() {
            return null;
        }
    }

    enum Element {

        DOMAIN("domain"),
        STANDALONE("standalone"),
        // default unknown element
        UNKNOWN(null);

        private static final Map<String, Element> elements;

        static {
            Map<String, Element> elementsMap = new HashMap<>();
            elementsMap.put(Element.DOMAIN.getLocalName(), Element.DOMAIN);
            elementsMap.put(Element.STANDALONE.getLocalName(), Element.STANDALONE);
            elements = elementsMap;
        }

        static Element of(QName qName) {
            final Element element = elements.get(qName.getLocalPart());
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

    private final BuildPropertyHandler propertyReplacer;

    public GeneratorConfigParser20(BuildPropertyHandler propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    public GeneratorConfig parseGeneratorConfig(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final GeneratorConfig.Builder builder = GeneratorConfig.builder();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return builder.build();
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case STANDALONE:
                            builder.setStandalone(parseStandaloneConfig(reader));
                            break;
                        case DOMAIN:
                            builder.setDomainProfile(new DomainProfileConfig(parseSingleAttribute(reader, Attribute.PROFILE)));
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

    public StandaloneConfig parseStandaloneConfig(XMLStreamReader reader) throws XMLStreamException {
        final StandaloneConfig.Builder stdBuilder = StandaloneConfig.builder();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case SERVER_CONFIG:
                    stdBuilder.setServerConfig(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        ParsingUtils.parseNoContent(reader);
        return stdBuilder.build();
    }

    private String parseSingleAttribute(XMLStreamReader reader, Attribute attr) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String value = null;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            if(attribute.equals(attr)) {
                value = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
            } else {
                throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (value == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(attr));
        }
        ParsingUtils.parseNoContent(reader);
        return value;
    }
}
