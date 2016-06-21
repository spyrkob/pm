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

package org.jboss.pm.def;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 * @author Alexey Loubyansky
 */
class DefLogger {

    private final StringWriter writer = new StringWriter();
    private BufferedWriter buf = new BufferedWriter(writer);
    private final int offsetStep = 2;
    private int offset;
    private boolean doOffset;

    void increaseOffset() {
        offset += offsetStep;
    }

    void decreaseOffset() {
        if(offset >= offsetStep) {
            offset -= offsetStep;
        }
    }

    void print(String str) throws IOException {
        if(doOffset) {
            offset();
            doOffset = false;
        }
        buf.write(str);
    }

    void newLine() throws IOException {
        buf.newLine();
        doOffset = true;
    }

    void println(String str) throws IOException {
        print(str);
        newLine();
    }

    private void offset() throws IOException {
        for(int i = 0; i < offset; ++i) {
            buf.append(' ');
        }
    }

    public String toString() {
        try {
            buf.flush();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        buf = null;
        return writer.toString();
    }
}
