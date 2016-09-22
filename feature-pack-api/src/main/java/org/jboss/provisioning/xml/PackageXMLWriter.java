/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.PackageXMLParser10.Attribute;
import org.jboss.provisioning.xml.PackageXMLParser10.Element;
import org.jboss.provisioning.xml.util.AttributeValue;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXMLStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageXMLWriter {

    public static final PackageXMLWriter INSTANCE = new PackageXMLWriter();

    private PackageXMLWriter() {
    }

    public void write(PackageDescription pkgDescr, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = newElement(null, Element.PACKAGE);
        addAttribute(pkg, Attribute.NAME, pkgDescr.getName());

        if(pkgDescr.hasDependencies()) {
            final ElementNode deps = newElement(pkg, Element.DEPENDENCIES);
            final String[] names = pkgDescr.getDependencies().toArray(new String[0]);
            Arrays.sort(names);
            for(String name : names) {
                writeDependency(deps, name);
            }
        }

        try (FormattingXMLStreamWriter writer = new FormattingXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)))) {
            writer.writeStartDocument();
            pkg.marshall(writer);
            writer.writeEndDocument();
        }
    }

    private static void writeDependency(ElementNode deps, String name) {
        addAttribute(newElement(deps, Element.DEPENDENCY), Attribute.NAME, name);
    }

    private static ElementNode newElement(ElementNode parent, Element e) {
        final ElementNode eNode = new ElementNode(parent, e.getLocalName(), PackageXMLParser10.NAMESPACE_1_0);
        if(parent != null) {
            parent.addChild(eNode);
        }
        return eNode;
    }

    private static void addAttribute(ElementNode e, Attribute a, String value) {
        e.addAttribute(a.getLocalName(), new AttributeValue(value));
    }
}
