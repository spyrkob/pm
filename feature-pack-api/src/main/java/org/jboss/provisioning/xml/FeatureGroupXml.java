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
import org.jboss.provisioning.config.ConfigItemContainerBuilder;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroup;
import org.jboss.provisioning.config.FeatureGroupBuilderSupport;
import org.jboss.provisioning.spec.FeatureDependencySpec;
import org.jboss.provisioning.spec.FeatureId;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeatureGroupXml {

    private static final FeatureGroupXml INSTANCE = new FeatureGroupXml();

    public static FeatureGroupXml getInstance() {
        return INSTANCE;
    }

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-feature-group:1.0";

    public enum Element implements XmlNameProvider {

        DEPENDS("depends"),
        EXCLUDE("exclude"),
        FEATURE("feature"),
        FEATURE_GROUP("feature-group"),
        FEATURE_GROUP_SPEC("feature-group-spec"),
        FEATURE_PACK("feature-pack"),
        INCLUDE("include"),
        PACKAGES("packages"),
        PARAMETER("param"),
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
        FEATURE("feature"),
        FEATURE_ID("feature-id"),
        INCLUDE("include"),
        INHERIT_FEATURES("inherit-features"),
        NAME("name"),
        OPTIONAL("optional"),
        PARENT_REF("parent-ref"),
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

    public FeatureGroupXml() {
        super();
    }

    public static void readFeatureGroupSpec(XMLExtendedStreamReader reader, FeatureGroup.Builder groupBuilder) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String name = null;
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        }
        groupBuilder.setName(name);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case FEATURE_GROUP:
                            groupBuilder.addFeatureGroup(readFeatureGroupDependency(null, reader));
                            break;
                        case FEATURE_PACK:
                            readFeaturePackDependency(reader, groupBuilder);
                            break;
                        case FEATURE:
                            final FeatureConfig nested = new FeatureConfig();
                            readFeatureConfig(reader, nested);
                            groupBuilder.addFeature(nested);
                            break;
                        case PACKAGES:
                            PackageDepsSpecXmlParser.parsePackageDeps(Element.PACKAGES, reader, groupBuilder);
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

    public static void readFeaturePackDependency(XMLExtendedStreamReader reader, ConfigItemContainerBuilder<?> groupBuilder) throws XMLStreamException {
        String fpDep = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case DEPENDENCY:
                    fpDep = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (fpDep == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.DEPENDENCY));
        }

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case FEATURE_GROUP:
                            groupBuilder.addFeatureGroup(readFeatureGroupDependency(fpDep, reader));
                            break;
                        case FEATURE:
                            final FeatureConfig nested = new FeatureConfig().setFpDep(fpDep);
                            readFeatureConfig(reader, nested);
                            groupBuilder.addFeature(nested);
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

    public static FeatureGroup readFeatureGroupDependency(String fpDep, XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        Boolean inheritFeatures = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case NAME:
                    name = reader.getAttributeValue(i);
                    break;
                case INHERIT_FEATURES:
                    inheritFeatures = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (name == null && inheritFeatures != null) {
            throw new XMLStreamException(Attribute.INHERIT_FEATURES + " attribute can't be used w/o attribute " + Attribute.NAME);
        }
        final FeatureGroup.Builder depBuilder = FeatureGroup.builder(name).setFpDep(fpDep);
        if(inheritFeatures != null) {
            depBuilder.setInheritFeatures(inheritFeatures);
        }
        readFeatureGroupConfigBody(reader, depBuilder);
        try {
            return depBuilder.build();
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to parse feature group dependency", reader.getLocation(), e);
        }
    }

    public static void readFeatureGroupConfigBody(XMLExtendedStreamReader reader, FeatureGroupBuilderSupport<?, ?> builder) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case INCLUDE:
                            readInclude(reader, null, builder);
                            break;
                        case EXCLUDE:
                            readExclude(reader, null, builder);
                            break;
                        case FEATURE_PACK:
                            readFeaturePackIncludeExclude(reader, builder);
                            break;
                        case FEATURE_GROUP:
                            builder.addFeatureGroup(readFeatureGroupDependency(null, reader));
                            break;
                        case FEATURE:
                            final FeatureConfig nested = new FeatureConfig();
                            readFeatureConfig(reader, nested);
                            builder.addFeature(nested);
                            break;
                        case PROPS:
                            readProps(reader, builder);
                            break;
                        case PACKAGES:
                            PackageDepsSpecXmlParser.parsePackageDeps(Element.PACKAGES, reader, builder);
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

    private static void readProps(XMLExtendedStreamReader reader, FeatureGroupBuilderSupport<?, ?> builder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case PROP:
                            readProp(reader, builder);
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

    private static void readProp(XMLExtendedStreamReader reader, FeatureGroupBuilderSupport<?, ?> builder) throws XMLStreamException {
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
        if(name == null) {
            if(value == null) {
                final Set<Attribute> attrs = new HashSet<>();
                attrs.add(Attribute.NAME);
                attrs.add(Attribute.VALUE);
                throw ParsingUtils.missingAttributes(reader.getLocation(), attrs);
            }
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.NAME));
        } else if(value == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.VALUE));
        }
        builder.setProperty(name, value);
        ParsingUtils.parseNoContent(reader);
    }

    private static void readFeaturePackIncludeExclude(XMLExtendedStreamReader reader, FeatureGroupBuilderSupport<?, ?> builder) throws XMLStreamException {
        String dependency = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case DEPENDENCY:
                    dependency = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (dependency == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.DEPENDENCY));
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case INCLUDE:
                            readInclude(reader, dependency, builder);
                            break;
                        case EXCLUDE:
                            readExclude(reader, dependency, builder);
                            break;
                        case FEATURE_GROUP:
                            builder.addFeatureGroup(readFeatureGroupDependency(null, reader));
                            break;
                        case FEATURE:
                            final FeatureConfig nested = new FeatureConfig();
                            readFeatureConfig(reader, nested);
                            builder.addFeature(nested);
                            break;
                        case PACKAGES:
                            PackageDepsSpecXmlParser.parsePackageDeps(Element.PACKAGES, reader, builder);
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

    private static void readInclude(XMLExtendedStreamReader reader, String dependency, FeatureGroupBuilderSupport<?, ?> depBuilder) throws XMLStreamException {
        String spec = null;
        String featureIdStr = null;
        String parentRef = null;
        FeatureConfig fc = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    featureIdStr = reader.getAttributeValue(i);
                    break;
                case SPEC:
                    spec = reader.getAttributeValue(i);
                    break;
                case PARENT_REF:
                    parentRef = reader.getAttributeValue(i);
                    fc = new FeatureConfig().setParentRef(parentRef);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }

        if(spec != null) {
            if(featureIdStr != null) {
                attributesCantBeCombined(Attribute.SPEC, Attribute.FEATURE_ID, reader);
            }
            if(parentRef != null) {
                attributesCantBeCombined(Attribute.SPEC, Attribute.PARENT_REF, reader);
            }
            try {
                depBuilder.includeSpec(dependency, spec);
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
            ParsingUtils.parseNoContent(reader);
            return;
        }
        if(featureIdStr == null) {
            throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
        }
        final FeatureId featureId = parseFeatureId(featureIdStr);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    try {
                        depBuilder.includeFeature(dependency, featureId, fc);
                    } catch (ProvisioningDescriptionException e) {
                        throw new XMLStreamException("Failed to parse config", e);
                    }
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    if(fc == null) {
                        try {
                            fc = new FeatureConfig(featureId.getSpec());
                        } catch (ProvisioningDescriptionException e) {
                            throw new XMLStreamException("Failed to parse config", e);
                        }
                    }
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case DEPENDS:
                            readFeatureDependency(reader, fc);
                            break;
                        case PARAMETER:
                            readParameter(reader, fc);
                            break;
                        case FEATURE:
                            final FeatureConfig nested = new FeatureConfig();
                            readFeatureConfig(reader, nested);
                            fc.addFeature(nested);
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

    private static void readExclude(XMLExtendedStreamReader reader, String dependency, FeatureGroupBuilderSupport<?, ?> depBuilder) throws XMLStreamException {
        String spec = null;
        String featureIdStr = null;
        String parentRef = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    featureIdStr = reader.getAttributeValue(i);
                    break;
                case SPEC:
                    spec = reader.getAttributeValue(i);
                    break;
                case PARENT_REF:
                    parentRef = reader.getAttributeValue(i);
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }

        if(spec != null) {
            if(featureIdStr != null) {
                attributesCantBeCombined(Attribute.SPEC, Attribute.FEATURE_ID, reader);
            }
            if(parentRef != null) {
                attributesCantBeCombined(Attribute.SPEC, Attribute.PARENT_REF, reader);
            }
            try {
                depBuilder.excludeSpec(dependency, spec);
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
        } else if(featureIdStr != null) {
            try {
                depBuilder.excludeFeature(dependency, parseFeatureId(featureIdStr), parentRef);
            } catch (ProvisioningDescriptionException e) {
                throw new XMLStreamException("Failed to parse config", e);
            }
        } else {
            throw new XMLStreamException("Either " + Attribute.SPEC + " or " + Attribute.FEATURE_ID + " has to be present", reader.getLocation());
        }
        ParsingUtils.parseNoContent(reader);
    }

    private static void attributesCantBeCombined(Attribute a1, Attribute a2, XMLExtendedStreamReader reader) throws XMLStreamException {
        throw new XMLStreamException(a1 + " attribute and " + a1 + " cannot be used in combination", reader.getLocation());
    }

    public static void readFeatureConfig(XMLExtendedStreamReader reader, FeatureConfig config) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case SPEC:
                    try {
                        config.setSpecName(reader.getAttributeValue(i));
                    } catch (ProvisioningDescriptionException e) {
                        throw new XMLStreamException("Failed to parse config", e);
                    }
                    break;
                case PARENT_REF:
                    config.setParentRef(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (config.getSpecId() == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.SPEC));
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    final Element element = Element.of(reader.getName().getLocalPart());
                    switch (element) {
                        case DEPENDS:
                            readFeatureDependency(reader, config);
                            break;
                        case PARAMETER:
                            readParameter(reader, config);
                            break;
                        case FEATURE:
                            final FeatureConfig child = new FeatureConfig();
                            readFeatureConfig(reader, child);
                            config.addFeature(child);
                            break;
                        case FEATURE_GROUP:
                            config.addFeatureGroup(readFeatureGroupDependency(null, reader));
                            break;
                        case FEATURE_PACK:
                            readFeaturePackDependency(reader, config);
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
        String fpDep = null;
        boolean include = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            switch (attribute) {
                case FEATURE_ID:
                    id = reader.getAttributeValue(i);
                    break;
                case DEPENDENCY:
                    fpDep = reader.getAttributeValue(i);
                    break;
                case INCLUDE:
                    include = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                default:
                    throw ParsingUtils.unexpectedAttribute(reader, i);
            }
        }
        if (id == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(Attribute.FEATURE_ID));
        }
        ParsingUtils.parseNoContent(reader);
        try {
            config.addFeatureDep(FeatureDependencySpec.create(parseFeatureId(id), fpDep, include));
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException(e);
        }
    }

    private static FeatureId parseFeatureId(String id) throws XMLStreamException {
        try {
            return FeatureId.fromString(id);
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to parse feature-id", e);
        }
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
                    throw ParsingUtils.unexpectedAttribute(reader, i);
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