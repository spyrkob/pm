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

import java.io.Reader;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.config.schema.ConfigSchema;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackSchemaXmlParser implements XmlParser<ConfigSchema> {

    private static final FeaturePackSchemaXmlParser INSTANCE = new FeaturePackSchemaXmlParser();

    public static FeaturePackSchemaXmlParser getInstance() {
        return INSTANCE;
    }

    private FeaturePackSchemaXmlParser() {
    }

    @Override
    public ConfigSchema parse(final Reader input) throws XMLStreamException {
        final ConfigSchema.Builder builder = ConfigSchema.builder();
        XmlParsers.parse(input, builder);
        try {
            return builder.build();
        } catch (ProvisioningDescriptionException e) {
            throw new XMLStreamException("Failed to create config schema", e);
        }
    }
}
