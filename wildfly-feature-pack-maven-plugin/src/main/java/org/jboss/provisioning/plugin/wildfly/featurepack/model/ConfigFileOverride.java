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
package org.jboss.provisioning.plugin.wildfly.featurepack.model;

import java.util.HashMap;
import java.util.Map;

import org.jboss.provisioning.plugin.wildfly.configassembly.SubsystemConfig;



/**
 *
 *
 * @author Eduardo Martins
 */
public class ConfigFileOverride {

    private final Map<String, String> properties;
    private final boolean useTemplate;
    private Map<String, Map<String, SubsystemConfig>> subsystems;
    private final String outputFile;

    public ConfigFileOverride(Map<String, String> properties, boolean useTemplate, Map<String, Map<String, SubsystemConfig>> subsystems, String outputFile) {
        this.properties = properties;
        this.useTemplate = useTemplate;
        this.subsystems = subsystems;
        this.outputFile = outputFile;
    }

    public ConfigFileOverride(boolean useTemplate, String outputFile) {
        this(new HashMap<String, String>(), useTemplate, null, outputFile);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isUseTemplate() {
        return useTemplate;
    }

    public Map<String, Map<String, SubsystemConfig>> getSubsystems() {
        return subsystems;
    }

    public void setSubsystems(Map<String, Map<String, SubsystemConfig>> subsystems) {
        this.subsystems = subsystems;
    }

    public String getOutputFile() {
        return outputFile;
    }

}
