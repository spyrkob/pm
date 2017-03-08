/*
 * Copyright 2016-2017 Red Hat, Inc. and/or its affiliates
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
package org.jboss.provisioning.plugin.wildfly;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility that copies content from reader to writer replacing the properties.
 *
 * @author Alexey Loubyansky
 */
public class PropertyReplacer {

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int RESOLVED = 3;
    private static final int DEFAULT = 4;

    public static void copy(final Path src, final Path target, PropertyResolver resolver) throws IOException {
        if(!Files.exists(target.getParent())) {
            Files.createDirectories(target.getParent());
        }
        try(BufferedReader reader = Files.newBufferedReader(src);
                BufferedWriter writer = Files.newBufferedWriter(target)) {
            copy(reader, writer, resolver);
        }
    }

    public static void copy(final Reader reader, Writer writer, PropertyResolver properties) throws IOException {
        int state = INITIAL;
        final StringBuilder buf = new StringBuilder();
        int ch = reader.read();
        while (ch >= 0) {
            switch (state) {
                case INITIAL: {
                    switch (ch) {
                        case '$': {
                            state = GOT_DOLLAR;
                            break;
                        }
                        default: {
                            writer.write(ch);
                        }
                    }
                    break;
                }
                case GOT_DOLLAR: {
                    switch (ch) {
                        case '$': {
                            // escaped $
                            buf.setLength(0);
                            writer.write(ch);
                            state = INITIAL;
                            break;
                        }
                        case '{': {
                            state = GOT_OPEN_BRACE;
                            break;
                        }
                        default: {
                            // invalid; emit and resume
                            writer.append('$');
                            writer.write(ch);
                            buf.setLength(0);
                            state = INITIAL;
                        }
                    }
                    break;
                }
                case GOT_OPEN_BRACE: {
                    switch (ch) {
                        case '}':
                        case ',': {
                            final String name = buf.toString();
                            if ("/".equals(name)) {
                                writer.append(File.separatorChar);
                                state = ch == '}' ? INITIAL : RESOLVED;
                            } else {
                                final String val = properties.resolveProperty(name);
                                if (val != null) {
                                    writer.write(val);
                                    // resolvedValue = val;
                                    state = ch == '}' ? INITIAL : RESOLVED;
                                } else if (ch == ',') {
                                    state = DEFAULT;
                                } else {
                                    throw new IllegalStateException("Failed to resolve expression: " + buf + ch);
                                }
                            }
                            buf.setLength(0);
                            break;
                        }
                        default: {
                            buf.appendCodePoint(ch);
                        }
                    }
                    break;
                }
                case RESOLVED: {
                    if (ch == '}') {
                        state = INITIAL;
                    }
                    break;
                }
                case DEFAULT: {
                    if (ch == '}') {
                        state = INITIAL;
                        final String val = properties.resolveProperty(buf.toString());
                        if (val != null) {
                            writer.write(val);
                        } else {
                            writer.write(buf.toString());
                        }
                    } else {
                        buf.appendCodePoint(ch);
                    }
                    break;
                }
                default:
                    throw new IllegalStateException("Unexpected char seen: " + ch);
            }
            ch = reader.read();
        }
        switch (state) {
            case GOT_DOLLAR: {
                writer.append('$');
                break;
            }
            case DEFAULT: {
                writer.write(buf.toString());
                break;
            }
            case GOT_OPEN_BRACE: {
                // We had a reference that was not resolved, throw ISE
//                if (resolvedValue == null)
                    throw new IllegalStateException("Incomplete expression: " + buf.toString());
            }
        }
    }

    public static void main(String[] args) throws Exception {

        final StringWriter writer = new StringWriter();

        //final StringReader reader = new StringReader(" f g $$ $ { ${a} d ${d,default} f");

        try(BufferedReader reader = Files.newBufferedReader(Paths.get(
                "/home/olubyans/git/wildfly/dist/src/distribution/resources/modules/system/layers/base/org/jboss/as/product/wildfly-full/dir/META-INF/MANIFEST.MF"))) {
            copy(reader, writer, new PropertyResolver(){
                @Override
                public String resolveProperty(String property) {
                    if("product.release.name".equals(property)) {
                        return "RELEASE";
                    } else if("project.version".equals(property)) {
                        return "VERSION";
                    }
                    return null;
                }});
        }

        System.out.println("'" + writer.getBuffer().toString() + "'");
    }
}