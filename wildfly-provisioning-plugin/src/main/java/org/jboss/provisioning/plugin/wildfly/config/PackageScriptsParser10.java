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

import org.jboss.provisioning.plugin.wildfly.config.PackageScripts.Script.Builder;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.provisioning.xml.XmlNameProvider;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageScriptsParser10 implements XMLElementReader<PackageScripts.Builder> {

    public static final String NAMESPACE = "urn:wildfly:package-scripts:1.0";

    enum Attribute implements XmlNameProvider {

        NAME("name"),
        PATH("path"),
        PREFIX("prefix"),
        VALUE("value"),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(NAME.getLocalName(), NAME);
            attributesMap.put(PATH.getLocalName(), PATH);
            attributesMap.put(PREFIX.getLocalName(), PREFIX);
            attributesMap.put(VALUE.getLocalName(), VALUE);
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
        HOST("host"),
        PARAM("param"),
        SCRIPT("script"),
        SCRIPTS("scripts"),
        STANDALONE("standalone"),
        // default unknown element
        UNKNOWN(null);

        private static final Map<String, Element> elements;

        static {
            final Map<String, Element> elementsMap = new HashMap<>();
            elementsMap.put(Element.DOMAIN.getLocalName(), Element.DOMAIN);
            elementsMap.put(Element.HOST.getLocalName(), Element.HOST);
            elementsMap.put(Element.PARAM.getLocalName(), Element.PARAM);
            elementsMap.put(Element.SCRIPT.getLocalName(), Element.SCRIPT);
            elementsMap.put(Element.SCRIPTS.getLocalName(), Element.SCRIPTS);
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

    public PackageScriptsParser10() {
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, PackageScripts.Builder builder) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if (count != 0) {
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
                        case STANDALONE:
                        case DOMAIN:
                        case HOST:
                            parseScripts(reader, builder, element);
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

    private static void parseScripts(final XMLExtendedStreamReader reader, PackageScripts.Builder builder, Element parent) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch(element) {
                        case SCRIPT:
                            switch (parent) {
                                case STANDALONE:
                                    builder.addStandalone(parseScript(reader));
                                    break;
                                case DOMAIN:
                                    builder.addDomain(parseScript(reader));
                                    break;
                                case HOST:
                                    builder.addHost(parseScript(reader));
                                    break;
                                default:
                                    throw ParsingUtils.unexpectedContent(reader);
                            }
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private static PackageScripts.Script parseScript(XMLStreamReader reader) throws XMLStreamException {
        String path = null;
        String prefix = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case PATH:
                    path = reader.getAttributeValue(i);
                    break;
                case PREFIX:
                    prefix = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }

        final PackageScripts.Script.Builder scriptBuilder = PackageScripts.Script.builder(path, prefix);
        final StringBuilder text = path == null ? new StringBuilder() : null;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT:
                    if (text != null) {
                        final String line = text.toString().trim();
                        if(line.isEmpty()) {
                            throw new XMLStreamException("Neither path nor content specified");
                        }
                        scriptBuilder.setLine(line);
                    }
                    return scriptBuilder.build();
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch(element) {
                        case PARAM:
                            if(path == null) {
                                throw new XMLStreamException("path attribute is missing");
                            }
                            parseScriptParam(scriptBuilder, reader);
                            break;
                        default:
                            throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if(text != null) {
                        text.append(reader.getText());
                    }
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private static void parseScriptParam(Builder scriptBuilder, XMLStreamReader reader) throws XMLStreamException {
        String name = null;
        String value = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case VALUE:
                    value = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        if (value == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.VALUE));
        }
        scriptBuilder.addParameter(name, value);
        ParsingUtils.parseNoContent(reader);
    }
}
