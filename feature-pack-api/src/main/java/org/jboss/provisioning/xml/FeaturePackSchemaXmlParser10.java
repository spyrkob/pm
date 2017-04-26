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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.schema.ConfigRef;
import org.jboss.provisioning.config.schema.XmlFeatureOccurence;
import org.jboss.provisioning.config.schema.FeatureParameter;
import org.jboss.provisioning.config.schema.ConfigSchema;
import org.jboss.provisioning.config.schema.XmlFeatureSpec;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
class FeaturePackSchemaXmlParser10 implements PlugableXmlParser<ConfigSchema.Builder> {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-config-schema:1.0";
    public static final QName ROOT_1_0 = new QName(NAMESPACE_1_0, Element.CONFIG_SCHEMA.getLocalName());

    enum Element implements XmlNameProvider {

        CONFIG_SCHEMA("config-schema"),
        FEATURE("feature"),
        FEATURES("features"),
        FEATURE_SPEC("feature-spec"),
        PARAMETERS("parameters"),
        PARAMETER("parameter"),
        PATH("path"),
        REFERENCES("references"),
        REFERENCE("reference"),

        // default unknown element
        UNKNOWN(null);


        private static final Map<QName, Element> elements;

        static {
            elements = Arrays.stream(values()).filter(val -> val.name != null).collect(Collectors.toMap(val -> new QName(NAMESPACE_1_0, val.getLocalName()), val -> val));
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

        @Override
        public String toString() {
            return name;
        }
    }

    enum Attribute implements XmlNameProvider {

        DEFAULT("default"),
        FEATURE("feature"),
        FEATURE_ID("feature-id"),
        NAME("name"),
        NILLABLE("nillable"),
        NS("ns"),
        OPTIONAL("optional"),
        PARAMETER("parameter"),
        REQUIRED("required"),
        SPEC("spec"),
        UNBOUNDED("unbounded"),

        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            attributes = Arrays.stream(values()).filter(val -> val.name != null).collect(Collectors.toMap(val -> new QName(val.getLocalName()), val -> val));
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

        @Override
        public String toString() {
            return name;
        }

    }

    @Override
    public QName getRoot() {
        return ROOT_1_0;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ConfigSchema.Builder schemaBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);

        boolean parsedFeatures = false;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE_SPEC:
                            parseFeatureSpec(reader, schemaBuilder);
                            break;
                        case FEATURES:
                            if(parsedFeatures) {
                                throw new XMLStreamException(Element.FEATURES + " may appear only once");
                            }
                            parsedFeatures = true;
                            parseFeatures(reader, schemaBuilder);
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

    private void parseFeatures(XMLExtendedStreamReader reader, ConfigSchema.Builder schemaBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE:
                            schemaBuilder.addFeature(parseFeature(reader));
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

    private void parseFeatureSpec(XMLExtendedStreamReader reader, ConfigSchema.Builder schemaBuilder) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String specName = null;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    specName = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (specName == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.SPEC));
        }

        final XmlFeatureSpec xmlSpec = new XmlFeatureSpec(specName);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    try {
                        schemaBuilder.addSpec(xmlSpec);
                    } catch (ProvisioningDescriptionException e) {
                        throw new XMLStreamException("Failed to add " + xmlSpec.getName() + " spec to the schema", e);
                    }
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case REFERENCES:
                            parseReferences(reader, xmlSpec);
                            break;
                        case PARAMETERS:
                            parseParameters(reader, xmlSpec);
                            break;
                        case FEATURES:
                            parseFeatures(reader, xmlSpec);
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

    private void parseFeatures(XMLExtendedStreamReader reader, XmlFeatureSpec specBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE:
                            specBuilder.addFeature(parseFeature(reader));
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

    private void parseReferences(XMLExtendedStreamReader reader, XmlFeatureSpec specBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case REFERENCE:
                            parseReference(reader, specBuilder);
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

    private void parseReference(XMLExtendedStreamReader reader, XmlFeatureSpec specBuilder) throws XMLStreamException {
        String feature = null;
        boolean nillable = false;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE:
                    feature = reader.getAttributeValue(i);
                    break;
                case NILLABLE:
                    nillable = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if(feature == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.FEATURE));
        }
        List<String> pathParams = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    specBuilder.addReference(ConfigRef.create(feature, nillable, pathParams == null ? null : pathParams.toArray(new String[pathParams.size()])));
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PATH:
                            if(pathParams == null) {
                                pathParams = Collections.singletonList(parsePathParameter(reader));
                            } else {
                                if(pathParams.size() == 1) {
                                    pathParams = new ArrayList<>(pathParams);
                                }
                                pathParams.add(parsePathParameter(reader));
                            }
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

    private String parsePathParameter(XMLExtendedStreamReader reader) throws XMLStreamException {
        String param = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case PARAMETER:
                    param = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if(param == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.PARAMETER));
        }
        ParsingUtils.parseNoContent(reader);
        return param;
    }

    private void parseParameters(XMLExtendedStreamReader reader, XmlFeatureSpec specBuilder) throws XMLStreamException {
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
                            parseParameter(reader, specBuilder);
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

    private void parseParameter(XMLExtendedStreamReader reader, XmlFeatureSpec specBuilder) throws XMLStreamException {
        String name = null;
        boolean featureId = false;
        String defaultValue = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case FEATURE_ID:
                    featureId = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                case DEFAULT:
                    defaultValue = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if(name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        try {
            specBuilder.addParameter(FeatureParameter.create(name, featureId, defaultValue));
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to add feature parameter", e);
        }
        ParsingUtils.parseNoContent(reader);
    }

    private XmlFeatureOccurence parseFeature(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String specName = null;
        boolean required = true;
        boolean unbounded = false;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case SPEC:
                    specName = reader.getAttributeValue(i);
                    break;
                case REQUIRED:
                    required = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                case UNBOUNDED:
                    unbounded = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if(specName == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.SPEC));
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return XmlFeatureOccurence.create(name, specName, required, unbounded);
                }
                default: {
                    throw ParsingUtils.unexpectedContent(reader);
                }
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }
}
