/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
