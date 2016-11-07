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

import java.io.Reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.descr.ResolvedInstallationDescription;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProvisionedInstallationXmlParser implements XmlParser<ResolvedInstallationDescription> {

    private static final QName ROOT_1_0 = new QName(ProvisionedInstallationXmlParser10.NAMESPACE_1_0, ProvisionedInstallationXmlParser10.Element.INSTALLATION.getLocalName());

    private static final XMLInputFactory inputFactory;
    static {
        final XMLInputFactory tmpIF = XMLInputFactory.newInstance();
        setIfSupported(tmpIF, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(tmpIF, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inputFactory = tmpIF;
    }

    private static void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private final XMLMapper mapper;

    public ProvisionedInstallationXmlParser() {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_1_0, new ProvisionedInstallationXmlParser10());
    }

    @Override
    public ResolvedInstallationDescription parse(final Reader input) throws XMLStreamException {

        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        final ResolvedInstallationDescription.Builder builder = ResolvedInstallationDescription.builder();
        mapper.parseDocument(builder, streamReader);
        return builder.build();
    }
}
