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
package org.jboss.provisioning.plugin.wildfly.config;


import java.io.Reader;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.xml.XmlParser;
import org.jboss.provisioning.xml.XmlParsers;
import org.jboss.staxmapper.XMLMapper;

/**
 * @author Alexey Loubyansky
 */
public class PackageScriptsParser implements XmlParser<PackageScripts> {

    private static final PackageScriptsParser INSTANCE = new PackageScriptsParser();

    public static PackageScriptsParser getInstance() {
        return INSTANCE;
    }

    private final XMLMapper mapper;

    private PackageScriptsParser() {
        mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(PackageScriptsParser10.getInstance().getRoot(), PackageScriptsParser10.getInstance());
        mapper.registerRootElement(PackageScriptXmlParser10.getInstance().getRoot(), PackageScriptXmlParser10.getInstance());
    }

    public void parse(final Reader input, final PackageScripts.Builder fpBuilder) throws XMLStreamException {
        mapper.parseDocument(fpBuilder, XmlParsers.createXMLStreamReader(input));
    }

    public List<PackageScripts.Script> parseScript(Reader input) throws XMLStreamException {
        final PackageScripts.ScriptsBuilder builder = new PackageScripts.ScriptsBuilder();
        mapper.parseDocument(builder, XmlParsers.createXMLStreamReader(input));
        return builder.build();
    }

    @Override
    public PackageScripts parse(Reader input) throws XMLStreamException {
        final PackageScripts.Builder builder = PackageScripts.builder();
        parse(input, builder);
        return builder.build();
    }
}
