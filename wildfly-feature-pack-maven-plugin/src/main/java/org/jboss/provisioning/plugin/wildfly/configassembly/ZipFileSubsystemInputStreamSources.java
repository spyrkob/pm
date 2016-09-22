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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class ZipFileSubsystemInputStreamSources implements SubsystemInputStreamSources {

    private final Map<String, ZipEntryInputStreamSource> inputStreamSourceMap = new HashMap<>();

    /**
     * Creates a zip entry inputstream source and maps it to the specified filename.
     * @param subsystemFileName
     * @param zipFile
     * @param zipEntry
     */
    public void addSubsystemFileSource(String subsystemFileName, File zipFile, ZipEntry zipEntry) {
       inputStreamSourceMap.put(subsystemFileName, new ZipEntryInputStreamSource(zipFile, zipEntry));
    }

    /**
     * Adds all subsystem input stream sources from the specified factory. Note that only absent sources will be added.
     * @param other
     */
    public void addAllSubsystemFileSources(ZipFileSubsystemInputStreamSources other) {
        for (Map.Entry<String, ZipEntryInputStreamSource> entry : other.inputStreamSourceMap.entrySet()) {
            if (!this.inputStreamSourceMap.containsKey(entry.getKey())) {
                this.inputStreamSourceMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds all file sources in the specified zip file.
     * @param file
     * @throws IOException
     */
    public void addAllSubsystemFileSourcesFromZipFile(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            // extract subsystem template and schema, if present
            if (zip.getEntry("subsystem-templates") != null) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        String entryName = entry.getName();
                        if (entryName.startsWith("subsystem-templates/")) {
                            addSubsystemFileSource(entryName.substring("subsystem-templates/".length()), file, entry);
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds the file source for the specified subsystem, from the specified zip file.
     * @param subsystem
     * @param file
     * @return true if such subsystem file source was found and added; false otherwise
     * @throws IOException
     */
    public boolean addSubsystemFileSourceFromZipFile(String subsystem, File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String entryName = "subsystem-templates/"+subsystem;
            ZipEntry entry = zip.getEntry(entryName);
            if (entry != null) {
                addSubsystemFileSource(subsystem, file, entry);
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStreamSource getInputStreamSource(String subsystemFileName) {
        return inputStreamSourceMap.get(subsystemFileName);
    }

    @Override
    public String toString() {
        return "zip subsystem parser factory files: "+ inputStreamSourceMap.keySet();
    }
}
