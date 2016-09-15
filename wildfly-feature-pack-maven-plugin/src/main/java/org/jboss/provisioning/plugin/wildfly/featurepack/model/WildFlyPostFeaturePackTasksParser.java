/*
 * Copyright 2014 Red Hat, Inc.
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

package org.jboss.provisioning.plugin.wildfly.featurepack.model;


import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.provisioning.plugin.wildfly.PropertyResolver;
import org.jboss.staxmapper.XMLMapper;

/**
 * @author Alexey Loubyansky
 */
public class WildFlyPostFeaturePackTasksParser {

    private static final QName ROOT_2_0 = new QName(WildFlyPostFeaturePackTasksParser20.NAMESPACE_2_0, WildFlyPostFeaturePackTasksParser20.Element.TASKS.getLocalName());

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLMapper mapper;

    public WildFlyPostFeaturePackTasksParser(PropertyResolver properties) {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(ROOT_2_0, new WildFlyPostFeaturePackTasksParser20(properties));
    }

    public WildFlyPostFeaturePackTasks parse(final InputStream input) throws XMLStreamException {

        final XMLInputFactory inputFactory = INPUT_FACTORY;
        setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(input);
        final WildFlyPostFeaturePackTasks.Builder builder = WildFlyPostFeaturePackTasks.builder();
        mapper.parseDocument(builder, streamReader);
        return builder.build();
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }
}
