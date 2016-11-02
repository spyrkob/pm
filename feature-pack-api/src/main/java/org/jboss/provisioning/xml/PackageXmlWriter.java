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

import org.jboss.provisioning.descr.PackageDependencyDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.xml.PackageXmlParser10.Attribute;
import org.jboss.provisioning.xml.PackageXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class PackageXmlWriter extends BaseXmlWriter {

    private static final String TRUE = "true";

    public static final PackageXmlWriter INSTANCE = new PackageXmlWriter();

    private PackageXmlWriter() {
    }

    public void write(PackageDescription pkgDescr, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = addElement(null, Element.PACKAGE_SPEC);
        addAttribute(pkg, Attribute.NAME, pkgDescr.getName());

        if(pkgDescr.hasLocalDependencies()) {
            final ElementNode deps = addElement(pkg, Element.DEPENDENCIES);
            final PackageDependencyDescription[] pkgDeps = pkgDescr.getLocalDependencies().getDescriptions().toArray(new PackageDependencyDescription[0]);
            Arrays.sort(pkgDeps);
            for(PackageDependencyDescription depDescr : pkgDeps) {
                writePackageDependency(deps, depDescr);
            }
        }

        ensureParentDir(outputFile);
        try (FormattingXmlStreamWriter writer = new FormattingXmlStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(
                        Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)))) {
            writer.writeStartDocument();
            pkg.marshall(writer);
            writer.writeEndDocument();
        }
    }

    private static void writePackageDependency(ElementNode deps, PackageDependencyDescription depDescr) {
        final ElementNode depElement = addElement(deps, Element.PACKAGE);
        addAttribute(depElement, Attribute.NAME, depDescr.getName());
        if(depDescr.isOptional()) {
            addAttribute(depElement, Attribute.OPTIONAL, TRUE);
        }
    }
}
