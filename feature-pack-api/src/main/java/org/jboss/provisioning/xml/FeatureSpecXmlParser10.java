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
import org.jboss.provisioning.spec.CapabilitySpec;
import org.jboss.provisioning.spec.FeatureAnnotation;
import org.jboss.provisioning.spec.FeatureParameterSpec;
import org.jboss.provisioning.spec.FeatureReferenceSpec;
import org.jboss.provisioning.spec.FeatureSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
class FeatureSpecXmlParser10 implements PlugableXmlParser<FeatureSpec.Builder> {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-feature-spec:1.0";
    public static final QName ROOT_1_0 = new QName(NAMESPACE_1_0, Element.FEATURE_SPEC.getLocalName());

    enum Element implements XmlNameProvider {

        ANNOTATION("annotation"),
        CAPABILITY("capability"),
        ELEM("elem"),
        FEATURE_PACK("feature-pack"),
        FEATURE_SPEC("feature-spec"),
        PACKAGE("package"),
        PACKAGES("packages"),
        PARAMETERS("params"),
        PARAMETER("param"),
        PROVIDES("provides"),
        REFERENCES("refs"),
        REFERENCE("ref"),
        REQUIRES("requires"),

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
        DEPENDENCY("dependency"),
        FEATURE("feature"),
        FEATURE_ID("feature-id"),
        MAPS_TO("maps-to"),
        NAME("name"),
        NILLABLE("nillable"),
        OPTIONAL("optional"),
        VALUE("value"),
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
    public void readElement(XMLExtendedStreamReader reader, FeatureSpec.Builder featureBuilder) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String specName = null;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    specName = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (specName == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        featureBuilder.setName(specName);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ANNOTATION:
                            parseAnnotation(reader, featureBuilder);
                            break;
                        case REFERENCES:
                            parseReferences(reader, featureBuilder);
                            break;
                        case PARAMETERS:
                            parseParameters(reader, featureBuilder);
                            break;
                        case PACKAGES:
                            parsePackages(reader, featureBuilder);
                            break;
                        case PROVIDES:
                            parseCapabilities(reader, featureBuilder, true);
                            break;
                        case REQUIRES:
                            parseCapabilities(reader, featureBuilder, false);
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

    private void parseAnnotation(XMLExtendedStreamReader reader, FeatureSpec.Builder builder) throws XMLStreamException {
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if(name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        final FeatureAnnotation fa = new FeatureAnnotation(name);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    builder.addAnnotation(fa);
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case ELEM:
                            parseAnnotationElem(reader, fa);
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

    private void parseAnnotationElem(XMLExtendedStreamReader reader, FeatureAnnotation fa) throws XMLStreamException {
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
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if(name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);
        fa.setAttr(name, value);
    }

    private void parseReferences(XMLExtendedStreamReader reader, FeatureSpec.Builder specBuilder) throws XMLStreamException {
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
                            try {
                                specBuilder.addRef(parseReference(reader));
                            } catch (ProvisioningDescriptionException e) {
                                throw new XMLStreamException("Failed to parse feature reference", e);
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

    private FeatureReferenceSpec parseReference(XMLExtendedStreamReader reader) throws XMLStreamException {
        String dependency = null;
        String name = null;
        String feature = null;
        boolean nillable = false;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case DEPENDENCY:
                    dependency = reader.getAttributeValue(i);
                    break;
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case FEATURE:
                    feature = reader.getAttributeValue(i);
                    break;
                case NILLABLE:
                    nillable = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if(feature == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.FEATURE));
        }
        if(name == null) {
            name = feature;
        }
        FeatureReferenceSpec.Builder refBuilder = FeatureReferenceSpec.builder(feature).setDependency(dependency).setName(name).setNillable(nillable);

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    try {
                        return refBuilder.build();
                    } catch (ProvisioningDescriptionException e) {
                        throw new XMLStreamException("Failed to parse feature reference", e);
                    }
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PARAMETER:
                            parseRefParameter(reader, refBuilder);
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

    private void parseRefParameter(XMLExtendedStreamReader reader, FeatureReferenceSpec.Builder refBuilder) throws XMLStreamException {
        String name = null;
        String mapsTo = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case MAPS_TO:
                    mapsTo = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if(name == null) {
            final Set<Attribute> set;
            if(mapsTo == null) {
                set = new HashSet<>();
                set.add(Attribute.NAME);
                set.add(Attribute.MAPS_TO);
            } else {
                set = Collections.singleton(Attribute.NAME);
            }
            throw ParsingUtils.missingAttributes(reader.getLocation(), set);
        } else if(mapsTo == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.MAPS_TO));
        }
        refBuilder.mapParam(name, mapsTo);
        ParsingUtils.parseNoContent(reader);
    }

    private void parseParameters(XMLExtendedStreamReader reader, FeatureSpec.Builder specBuilder) throws XMLStreamException {
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
                            try {
                                specBuilder.addParam(parseParameter(reader));
                            } catch (ProvisioningDescriptionException e) {
                                throw new XMLStreamException("Failed to add parameter to the spec", reader.getLocation(), e);
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

    private FeatureParameterSpec parseParameter(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        boolean featureId = false;
        String defaultValue = null;
        boolean nillable = false;
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
                case NILLABLE:
                    nillable = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if(name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);
        try {
            return FeatureParameterSpec.create(name, featureId, nillable, defaultValue);
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to create feature parameter", reader.getLocation(), e);
        }
    }

    private void parsePackages(XMLExtendedStreamReader reader, FeatureSpec.Builder spec) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PACKAGE:
                            spec.addPackageDependency(parsePackageDependency(reader));
                            break;
                        case FEATURE_PACK:
                            parseFeaturePackDependency(reader, spec);
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

    private PackageDependencySpec parsePackageDependency(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        boolean optional = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case OPTIONAL:
                    optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);
        return PackageDependencySpec.create(name, optional);
    }

    private void parseFeaturePackDependency(XMLExtendedStreamReader reader, FeatureSpec.Builder spec) throws XMLStreamException {
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case DEPENDENCY:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.DEPENDENCY));
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case PACKAGE:
                            spec.addPackageDependency(name, parsePackageDependency(reader));
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

    private void parseCapabilities(XMLExtendedStreamReader reader, FeatureSpec.Builder spec, boolean provides) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case CAPABILITY:
                            final CapabilitySpec cap = parseCapabilityName(reader);
                                if (provides) {
                                    spec.providesCapability(cap);
                                } else {
                                    spec.requiresCapability(cap);
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

    private CapabilitySpec parseCapabilityName(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        boolean optional = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case OPTIONAL:
                    optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        ParsingUtils.parseNoContent(reader);
        try {
            return CapabilitySpec.fromString(name, optional);
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to parse capability '" + name + "'", reader.getLocation(), e);
       }
    }
}