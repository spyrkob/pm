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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;
import org.jboss.provisioning.xml.ParameterSetXmlParser10.Attribute;
import org.jboss.provisioning.xml.ParameterSetXmlParser10.Element;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.FormattingXmlStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ParameterSetXmlWriter extends BaseXmlWriter {

    private static final ParameterSetXmlWriter INSTANCE = new ParameterSetXmlWriter();

    public static ParameterSetXmlWriter getInstance() {
        return INSTANCE;
    }

    private ParameterSetXmlWriter() {
    }

    public void write(ParameterSet configSpec, Path outputFile) throws XMLStreamException, IOException {

        final ElementNode pkg = addElement(null, Element.CONFIG);
        addAttribute(pkg, Attribute.NAME, configSpec.getName());

        if(configSpec.hasParameters()) {
            for(PackageParameter param : configSpec.getParameters()) {
                PackageParametersXml.writeParameter(pkg, param);
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
}
