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

import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.parameters.ParameterSet;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.provisioning.xml.util.ElementNode;

/**
 *
 * @author Alexey Loubyansky
 */
class ParameterSetsXml extends BaseXmlWriter {

    static final String NAME = "name";
    static final String CONFIG = "config";

    static void write(final ElementNode parent, Collection<ParameterSet> configs) {
        for (ParameterSet config : configs) {
            writeConfig(parent, config);
        }
    }

    private static void writeConfig(ElementNode params, ParameterSet config) {
        final ElementNode configElement = addElement(params, CONFIG, params.getNamespace());
        addAttribute(configElement, NAME, config.getName());
        for(PackageParameter param : config.getParameters()) {
            PackageParametersXml.writeParameter(configElement, param);
        }
    }

    static ParameterSet readConfig(XMLStreamReader reader) throws XMLStreamException {
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if(reader.getAttributeName(i).getLocalPart().equals(NAME)) {
                name = reader.getAttributeValue(i);
            } else {
                throw ParsingUtils.unexpectedContent(reader);
            }
        }
        if (name == null) {
            throw ParsingUtils.missingAttributes(reader.getLocation(), Collections.singleton(new XmlNameProvider() {
                @Override
                public String getNamespace() {
                    return "";
                }
                @Override
                public String getLocalName() {
                    return NAME;
                }}));
        }
        final ParameterSet.Builder configBuilder = ParameterSet.builder(name);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return configBuilder.build();
                case XMLStreamConstants.START_ELEMENT:
                    if (reader.getName().getLocalPart().equals(PackageParametersXml.PARAMETER)) {
                        PackageParametersXml.readParameter(reader, configBuilder);
                    } else {
                        throw ParsingUtils.unexpectedContent(reader);
                    }
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }
}
