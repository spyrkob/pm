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
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.config.ConfigModel;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
public class ConfigXml {

    private static final ConfigXml INSTANCE = new ConfigXml();

    public static ConfigXml getInstance() {
        return INSTANCE;
    }

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-config:1.0";

    public enum Element implements XmlNameProvider {

        CONFIG("config"),
        FEATURE("feature"),
        FEATURE_GROUP("feature-group"),
        FEATURE_PACK("feature-pack"),
        PACKAGES("packages"),
        PROP("prop"),
        PROPS("props"),

        // default unknown element
        UNKNOWN(null);

        private static final Map<String, Element> elementsByLocal;

        static {
            elementsByLocal = Arrays.stream(values()).filter(val -> val.name != null)
                    .collect(Collectors.toMap(val -> val.getLocalName(), val -> val));
        }

        static Element of(String localName) {
            final Element element = elementsByLocal.get(localName);
            return element == null ? UNKNOWN : element;
        }

        private final String name;
        private final String namespace = NAMESPACE_1_0;

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

        @Override
        public String getNamespace() {
            return namespace;
        }
    }

    protected enum Attribute implements XmlNameProvider {

        DEPENDENCY("dependency"),
        INHERIT_FEATURES("inherit-features"),
        NAME("name"),
        MODEL("model"),
        VALUE("value"),

        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            attributes = Arrays.stream(values()).filter(val -> val.name != null)
                    .collect(Collectors.toMap(val -> new QName(val.getLocalName()), val -> val));
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

        @Override
        public String getNamespace() {
            return null;
        }
    }

    public ConfigXml() {
        super();
    }

    public static void readConfig(XMLExtendedStreamReader reader, ConfigModel.Builder configBuilder) throws XMLStreamException {
        String name = null;
        Boolean inheritFeatures = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    configBuilder.setName(name);
                    break;
                case MODEL:
                    configBuilder.setModel(reader.getAttributeValue(i));
                    break;
                case INHERIT_FEATURES:
                    inheritFeatures = Boolean.parseBoolean(reader.getAttributeValue(i));
                    configBuilder.setInheritFeatures(inheritFeatures);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null && inheritFeatures != null) {
            throw new XMLStreamException(Attribute.INHERIT_FEATURES + " attribute can't be used w/o attribute " + Attribute.NAME);
        }
        FeatureGroupXml.readFeatureGroupConfigBody(reader, configBuilder);
    }
}