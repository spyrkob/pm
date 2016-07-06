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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final Path workDir;
    private final Path homeDir;

    public FeaturePackBuild(InstallationDef installation, Path homeDir, Path workDir) {
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

        Path fpZip = workDir.resolve(Constants.FEATURE_PACKS);
        final GAV gav = fpDef.getGAV();
//        String[] parts = gav.getGroupId().split("\\.");
//        for(String part : parts) {
//            fpZip = new File(fpZip, part);
//        }
        fpZip = fpZip.resolve(gav.getGroupId());
        fpZip = fpZip.resolve(gav.getArtifactId());
        try {
            Files.createDirectories(fpZip);
        } catch (IOException e1) {
            throw new PMBuildException("Failed to create directory " + fpZip.toAbsolutePath());
        }

        fpZip = fpZip.resolve(gav.getVersion());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(fpZip))) {
            final Set<String> groupNames = fpDef.getGroupNames();
            for(String groupName : groupNames) {
                final GroupDef groupDef = fpDef.getGroupDef(groupName);
                copyGroupContent(zos, groupDef);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void copyGroupContent(ZipOutputStream zos, GroupDef groupDef) throws PMBuildException {
        if(!groupDef.hasContent()) {
            // for now it's skipped due to assumption the group simply references other groups
            // defined in the same feature pack
            return;
        }
        for(String relativePath : groupDef.getContentPaths()) {
            final Path src = homeDir.resolve(relativePath);
            if(!Files.exists(src)) {
                throw new PMBuildException("Failed to locate " + src.toAbsolutePath());
            }
            try {
                zos.putNextEntry(new ZipEntry(relativePath));
                copy(src, zos);
                zos.closeEntry();
            } catch (IOException e) {
                throw new PMBuildException("Failed to copy " + src.toAbsolutePath() + " to ZIP", e);
            }
        }
    }

    private static void copy(Path src, ZipOutputStream zos) throws IOException {
        try (final InputStream bis = Files.newInputStream(src)) {
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
