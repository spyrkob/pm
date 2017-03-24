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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.parameters.BuilderWithParameters;
import org.jboss.provisioning.parameters.PackageParameter;
import org.jboss.provisioning.util.ParsingUtils;
import org.jboss.provisioning.xml.util.ElementNode;
import org.jboss.provisioning.xml.util.TextNode;

/**
 *
 * @author Alexey Loubyansky
 */
class PackageParametersXml extends BaseXmlWriter {

    static final String NAME = "name";
    static final String PARAMETERS = "parameters";
    static final String PARAMETER = "parameter";

    private static final String ATTR_EQ = "=\"";
    private static final String ELEM_END = "</";
    private static final String EMPTY_ELEM_END = "/>";
    private static final String XMLNS = " xmlns";

    static void write(final ElementNode parent, Collection<PackageParameter> params) {
        final ElementNode paramsElement = addElement(parent, PARAMETERS, parent.getNamespace());
        for (PackageParameter param : params) {
            writeParameter(paramsElement, param);
        }
    }

    private static void writeParameter(ElementNode params, PackageParameter param) {
        final ElementNode paramElement = addElement(params, PARAMETER, params.getNamespace());
        addAttribute(paramElement, NAME, param.getName());
        paramElement.addChild(new TextNode(param.getValue()));
    }

    static void read(XMLStreamReader reader, BuilderWithParameters<?> pkgBuilder) throws XMLStreamException {
        ParsingUtils.parseNoAttributes(reader);
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT:
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    if (reader.getName().getLocalPart().equals(PARAMETER)) {
                        readParameter(reader, pkgBuilder);
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

    private static void readParameter(XMLStreamReader reader, BuilderWithParameters<?> pkgBuilder) throws XMLStreamException {
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

        final StringBuilder buf = new StringBuilder();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT:
                    if(buf.length() == 0) {
                        throw new XMLStreamException("Parameter " + name + " is missing value");
                    }
                    pkgBuilder.addParameter(name, buf.toString().trim());
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    append(reader, buf);
                    break;
                case XMLStreamConstants.CHARACTERS:
                    buf.append(reader.getText());
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }

    private static void append(XMLStreamReader reader, StringBuilder buf) throws XMLStreamException {

        buf.append('<');
        final QName name = reader.getName();
        if(!name.getPrefix().isEmpty()) {
            buf.append(name.getPrefix()).append(':');
        }
        buf.append(name.getLocalPart());

        for(int i = 0; i < reader.getNamespaceCount(); ++i) {
            buf.append(XMLNS);
            final String nsPrefix = reader.getNamespacePrefix(i);
            if(nsPrefix != null) {
                buf.append(':').append(nsPrefix);
            }
            buf.append(ATTR_EQ).append(reader.getNamespaceURI(i)).append('\"');
        }

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            buf.append(' ').append(reader.getAttributeName(i)).append(ATTR_EQ).append(reader.getAttributeValue(i)).append('\"');
        }

        boolean closedStartBracket = false;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.END_ELEMENT:
                    if(!closedStartBracket) {
                        buf.append(EMPTY_ELEM_END);
                    } else {
                        buf.append(ELEM_END);
                        if(!name.getPrefix().isEmpty()) {
                            buf.append(name.getPrefix()).append(':');
                        }
                        buf.append(name.getLocalPart()).append('>');
                    }
                    return;
                case XMLStreamConstants.START_ELEMENT:
                    if(!closedStartBracket) {
                        closedStartBracket = true;
                        buf.append('>');
                    }
                    append(reader, buf);
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if(!closedStartBracket) {
                        closedStartBracket = true;
                        buf.append('>');
                    }
                    buf.append(reader.getText());
                    break;
                default:
                    throw ParsingUtils.unexpectedContent(reader);
            }
        }
        throw ParsingUtils.endOfDocument(reader.getLocation());
    }
}
