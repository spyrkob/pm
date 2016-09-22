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
package org.jboss.provisioning.plugin.wildfly.configassembly;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexey Loubyansky
 */
public class ServerProvisioning {

    public static class Config {

        private final ZipFileSubsystemInputStreamSources inputStreamSources = new ZipFileSubsystemInputStreamSources();
        private final Map<String, ConfigFile> standaloneConfigFiles = new HashMap<>();
        private final Map<String, ConfigFile> domainConfigFiles = new HashMap<>();
        private final Map<String, ConfigFile> hostConfigFiles = new HashMap<>();

        public Map<String, ConfigFile> getStandaloneConfigFiles() {
            return standaloneConfigFiles;
        }

        public Map<String, ConfigFile> getDomainConfigFiles() {
            return domainConfigFiles;
        }

        public ZipFileSubsystemInputStreamSources getInputStreamSources() {
            return inputStreamSources;
        }

        public Map<String, ConfigFile> getHostConfigFiles() {
            return hostConfigFiles;
        }
    }

    public static class ConfigFile {

        private InputStreamSource templateInputStreamSource;
        private final Map<String, Map<String, SubsystemConfig>> subsystems;
        private final String outputFile;

        public ConfigFile(String outputFile) {
            this.subsystems = new HashMap<>();
            this.outputFile = outputFile;
        }

        public InputStreamSource getTemplateInputStreamSource() {
            return templateInputStreamSource;
        }

        public void setTemplateInputStreamSource(InputStreamSource templateInputStreamSource) {
            this.templateInputStreamSource = templateInputStreamSource;
        }

        public Map<String, Map<String, SubsystemConfig>> getSubsystems() {
            return subsystems;
        }

        public String getOutputFile() {
            return outputFile;
        }
    }
/*
    private void processFeaturePackConfigFile(ConfigFile configFile, ZipFile zipFile, ServerProvisioningFeaturePack provisioningFeaturePack, Map<String, ServerProvisioning.ConfigFile> provisioningConfigFiles)
            throws IOException, XMLStreamException {
        // get provisioning config file for the output file being processed
        ServerProvisioning.ConfigFile provisioningConfigFile = provisioningConfigFiles.get(configFile.getOutputFile());
        if (provisioningConfigFile == null) {
            // the provisioning config file does not exists yet, create one
            provisioningConfigFile = new ServerProvisioning.ConfigFile(configFile.getOutputFile());
            provisioningConfigFiles.put(configFile.getOutputFile(), provisioningConfigFile);
        }

        // get this config file subsystems
        Map<String, Map<String, SubsystemConfig>> subsystems = serverProvisioningFeaturePackConfigFile.getSubsystems();
        // merge the subsystems in the provisioning config file
        for (Map.Entry<String, Map<String, SubsystemConfig>> subsystemsEntry : subsystems.entrySet()) {
            // get the subsystems in the provisioning config file
            String profileName = subsystemsEntry.getKey();
            Map<String, SubsystemConfig> subsystemConfigMap = subsystemsEntry.getValue();
            Map<String, SubsystemConfig> provisioningSubsystems = provisioningConfigFile.getSubsystems().get(profileName);
            if (provisioningSubsystems == null) {
                // do not exist yet, create it
                provisioningSubsystems = new LinkedHashMap<>();
                provisioningConfigFile.getSubsystems().put(profileName, provisioningSubsystems);
            }
            // add the 'new' subsystem configs and related input stream sources
            for (Map.Entry<String, SubsystemConfig> subsystemConfigMapEntry : subsystemConfigMap.entrySet()) {
                String subsystemFile = subsystemConfigMapEntry.getKey();
                SubsystemConfig subsystemConfig = subsystemConfigMapEntry.getValue();
                //getLog().debugf("Adding subsystem config %s to provisioning config file %s", subsystemFile, provisioningConfigFile.getOutputFile());
                // put subsystem config
                provisioningSubsystems.put(subsystemFile, subsystemConfig);
            }
        }
    }
    */
}
