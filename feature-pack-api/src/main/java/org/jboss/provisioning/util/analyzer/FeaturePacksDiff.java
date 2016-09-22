/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.Gav;
import org.jboss.provisioning.descr.FeaturePackDescription;
import org.jboss.provisioning.descr.PackageDescription;
import org.jboss.provisioning.descr.ProvisioningDescriptionException;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.HashUtils;
import org.jboss.provisioning.util.LayoutUtils;
import org.jboss.provisioning.util.analyzer.FeaturePackSpecificDescription.Builder;

/**
 * Analyzes feature pack layouts with the goal to identify common dependencies,
 * content and differences between feature packs.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePacksDiff {

    static FeaturePacksDiff newInstance(Path fpLayoutDir, String encoding, Gav gav1, Gav gav2) throws ProvisioningDescriptionException {
        return new FeaturePacksDiff(fpLayoutDir, encoding, gav1, gav2);
    }

    public static FeaturePackDescriptionDiffs compare(Path fpLayoutDir, String encoding, Gav gav1, Gav gav2) throws ProvisioningDescriptionException {
        return newInstance(fpLayoutDir, encoding, gav1, gav2).compare();
    }

    private static byte[] hashPath(Path path) throws ProvisioningDescriptionException {
        try {
            return HashUtils.hashPath(path);
        } catch (IOException e) {
            throw new ProvisioningDescriptionException(Errors.hashCalculation(path), e);
        }
    }

    private final Path fpLayoutDir;
    private final String encoding;
    private final FeaturePackDescription fp1Descr;
    private final FeaturePackDescription fp2Descr;

    FeaturePacksDiff(Path fpLayoutDir, String encoding, Gav gav1, Gav gav2) throws ProvisioningDescriptionException {
        this.fpLayoutDir = fpLayoutDir;
        this.encoding = encoding;
        fp1Descr = FeaturePackLayoutDescriber.describeFeaturePack(LayoutUtils.getFeaturePackDir(fpLayoutDir, gav1), encoding);
        fp2Descr = FeaturePackLayoutDescriber.describeFeaturePack(LayoutUtils.getFeaturePackDir(fpLayoutDir, gav2), encoding);
    }

    FeaturePackDescription getFeaturePackDescription1() {
        return fp1Descr;
    }

    FeaturePackDescription getFeaturePackDescription2() {
        return fp2Descr;
    }

    FeaturePackDescriptionDiffs compare() throws ProvisioningDescriptionException {
        final Builder fp1Diff = FeaturePackSpecificDescription.builder(fp1Descr.getGav());
        final Builder fp2Diff = FeaturePackSpecificDescription.builder(fp2Descr.getGav());
        compareDependencies(fp1Diff, fp2Diff);
        comparePackages(fp1Diff, fp2Diff);
        return new FeaturePackDescriptionDiffs(fp1Diff.build(), fp2Diff.build());
    }

    private void comparePackages(final Builder fp1Diff, final Builder fp2Diff) throws ProvisioningDescriptionException {

        final Map<String, FeaturePackPackageView.ResolvedPackage> fp1Packages = FeaturePackPackageView.resolve(fpLayoutDir, encoding, fp1Descr);
        final Map<String, FeaturePackPackageView.ResolvedPackage> fp2Packages = FeaturePackPackageView.resolve(fpLayoutDir, encoding, fp2Descr);

        if(fp1Packages.isEmpty()) {
            if(!fp2Packages.isEmpty()) {
                for(FeaturePackPackageView.ResolvedPackage resolvedPackage : fp2Packages.values()) {
                    fp2Diff.addUniquePackage(resolvedPackage.getDescription());
                }
            }
        } else {
            if(fp2Packages.isEmpty()) {
                for(FeaturePackPackageView.ResolvedPackage resolvedPackage : fp1Packages.values()) {
                    fp1Diff.addUniquePackage(resolvedPackage.getDescription());
                }
            } else {
                final Set<String> fp2PkgNames = new HashSet<String>(fp2Packages.keySet());
                for(String fp1PkgName : fp1Packages.keySet()) {
                    if(fp2PkgNames.remove(fp1PkgName)) {
                        comparePackages(fp1Packages.get(fp1PkgName), fp2Packages.get(fp1PkgName), fp1Diff, fp2Diff);
                    } else {
                        fp1Diff.addUniquePackage(fp1Packages.get(fp1PkgName).getDescription());
                    }
                }
                if(!fp2PkgNames.isEmpty()) {
                    for(String pkgName : fp2PkgNames) {
                        fp2Diff.addUniquePackage(fp2Packages.get(pkgName).getDescription());
                    }
                }
            }
        }
    }

    private void comparePackages(FeaturePackPackageView.ResolvedPackage fp1Pkg, FeaturePackPackageView.ResolvedPackage fp2Pkg,
            Builder fp1Diff, Builder fp2Diff) throws ProvisioningDescriptionException {
        final PackageSpecificDescription.Builder g1Diff = PackageSpecificDescription.builder(fp1Pkg.getName());
        final PackageSpecificDescription.Builder g2Diff = PackageSpecificDescription.builder(fp2Pkg.getName());

        compareDependencies(fp1Pkg.getDescription(), fp2Pkg.getDescription(), g1Diff, g2Diff);

        final Path g1Content = LayoutUtils.getPackageContentDir(LayoutUtils.getFeaturePackDir(fpLayoutDir, fp1Pkg.getGAV()), fp1Pkg.getName());
        final Path g2Content = LayoutUtils.getPackageContentDir(LayoutUtils.getFeaturePackDir(fpLayoutDir, fp2Pkg.getGAV()), fp2Pkg.getName());

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
            fp1Diff.addMatchedPackage(fp1Pkg.getDescription());
            fp2Diff.addMatchedPackage(fp2Pkg.getDescription());
        }
    }

    private void comparePackageContent(Path g1Content, Path g2Content,
            PackageSpecificDescription.Builder g1Diff, PackageSpecificDescription.Builder g2Diff) throws ProvisioningDescriptionException {

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
            final ContentDiff.Builder c1Builder, final ContentDiff.Builder c2Builder) throws ProvisioningDescriptionException {
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
            throw new ProvisioningDescriptionException(Errors.readDirectory(pkg1Dir));
        }
    }

    private static Map<String, Path> getChildren(Path p) throws ProvisioningDescriptionException {
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
            throw new ProvisioningDescriptionException(Errors.readDirectory(p));
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
                final Set<Gav> fp2Deps = new HashSet<Gav>(fp2Descr.getDependencyGAVs());
                for(Gav gav : fp1Descr.getDependencyGAVs()) {
                    if(!fp2Deps.remove(gav)) {
                        fp1Diff.addDependency(fp1Descr.getDependency(gav));
                    }
                }
                if(!fp2Deps.isEmpty()) {
                    for(Gav gav : fp2Deps) {
                        fp2Diff.addDependency(fp2Descr.getDependency(gav));
                    }
                }
            }
        }
    }
}
