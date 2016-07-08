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

package org.jboss.pm.fp.xml;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackXMLParser10 {

    public static final String NAMESPACE_1_0 = "urn:wildfly:pm-feature-pack:1.0";

    public enum Element {

        DEPENDENCIES("dependencies"),
        DEPENDENCY("dependency"),
        FEATURE_PACK("feature-pack"),
        PACKAGES("packages"),
        PACKAGE("package"),

        // default unknown element
        UNKNOWN(null);

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            addElement(elementsMap, Element.DEPENDENCIES);
            addElement(elementsMap, Element.DEPENDENCY);
            addElement(elementsMap, Element.FEATURE_PACK);
            addElement(elementsMap, Element.PACKAGES);
            addElement(elementsMap, Element.PACKAGE);
            elements = elementsMap;
        }

        private static void addElement(Map<QName, Element> map, Element e) {
            map.put(new QName(NAMESPACE_1_0,  e.getLocalName()), e);
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
        public String getLocalName() {
            return name;
        }
    }

    enum Attribute {

        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        EXTENSION("extension"),
        VERSION("version"),
        NAME("name"),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            attributesMap.put(new QName(GROUP_ID.getLocalName()), GROUP_ID);
            attributesMap.put(new QName(ARTIFACT_ID.getLocalName()), ARTIFACT_ID);
            attributesMap.put(new QName(CLASSIFIER.getLocalName()), CLASSIFIER);
            attributesMap.put(new QName(EXTENSION.getLocalName()), EXTENSION);
            attributesMap.put(new QName(VERSION.getLocalName()), VERSION);
            attributesMap.put(new QName(NAME.getLocalName()), NAME);
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
        public String getLocalName() {
            return name;
        }
    }
}
