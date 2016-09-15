/*
 * Copyright 2014 Red Hat, Inc.
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

package org.jboss.provisioning.plugin.wildfly.featurepack.model;


import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.plugin.wildfly.BuildPropertyReplacer;
import org.jboss.provisioning.util.ParsingUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Eduardo Martins
 * @author Alexey Loubyansky
 */
public class ConfigModelParser20 {

    public static final String ELEMENT_LOCAL_NAME = "config";

    enum Element {

        // default unknown element
        UNKNOWN(null),
        STANDALONE("standalone"),
        DOMAIN("domain"),
        PROPERTY("property"),
        HOST("host"),
        ;

        private static final Map<String, Element> elements;

        static {
            Map<String, Element> elementsMap = new HashMap<>();
            elementsMap.put(Element.STANDALONE.getLocalName(), Element.STANDALONE);
            elementsMap.put(Element.DOMAIN.getLocalName(), Element.DOMAIN);
            elementsMap.put(Element.PROPERTY.getLocalName(), Element.PROPERTY);
            elementsMap.put(Element.HOST.getLocalName(), Element.HOST);
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

    enum Attribute {

        // default unknown attribute
        UNKNOWN(null),
        TEMPLATE("template"),
        SUBSYSTEMS("subsystems"),
        OUTPUT_FILE("output-file"),
        NAME("name"),
        VALUE("value"),
        ;

        private static final Map<String, Attribute> attributes;

        static {
            Map<String, Attribute> attributesMap = new HashMap<>();
            attributesMap.put(TEMPLATE.getLocalName(), TEMPLATE);
            attributesMap.put(SUBSYSTEMS.getLocalName(), SUBSYSTEMS);
            attributesMap.put(OUTPUT_FILE.getLocalName(), OUTPUT_FILE);
            attributesMap.put(NAME.getLocalName(), NAME);
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
        public String getLocalName() {
            return name;
        }
    }

    private final BuildPropertyReplacer propertyReplacer;

    public ConfigModelParser20(BuildPropertyReplacer propertyReplacer) {
        this.propertyReplacer = propertyReplacer;
    }

    public ConfigDescription parseConfig(final XMLStreamReader reader) throws XMLStreamException {
        final ConfigDescription.Builder builder = ConfigDescription.builder();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return builder.build();
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case STANDALONE:
                            builder.addStandalone(parseConfigFile(reader));
                            break;
                        case DOMAIN:
                            builder.addDomain(parseConfigFile(reader));
                            break;
                        case HOST:
                            builder.addHost(parseConfigFile(reader));
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

    private ConfigFileDescription parseConfigFile(XMLStreamReader reader) throws XMLStreamException {
        final ConfigFileDescription.Builder configFile = ConfigFileDescription.builder();
        final Set<Attribute> required = EnumSet.of(Attribute.TEMPLATE, Attribute.SUBSYSTEMS, Attribute.OUTPUT_FILE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case TEMPLATE:
                    configFile.setTemplate(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                case SUBSYSTEMS:
                    configFile.setSubsystems(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                case OUTPUT_FILE:
                    configFile.setOutputFile(propertyReplacer.replaceProperties(reader.getAttributeValue(i)));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return configFile.build();
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PROPERTY:
                            parseProperty(reader, configFile);
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

    private void parseProperty(XMLStreamReader reader, ConfigFileDescription.Builder builder) throws XMLStreamException {
        String name = null;
        String value = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME:
                    name = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                case VALUE:
                    value = propertyReplacer.replaceProperties(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (!required.isEmpty()) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), required);
        }
        ParsingUtils.parseNoContent(reader);
        builder.addProperty(name, value);
    }

}
