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

package org.jboss.provisioning.plugin.wildfly;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import javax.xml.stream.XMLStreamException;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.util.plugin.ProvisioningContext;
import org.jboss.provisioning.util.plugin.ProvisioningPlugin;

/**
 *
 * @author Alexey Loubyansky
 */
public class WFProvisioningPlugin implements ProvisioningPlugin {

    /* (non-Javadoc)
     * @see org.jboss.provisioning.util.plugin.ProvisioningPlugin#execute()
     */
    @Override
    public void execute(ProvisioningContext ctx) throws ProvisioningException {

        final Path templatesDir = ctx.getResourcesDir().resolve("wildfly").resolve("configuration");
        if(!Files.exists(templatesDir)) {
            System.out.println("Configuration templates not found.");
            return;
        }

        System.out.println("WF CONFIG ASSEMBLER layout=" + ctx.getLayoutDir() + " install-dir=" + ctx.getInstallDir());

        final Path layoutDir = ctx.getLayoutDir();
        for(FeaturePackDescription fpDescr : ctx.getInstallationDescription().getFeaturePacks()) {
            final PackageDescription modulesDescr = fpDescr.getPackageDescription("modules");
            if(modulesDescr == null) {
                continue;
            }
            final GAV fpGav = fpDescr.getGAV();
            final Path fpDir = layoutDir.resolve(fpGav.getGroupId()).resolve(fpGav.getArtifactId()).resolve(fpGav.getVersion());
            System.out.println(" processing " + fpGav + " " + fpDir);
            final Path packagesDir = fpDir.resolve(Constants.PACKAGES);
            for(String pkgName : modulesDescr.getDependencies()) {
                final Path pkgContent = packagesDir.resolve(pkgName).resolve(Constants.CONTENT);
                if(!Files.exists(pkgContent)) {
                    continue;
                }
                try {
                    Files.walkFileTree(pkgContent, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if(!file.getFileName().toString().equals("module.xml")) {
                                return FileVisitResult.CONTINUE;
                            }

                            try {
                                final ModuleParseResult parsedModule = ModuleXmlParser.parse(file, ctx.getEncoding());
                                for(ModuleParseResult.ArtifactName artName : parsedModule.artifacts) {
                                    try {
                                        ctx.resolveArtifact(GAV.fromString(artName.getArtifactCoords()), "jar");
                                    } catch(ProvisioningException e) {
                                        System.out.println("FAILED to resolve " + artName.getArtifactCoords());
                                    }
                                }
                            } catch (XMLStreamException e) {
                                e.printStackTrace();
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.TERMINATE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    throw new ProvisioningException("Failed to process package " + pkgName + " of " + fpGav, e);
                }
            }
        }
    }
}
