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

package org.jboss.pm.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.codehaus.plexus.util.IOUtil;


/**
 *
 * @author Alexey Loubyansky
 */
class Util {

    private static final File TMP_DIR = new File(PropertyUtils.getSystemProperty("java.io.tmpdir"));

    static File createTmpDir(String name) {
        final File dir = new File(TMP_DIR, name);
        if(!dir.mkdirs()) {
            throw new IllegalStateException("Failed to create " + dir.getAbsolutePath());
        }
        return dir;
    }

    static File createRandomTmpDir() {
        return createTmpDir(UUID.randomUUID().toString());
    }

    static boolean recursiveDelete(File root) {
        if (root == null) {
            return true;
        }
        boolean ok = true;
        if (root.isDirectory()) {
            final File[] files = root.listFiles();
            for (File file : files) {
                ok &= recursiveDelete(file);
            }
            return ok && (root.delete() || !root.exists());
        } else {
            ok &= root.delete() || !root.exists();
        }
        return ok;
    }

    static void copy(File src, File trg) throws IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(src);
            output = new FileOutputStream(trg);
            IOUtil.copy(input, output);
        } finally {
            IOUtil.close(input);
            IOUtil.close(output);
        }
    }
}
