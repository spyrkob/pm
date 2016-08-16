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
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Martins
 */
public class ZipEntryInputStreamSource implements InputStreamSource {

    private final File file;
    private final ZipEntry zipEntry;

    public ZipEntryInputStreamSource(File file, ZipEntry zipEntry) {
        this.file = file;
        this.zipEntry = zipEntry;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        final ZipFile zipFile = new ZipFile(file);
        try {
            return new ZipEntryInputStream(zipFile, zipFile.getInputStream(zipEntry));
        } catch (Throwable t) {
            try {
                zipFile.close();
            } catch (Throwable ignore) {

            }
            throw new IOException("failed to retrieve input stream for " + zipEntry.getName()
                    + " from " + file.getAbsolutePath(), t);
        }
    }

    private static class ZipEntryInputStream extends InputStream {

        private final ZipFile zipFile;
        private final InputStream zipEntryInputStream;

        private ZipEntryInputStream(ZipFile zipFile, InputStream zipEntryInputStream) {
            if(zipEntryInputStream == null) {
                throw new IllegalArgumentException("zipEntryInputStream is null");
            }
            this.zipFile = zipFile;
            this.zipEntryInputStream = zipEntryInputStream;
        }

        @Override
        public int read() throws IOException {
            return zipEntryInputStream.read();
        }

        @Override
        public int available() throws IOException {
            return zipEntryInputStream.available();
        }

        @Override
        public void close() throws IOException {
            try {
                zipEntryInputStream.close();
            } finally {
                try {
                    this.zipFile.close();
                } catch (Throwable t) {
                    // ignore
                    t.printStackTrace();
                }
            }
        }

        @Override
        public synchronized void mark(int readlimit) {
            zipEntryInputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            zipEntryInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return zipEntryInputStream.markSupported();
        }
    }
}
