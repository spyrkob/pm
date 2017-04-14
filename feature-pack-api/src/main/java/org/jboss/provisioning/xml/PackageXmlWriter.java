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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.spec.PackageDependencyGroupSpec;
import org.jboss.provisioning.spec.PackageDependencySpec;
import org.jboss.provisioning.spec.PackageSpec;
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

    private static final PackageXmlWriter INSTANCE = new PackageXmlWriter();

    public static PackageXmlWriter getInstance() {
        return INSTANCE;
    }

    private PackageXmlWriter() {
    }

    public void write(PackageSpec pkgSpec, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = addElement(null, Element.PACKAGE_SPEC);
        addAttribute(pkg, Attribute.NAME, pkgSpec.getName());

        ElementNode deps = null;
        if(pkgSpec.hasLocalDependencies()) {
            deps = addElement(pkg, Element.DEPENDENCIES);
            final PackageDependencySpec[] pkgDeps = pkgSpec.getLocalDependencies().getDescriptions().toArray(new PackageDependencySpec[0]);
            Arrays.sort(pkgDeps);
            for(PackageDependencySpec depSpec : pkgDeps) {
                writePackageDependency(deps, depSpec);
            }
        }
        if(pkgSpec.hasExternalDependencies()) {
            if(deps == null) {
                deps = addElement(pkg, Element.DEPENDENCIES);
            }
            final String[] names = pkgSpec.getExternalDependencyNames().toArray(new String[0]);
            Arrays.sort(names);
            for(String name : names) {
                writeFeaturePackDependency(deps, pkgSpec.getExternalDependencies(name));
            }
        }
        if(pkgSpec.hasParameters()) {
            PackageParametersXml.write(pkg, pkgSpec.getParameters());
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

    private static void writeFeaturePackDependency(ElementNode deps, PackageDependencyGroupSpec depGroupSpec) {
        final ElementNode fpElement = addElement(deps, Element.FEATURE_PACK);
        addAttribute(fpElement, Attribute.DEPENDENCY, depGroupSpec.getGroupName());
        final PackageDependencySpec[] pkgDeps = depGroupSpec.getDescriptions().toArray(new PackageDependencySpec[0]);
        Arrays.sort(pkgDeps);
        for(PackageDependencySpec depSpec : pkgDeps) {
            writePackageDependency(fpElement, depSpec);
        }
    }

    private static void writePackageDependency(ElementNode deps, PackageDependencySpec depSpec) {
        final ElementNode depElement = addElement(deps, Element.PACKAGE);
        addAttribute(depElement, Attribute.NAME, depSpec.getName());
        if(depSpec.isOptional()) {
            addAttribute(depElement, Attribute.OPTIONAL, TRUE);
        }
        if(depSpec.hasParams()) {
            PackageParametersXml.write(depElement, depSpec.getParameters());
        }
        if(depSpec.hasConfigs()) {
            ParameterSetsXml.write(depElement, depSpec.getConfigs());
        }
    }
}
