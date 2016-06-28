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

package org.jboss.pm.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author Alexey Loubyansky
 */
class ZipUtils {

    /**
     * unpack...
     *
     * @param zip the zip
     * @param patchDir the patch dir
     * @throws IOException
     */
    public static void unzip(final File zip, final File patchDir) throws IOException {
        try (final ZipFile zipFile = new ZipFile(zip)){
            unzip(zipFile, patchDir);
            zipFile.close();
        }
    }

    /**
     * unpack...
     *
     * @param zip the zip
     * @param patchDir the patch dir
     * @throws IOException
     */
    private static void unzip(final ZipFile zip, final File patchDir) throws IOException {
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        while(entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final String name = entry.getName();
            final File current = new File(patchDir, name);
            if(entry.isDirectory()) {
                continue;
            } else {
                if(! current.getParentFile().exists()) {
                    current.getParentFile().mkdirs();
                }
                try (final InputStream eis = zip.getInputStream(entry)){
                    Files.copy(eis, current.toPath());
                    //copy(eis, current);
                }
            }
        }
    }

}
