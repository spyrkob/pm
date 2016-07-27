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

package org.jboss.provisioning.util.analyzer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.Constants;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.GAV;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.HashUtils;
import org.jboss.provisioning.util.analyzer.FeaturePackSpecificDescription.Builder;

/**
 * Analyzes feature pack layouts with the goal to identify common dependencies,
 * content and differences between feature packs.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePacksDiff {

    static FeaturePacksDiff newInstance(Path fpLayoutDir, GAV gav1, GAV gav2) throws InstallationDescriptionException {
        return new FeaturePacksDiff(getFeaturePackDir(fpLayoutDir, gav1), getFeaturePackDir(fpLayoutDir, gav2));
    }

    public static FeaturePackDescriptionDiffs compare(Path fpLayoutDir, GAV gav1, GAV gav2) throws InstallationDescriptionException {
        return newInstance(fpLayoutDir, gav1, gav2).compare();
    }

    private static Path getFeaturePackDir(Path fpLayoutDir, GAV gav) throws InstallationDescriptionException {
        final Path fpPath = fpLayoutDir.resolve(gav.getGroupId()).resolve(gav.getArtifactId()).resolve(gav.getVersion());
        if(!Files.exists(fpPath)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(fpPath));
        }
        return fpPath;
    }

    private static Path getPackageDir(Path fpDir, String packageName) throws InstallationDescriptionException {
        final Path dir = fpDir.resolve(Constants.PACKAGES).resolve(packageName);
        if(!Files.exists(dir)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(dir));
        }
        return dir;
    }

    private static byte[] hashPath(Path path) throws InstallationDescriptionException {
        try {
            return HashUtils.hashPath(path);
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.hashCalculation(path), e);
        }
    }

    private final Path fp1Dir;
    private final Path fp2Dir;
    private final FeaturePackDescription fp1Descr;
    private final FeaturePackDescription fp2Descr;

    FeaturePacksDiff(Path fp1Dir, Path fp2Dir) throws InstallationDescriptionException {
        this.fp1Dir = fp1Dir;
        this.fp2Dir = fp2Dir;
        fp1Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp1Dir);
        fp2Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp2Dir);
    }

    FeaturePackDescription getFeaturePackDescription1() {
        return fp1Descr;
    }

    FeaturePackDescription getFeaturePackDescription2() {
        return fp2Descr;
    }

    FeaturePackDescriptionDiffs compare() throws InstallationDescriptionException {
        final Builder fp1Diff = FeaturePackSpecificDescription.builder(fp1Descr.getGAV());
        final Builder fp2Diff = FeaturePackSpecificDescription.builder(fp2Descr.getGAV());
        compareDependencies(fp1Diff, fp2Diff);
        comparePackages(fp1Diff, fp2Diff);
        return new FeaturePackDescriptionDiffs(fp1Diff.build(), fp2Diff.build());
    }

    private void comparePackages(final Builder fp1Diff, final Builder fp2Diff) throws InstallationDescriptionException {
        if(!fp1Descr.hasPackages()) {
            if(fp2Descr.hasPackages()) {
                fp2Diff.addAllUniquePackages(fp2Descr.getPackages());
            }
        } else {
            if(!fp2Descr.hasPackages()) {
                fp1Diff.addAllUniquePackages(fp1Descr.getPackages());
            } else {
                final Set<String> fp2PkgNames = new HashSet<String>(fp2Descr.getPackageNames());
                for(String fp1PkgName : fp1Descr.getPackageNames()) {
                    if(fp2PkgNames.remove(fp1PkgName)) {
                        comparePackages(fp1Descr.getPackageDescription(fp1PkgName),
                                fp2Descr.getPackageDescription(fp1PkgName),
                                fp1Diff, fp2Diff);
                    } else {
                        fp1Diff.addUniquePackage(fp1Descr.getPackageDescription(fp1PkgName));
                    }
                }
                if(!fp2PkgNames.isEmpty()) {
                    for(String pkgName : fp2PkgNames) {
                        fp2Diff.addUniquePackage(fp2Descr.getPackageDescription(pkgName));
                    }
                }
            }
        }
    }

    private void comparePackages(PackageDescription fp1Pkg, PackageDescription fp2Pkg, Builder fp1Diff, Builder fp2Diff) throws InstallationDescriptionException {
        final PackageSpecificDescription.Builder g1Diff = PackageSpecificDescription.builder(fp1Pkg.getName());
        final PackageSpecificDescription.Builder g2Diff = PackageSpecificDescription.builder(fp2Pkg.getName());

        compareDependencies(fp1Pkg, fp2Pkg, g1Diff, g2Diff);

        final Path g1Content = getPackageDir(fp1Dir, fp1Pkg.getName()).resolve(Constants.CONTENT);
        final Path g2Content = getPackageDir(fp2Dir, fp2Pkg.getName()).resolve(Constants.CONTENT);

        final boolean g1ContentExists = Files.exists(g1Content);
        final boolean g2ContentExists = Files.exists(g2Content);
        if(g1ContentExists != g2ContentExists) {
            g1Diff.setContentExists(g1ContentExists);
            g2Diff.setContentExists(g2ContentExists);
        } else if(g1ContentExists && g2ContentExists) {
            comparePackageContent(g1Content, g2Content, g1Diff, g2Diff);
        }

        if(g1Diff.hasRecords()) {
            fp1Diff.addConflictingPackage(g1Diff.build());
        }
        if(g2Diff.hasRecords()) {
            fp2Diff.addConflictingPackage(g2Diff.build());
        } else if(!g1Diff.hasRecords()) {
            fp1Diff.addMatchedPackage(fp1Pkg);
            fp2Diff.addMatchedPackage(fp2Pkg);
        }
    }

    private void comparePackageContent(Path g1Content, Path g2Content,
            PackageSpecificDescription.Builder g1Diff, PackageSpecificDescription.Builder g2Diff) throws InstallationDescriptionException {

        final ContentDiff.Builder c1Builder = ContentDiff.builder();
        final ContentDiff.Builder c2Builder = ContentDiff.builder();

        compareDirs(g1Content, g2Content, g1Content, g2Content, c1Builder, c2Builder);

        if(c1Builder.hasRecords()) {
            g1Diff.setContentDiff(c1Builder.build());
        }
        if(c2Builder.hasRecords()) {
            g2Diff.setContentDiff(c2Builder.build());
        }
    }

    private void compareDirs(Path pkg1Dir, Path pkg2Dir, Path c1Dir, Path c2Dir,
            final ContentDiff.Builder c1Builder, final ContentDiff.Builder c2Builder) throws InstallationDescriptionException {
        final Map<String, Path> c2Children = getChildren(c2Dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(c1Dir)) {
            if(c2Children.isEmpty()) {
                for (Path c1 : stream) {
                    c1Builder.addUniquePath(pkg1Dir.relativize(c1).toString());
                }
            } else {
                for (Path c1 : stream) {
                    final Path c2 = c2Children.remove(c1.getFileName().toString());
                    if (c2 == null) {
                        c1Builder.addUniquePath(pkg1Dir.relativize(c1).toString());
                    } else {
                        if (Files.isDirectory(c1)) {
                            if (!Files.isDirectory(c2)) {
                                c1Builder.addConflictPath(pkg1Dir.relativize(c1).toString());
                                c2Builder.addConflictPath(pkg2Dir.relativize(c2).toString());
                            } else {
                                compareDirs(pkg1Dir, pkg2Dir, c1, c2, c1Builder, c2Builder);
                            }
                        } else if (Files.isDirectory(c2)) {
                            c1Builder.addConflictPath(pkg1Dir.relativize(c1).toString());
                            c2Builder.addConflictPath(pkg2Dir.relativize(c2).toString());
                        } else if (c1.getFileName().toString().endsWith(".jar")) {
                            if(!Arrays.equals(HashUtils.hashJar(c1, true), HashUtils.hashJar(c2, true))) {
                                c1Builder.addConflictPath(pkg1Dir.relativize(c1).toString());
                                c2Builder.addConflictPath(pkg2Dir.relativize(c2).toString());
                            }
                        } else if (!Arrays.equals(hashPath(c1), hashPath(c2))) {
                            c1Builder.addConflictPath(pkg1Dir.relativize(c1).toString());
                            c2Builder.addConflictPath(pkg2Dir.relativize(c2).toString());
                        }
                    }
                }
                if (!c2Children.isEmpty()) {
                    for (Path c2 : c2Children.values()) {
                        c2Builder.addUniquePath(pkg2Dir.relativize(c2).toString());
                    }
                }
            }
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.readDirectory(pkg1Dir));
        }
    }

    private static Map<String, Path> getChildren(Path p) throws InstallationDescriptionException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
            Map<String, Path> children = Collections.emptyMap();
            for (Path c : stream) {
                if (children.isEmpty()) {
                    children = new HashMap<String, Path>(children);
                }
                children.put(c.getFileName().toString(), c);
            }
            return children;
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.readDirectory(p));
        }
    }

    private void compareDependencies(final PackageDescription fp1Pkg, final PackageDescription fp2Pkg,
            final PackageSpecificDescription.Builder fp1PkgDiff, final PackageSpecificDescription.Builder fp2PkgDiff) {
        if(!fp1Pkg.hasDependencies()) {
            if(fp2Pkg.hasDependencies()) {
                fp2PkgDiff.addAllDependencies(fp2Pkg.getDependencies());
            }
        } else {
            if(!fp2Pkg.hasDependencies()) {
                fp1PkgDiff.addAllDependencies(fp1Pkg.getDependencies());
            } else {
                final Set<String> fp2Deps = new HashSet<String>(fp2Pkg.getDependencies());
                for(String dep : fp1Pkg.getDependencies()) {
                    if(!fp2Deps.remove(dep)) {
                        fp1PkgDiff.addDependency(dep);
                    }
                }
                if(!fp2Deps.isEmpty()) {
                    fp2PkgDiff.addAllDependencies(fp2Deps);
                }
            }
        }
    }

    private void compareDependencies(final Builder fp1Diff, final Builder fp2Diff) {
        if(!fp1Descr.hasDependencies()) {
            if(fp2Descr.hasDependencies()) {
                fp2Diff.addAllDependencies(fp2Descr.getDependencies());
            }
        } else {
            if(!fp2Descr.hasDependencies()) {
                fp1Diff.addAllDependencies(fp1Descr.getDependencies());
            } else {
                final Set<GAV> fp2Deps = new HashSet<GAV>(fp2Descr.getDependencyGAVs());
                for(GAV gav : fp1Descr.getDependencyGAVs()) {
                    if(!fp2Deps.remove(gav)) {
                        fp1Diff.addDependency(fp1Descr.getDependency(gav));
                    }
                }
                if(!fp2Deps.isEmpty()) {
                    for(GAV gav : fp2Deps) {
                        fp2Diff.addDependency(fp2Descr.getDependency(gav));
                    }
                }
            }
        }
    }
}
