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
