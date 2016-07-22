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
package org.jboss.provisioning.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Alexey Loubyansky
 */
public class HashUtils {

    private static final MessageDigest DIGEST;
    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hashPath(Path path) throws IOException {
        synchronized (DIGEST) {
            DIGEST.reset();
            updateDigest(DIGEST, path);
            return DIGEST.digest();
        }
    }

    private static void updateDigest(MessageDigest digest, Path path) throws IOException {
        if(Files.isDirectory(path)) {
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                final Map<String, Path> sortedChildren = new TreeMap<String, Path>();
                for(Path p : stream) {
                    sortedChildren.put(p.getFileName().toString(), p);
                }
                for (Path child : sortedChildren.values()) {
                    updateDigest(digest, child);
                }
            }
        } else {
            try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path))){
                byte[] bytes = new byte[8192];
                int read;
                while ((read = bis.read(bytes)) > -1) {
                    digest.update(bytes, 0, read);
                }
            }
        }
    }
}
