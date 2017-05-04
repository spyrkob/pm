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

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.feature.Config;
import org.jboss.provisioning.feature.ConfigDependency;
import org.jboss.provisioning.feature.FeatureConfig;
import org.jboss.provisioning.feature.FeatureId;
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
        DEPENDENCIES("dependencies"),
        DEPENDENCY("dependency"),
        EXCLUDE("exclude"),
        FEATURE("feature"),
        FEATURES("features"),
        INCLUDE("include"),
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

    protected enum Attribute implements XmlNameProvider {

        CONFIG("config"),
        FEATURE("feature"),
        FEATURE_ID("feature-id"),
        INHERIT_FEATURES("inherit-features"),
        NAME("name"),
        OPTIONAL("optional"),
        SPEC("spec"),
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

    public static void readConfig(XMLExtendedStreamReader reader, Config.Builder config) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        config.setName(name);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCIES:
                            readConfigDependencies(reader, config);
                            break;
                        case FEATURES:
                            readFeatures(reader, config);
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

    private static void readConfigDependencies(XMLExtendedStreamReader reader, Config.Builder config) throws XMLStreamException {
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
                            config.addDependency(readConfigDependency(reader));
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

    private static ConfigDependency readConfigDependency(XMLExtendedStreamReader reader) throws XMLStreamException {
        String config = null;
        Boolean inheritFeatures = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case CONFIG:
                    config = reader.getAttributeValue(i);
                    break;
                case INHERIT_FEATURES:
                    inheritFeatures = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (config == null && inheritFeatures != null) {
            throw new XMLStreamException(Attribute.INHERIT_FEATURES + " attribute can't be used w/o attribute " + Attribute.CONFIG);
        }
        final ConfigDependency.Builder depBuilder = ConfigDependency.builder(config);
        if(inheritFeatures != null) {
            depBuilder.setInheritFeatures(inheritFeatures);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return depBuilder.build();
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case INCLUDE:
                            readInclude(reader, depBuilder);
                            break;
                        case EXCLUDE:
                            readExclude(reader, depBuilder);
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

    private static void readInclude(XMLExtendedStreamReader reader, ConfigDependency.Builder depBuilder) throws XMLStreamException {
        String spec = null;
        String featureId = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    featureId = reader.getAttributeValue(i);
                    break;
                case SPEC:
                    spec = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }

        if(spec != null) {
            if(featureId != null) {
                throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
            }
            try {
                depBuilder.includeSpec(spec);
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
            ParsingUtils.parseNoContent(reader);
            return;
        }
        if(featureId == null) {
            throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
        }
        FeatureConfig fc = null;
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    try {
                        depBuilder.includeFeature(parseFeatureId(featureId), fc);
                    } catch (ProvisioningDescriptionException e) {
                        throw new XMLStreamException("Failed to parse config", e);
                    }
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    if(fc == null) {
                        fc = new FeatureConfig();
                    }
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCIES:
                            readFeatureDependencies(reader, fc);
                            break;
                        case PARAMETERS:
                            readParameters(reader, fc);
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

    private static void readExclude(XMLExtendedStreamReader reader, ConfigDependency.Builder depBuilder) throws XMLStreamException {
        String spec = null;
        String featureId = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    featureId = reader.getAttributeValue(i);
                    break;
                case SPEC:
                    spec = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }

        if(spec != null) {
            if(featureId != null) {
                throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
            }
            try {
                depBuilder.excludeSpec(spec);
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
        } else if(featureId != null) {
            try {
                depBuilder.excludeFeature(parseFeatureId(featureId));
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
        } else {
            throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
        }
        ParsingUtils.parseNoContent(reader);
    }

    private static void readFeatures(XMLExtendedStreamReader reader, Config.Builder config) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case FEATURE:
                            final FeatureConfig fc = new FeatureConfig();
                            readFeatureConfig(reader, fc);
                            config.addFeature(fc);
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

    public static void readFeatureConfig(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
        String spec = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case SPEC:
                    spec = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (spec == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.SPEC));
        }
        config.setSpecName(spec);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCIES:
                            readFeatureDependencies(reader, config);
                            break;
                        case PARAMETERS:
                            readParameters(reader, config);
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

    private static void readFeatureDependencies(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case DEPENDENCY:
                            readFeatureDependency(reader, config);
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

    private static void readFeatureDependency(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
        String id = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    id = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (id == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.FEATURE_ID));
        }
        ParsingUtils.parseNoContent(reader);
        config.addDependency(parseFeatureId(id));
    }

    private static FeatureId parseFeatureId(String id) throws XMLStreamException {
        try {
            return FeatureId.fromString(id);
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to parse feature-id", e);
        }
    }

    private static void readParameters(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PARAMETER:
                            readParameter(reader, config);
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

    private static void readParameter(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
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
        config.setParam(name, value);
    }
}