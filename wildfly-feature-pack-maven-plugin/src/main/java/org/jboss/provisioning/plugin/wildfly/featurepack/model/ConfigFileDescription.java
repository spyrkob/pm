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
package org.jboss.provisioning.plugin.wildfly.featurepack.model;

import org.jboss.provisioning.plugin.wildfly.configassembly.InputStreamSource;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemConfig;
import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemsParser;
import org.jboss.provisioning.plugin.wildfly.configassembly.ZipEntryInputStreamSource;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigFileDescription {

    public static class Builder {
        private String template;
        private String subsystems;
        private String outputFile;
        private Map<String, String> properties = Collections.emptyMap();

        private Builder() {
        }

        public Builder setTemplate(String template) {
            this.template = template;
            return this;
        }

        public Builder setSubsystems(String subsystems) {
            this.subsystems = subsystems;
            return this;
        }

        public Builder setOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder addProperty(String name, String value) {
            switch(properties.size()) {
                case 0:
                    properties = Collections.singletonMap(name, value);
                    break;
                case 1:
                    properties = new HashMap<>(properties);
                default:
                    properties.put(name, value);
            }
            return this;
        }

        public ConfigFileDescription build() {
            return new ConfigFileDescription(Collections.unmodifiableMap(properties), template, subsystems, outputFile);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, String> properties;
    private final String template;
    private final String subsystems;
    private final String outputFile;

    private ConfigFileDescription(Map<String, String> properties, String template, String subsystems, String outputFile) {
        this.properties = properties;
        this.template = template;
        this.subsystems = subsystems;
        this.outputFile = outputFile;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getTemplate() {
        return template;
    }

    public String getSubsystems() {
        return subsystems;
    }

    public String getOutputFile() {
        return outputFile;
    }

    /**
     * Retrieves the subsystems configs.
     * @param featurePackFile the feature pack's file containing the subsystem configs
     * @return
     * @throws IOException
     * @throws XMLStreamException
     */
    public Map<String, Map<String, SubsystemConfig>> getSubsystemConfigs(File featurePackFile) throws IOException, XMLStreamException {
        Map<String, Map<String, SubsystemConfig>> subsystems = new HashMap<>();
        try (ZipFile zip = new ZipFile(featurePackFile)) {
            ZipEntry zipEntry = zip.getEntry(getSubsystems());
            if (zipEntry == null) {
                throw new RuntimeException("Feature pack " + featurePackFile + " subsystems file " + getSubsystems() + " not found");
            }
            InputStreamSource inputStreamSource = new ZipEntryInputStreamSource(featurePackFile, zipEntry);
            SubsystemsParser.parse(inputStreamSource, getProperties(), subsystems);
        }
        return subsystems;
    }
}
