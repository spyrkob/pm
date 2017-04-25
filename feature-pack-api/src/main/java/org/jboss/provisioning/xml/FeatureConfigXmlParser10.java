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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.config.schema.ConfigId;
import org.jboss.provisioning.config.schema.FeatureConfig;
import org.jboss.provisioning.config.schema.FeatureConfig.Builder;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;


/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureConfigXmlParser10 implements PlugableXmlParser<FeatureConfig.Builder> {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-feature-config:1.0";
    public static final QName ROOT_1_0 = new QName(NAMESPACE_1_0, Element.FEATURE.getLocalName());

    public enum Element implements XmlNameProvider {

        DEPENDENCIES("dependencies"),
        DEPENDENCY("dependency"),
        FEATURE("feature"),
        PARAMETERS("parameters"),
        PARAMETER("parameter"),

        // default unknown element
        UNKNOWN(null);

        private static final Map<QName, Element> elements;

        static {
            elements = Arrays.stream(values()).filter(val -> val.name != null)
                    .collect(Collectors.toMap(val -> new QName(NAMESPACE_1_0, val.getLocalName()), val -> val));
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

    enum Attribute implements XmlNameProvider {

        FEATURE("feature"),
        NAME("name"),
        OPTIONAL("optional"),
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

    @Override
    public QName getRoot() {
        return ROOT_1_0;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, FeatureConfig.Builder configBuilder) throws XMLStreamException {
        configBuilder.setName(parseName(reader, false));
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCIES:
                            readDependencies(reader, configBuilder);
                            break;
                        case PARAMETERS:
                            readParameters(reader, configBuilder);
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

    private void readDependencies(XMLExtendedStreamReader reader, Builder configBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCY:
                            readDependency(reader, configBuilder);
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

    private void readDependency(XMLExtendedStreamReader reader, Builder builder) throws XMLStreamException {
        String feature = null;
        boolean optional = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE:
                    feature = reader.getAttributeValue(i);
                    break;
                case OPTIONAL:
                    optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (feature == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.FEATURE));
        }
        ParsingUtils.parseNoContent(reader);
        builder.addDependency(ConfigId.fromString(feature), optional);
    }

    private void readParameters(XMLExtendedStreamReader reader, Builder configBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PARAMETER:
                            readParameter(reader, configBuilder);
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

    private void readParameter(XMLExtendedStreamReader reader, Builder builder) throws XMLStreamException {
        String name = null;
        String value = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
            final Set<Attribute> missingAttrs;
            if(value == null) {
                missingAttrs = new HashSet<>();
                missingAttrs.add(Attribute.NAME);
                missingAttrs.add(Attribute.VALUE);
            } else {
                missingAttrs = Collections.singleton(Attribute.NAME);
            }
            throw ParsingUtils.missingAttributes(reader.getLocation(), missingAttrs);
        } else if (value == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.VALUE));
        }
        ParsingUtils.parseNoContent(reader);
        builder.addParameter(name, value);
    }

    private String parseName(final XMLExtendedStreamReader reader, boolean exclusive) throws XMLStreamException {
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
        if(exclusive) {
            ParsingUtils.parseNoContent(reader);
        }
        return path;
    }
}
