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

package org.jboss.pm.build;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.pm.Constants;
import org.jboss.pm.GAV;
import org.jboss.pm.def.FeaturePackDef;
import org.jboss.pm.def.GroupDef;
import org.jboss.pm.def.InstallationDef;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackBuild {

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    private final InstallationDef installation;
    private final File workDir;
    private final File homeDir;

    public FeaturePackBuild(InstallationDef installation, File homeDir, File workDir) {
        this.installation = installation;
        this.workDir = workDir;
        this.homeDir = homeDir;
    }

    public void buildFeaturePacks() throws PMBuildException {
        if(!installation.hasFeaturePacks()) {
            return;
        }

        for(FeaturePackDef fpDef : installation.getFeaturePackDefs()) {
            buildFeaturePack(fpDef);
        }
    }

    private void buildFeaturePack(FeaturePackDef fpDef) throws PMBuildException {

        File fpZip = new File(workDir, Constants.FEATURE_PACKS);
        final GAV gav = fpDef.getGAV();
//        String[] parts = gav.getGroupId().split("\\.");
//        for(String part : parts) {
//            fpZip = new File(fpZip, part);
//        }
        fpZip = new File(fpZip, gav.getGroupId());
        fpZip = new File(fpZip, gav.getArtifactId());
        fpZip.mkdirs();

        fpZip = new File(fpZip, gav.getVersion());
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(fpZip));
            final Set<String> groupNames = fpDef.getGroupNames();
            for(String groupName : groupNames) {
                final GroupDef groupDef = fpDef.getGroupDef(groupName);
                copyGroupContent(zos, groupDef);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch(IOException e) {
            }
        }
    }

    private void copyGroupContent(ZipOutputStream zos, GroupDef groupDef) throws PMBuildException {
        if(!groupDef.hasContent()) {
            // for now it's skipped due to assumption the group simply references other groups
            // defined in the same feature pack
            return;
        }
        for(String relativePath : groupDef.getContentPaths()) {
            final File src = new File(homeDir, relativePath);
            if(!src.exists()) {
                throw new PMBuildException("Failed to locate " + src.getAbsolutePath());
            }
            try {
                zos.putNextEntry(new ZipEntry(relativePath));
                copy(src, zos);
                zos.closeEntry();
            } catch (IOException e) {
                throw new PMBuildException("Failed to copy " + src.getAbsolutePath() + " to ZIP", e);
            }
        }
    }

    private static void copy(File src, ZipOutputStream zos) throws IOException {
        try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src))){
            copyStream(bis, zos);
        }
    }

    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buff = new byte[DEFAULT_BUFFER_SIZE];
        int rc;
        while ((rc = is.read(buff)) != -1) os.write(buff, 0, rc);
        os.flush();
    }
}
