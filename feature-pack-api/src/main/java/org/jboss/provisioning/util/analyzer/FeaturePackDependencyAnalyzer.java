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
import org.jboss.provisioning.descr.GroupDescription;
import org.jboss.provisioning.descr.InstallationDescriptionException;
import org.jboss.provisioning.util.FeaturePackLayoutDescriber;
import org.jboss.provisioning.util.HashUtils;
import org.jboss.provisioning.util.analyzer.FeaturePackSpecificDescription.Builder;

/**
 * Analyzes feature pack layouts with the goal to identify dependencies between
 * feature packs.
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDependencyAnalyzer {

    public static FeaturePackDescriptionDiffs compare(Path fpLayoutDir, GAV gav1, GAV gav2) throws InstallationDescriptionException {
        return new FeaturePackDependencyAnalyzer(getFeaturePackDir(fpLayoutDir, gav1), getFeaturePackDir(fpLayoutDir, gav2)).compare();
    }

    private static Path getFeaturePackDir(Path fpLayoutDir, GAV gav) throws InstallationDescriptionException {
        final Path fpPath = fpLayoutDir.resolve(gav.getGroupId()).resolve(gav.getArtifactId()).resolve(gav.getVersion());
        if(!Files.exists(fpPath)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(fpPath));
        }
        return fpPath;
    }

    private static Path getGroupDir(Path fpDir, String groupName) throws InstallationDescriptionException {
        final Path groupDir = fpDir.resolve(Constants.PACKAGES).resolve(groupName);
        if(!Files.exists(groupDir)) {
            throw new InstallationDescriptionException(Errors.pathDoesNotExist(groupDir));
        }
        return groupDir;
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

    public FeaturePackDependencyAnalyzer(Path fp1Dir, Path fp2Dir) throws InstallationDescriptionException {
        this.fp1Dir = fp1Dir;
        this.fp2Dir = fp2Dir;
        fp1Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp1Dir);
        fp2Descr = FeaturePackLayoutDescriber.describeFeaturePack(fp2Dir);
    }

    public FeaturePackDescriptionDiffs compare() throws InstallationDescriptionException {
        final Builder fp1Diff = FeaturePackSpecificDescription.builder(fp1Descr.getGAV());
        final Builder fp2Diff = FeaturePackSpecificDescription.builder(fp2Descr.getGAV());
        compareDependencies(fp1Diff, fp2Diff);
        compareGroups(fp1Diff, fp2Diff);
        return new FeaturePackDescriptionDiffs(fp1Diff.build(), fp2Diff.build());
    }

    private void compareGroups(final Builder fp1Diff, final Builder fp2Diff) throws InstallationDescriptionException {
        if(!fp1Descr.hasGroups()) {
            if(fp2Descr.hasGroups()) {
                fp2Diff.addAllUniqueGroups(fp2Descr.getGroups());
            }
        } else {
            if(!fp2Descr.hasGroups()) {
                fp1Diff.addAllUniqueGroups(fp1Descr.getGroups());
            } else {
                final Set<String> fp2GroupNames = new HashSet<String>(fp2Descr.getGroupNames());
                for(String fp1GroupName : fp1Descr.getGroupNames()) {
                    if(fp2GroupNames.remove(fp1GroupName)) {
                        compareGroups(fp1Descr.getGroupDescription(fp1GroupName),
                                fp2Descr.getGroupDescription(fp1GroupName),
                                fp1Diff, fp2Diff);
                    } else {
                        fp1Diff.addUniqueGroup(fp1Descr.getGroupDescription(fp1GroupName));
                    }
                }
                if(!fp2GroupNames.isEmpty()) {
                    for(String groupName : fp2GroupNames) {
                        fp2Diff.addUniqueGroup(fp2Descr.getGroupDescription(groupName));
                    }
                }
            }
        }
    }

    private void compareGroups(GroupDescription fp1Group, GroupDescription fp2Group, Builder fp1Diff, Builder fp2Diff) throws InstallationDescriptionException {
        final GroupSpecificDescription.Builder g1Diff = GroupSpecificDescription.builder(fp1Group.getName());
        final GroupSpecificDescription.Builder g2Diff = GroupSpecificDescription.builder(fp2Group.getName());

        compareDependencies(fp1Group, fp2Group, g1Diff, g2Diff);

        final Path g1Content = getGroupDir(fp1Dir, fp1Group.getName()).resolve(Constants.CONTENT);
        final Path g2Content = getGroupDir(fp2Dir, fp2Group.getName()).resolve(Constants.CONTENT);

        final boolean g1ContentExists = Files.exists(g1Content);
        final boolean g2ContentExists = Files.exists(g2Content);
        if(g1ContentExists != g2ContentExists) {
            g1Diff.setContentExists(g1ContentExists);
            g2Diff.setContentExists(g2ContentExists);
        } else if(g1ContentExists && g2ContentExists) {
            compareGroupContent(g1Content, g2Content, g1Diff, g2Diff);
        }

        if(g1Diff.hasRecords()) {
            fp1Diff.addConflictingGroup(g1Diff.build());
        }
        if(g2Diff.hasRecords()) {
            fp2Diff.addConflictingGroup(g2Diff.build());
        }
    }

    private void compareGroupContent(Path g1Content, Path g2Content,
            GroupSpecificDescription.Builder g1Diff, GroupSpecificDescription.Builder g2Diff) throws InstallationDescriptionException {

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

    private void compareDirs(Path group1Dir, Path group2Dir, Path c1Dir, Path c2Dir,
            final ContentDiff.Builder c1Builder, final ContentDiff.Builder c2Builder) throws InstallationDescriptionException {
        final Map<String, Path> c2Children = getChildren(c2Dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(c1Dir)) {
            if(c2Children.isEmpty()) {
                for (Path c1 : stream) {
                    c1Builder.addUniquePath(group1Dir.relativize(c1).toString());
                }
            } else {
                for (Path c1 : stream) {
                    final Path c2 = c2Children.remove(c1.getFileName().toString());
                    if (c2 == null) {
                        c1Builder.addUniquePath(group1Dir.relativize(c1).toString());
                    } else {
                        if (Files.isDirectory(c1)) {
                            if (!Files.isDirectory(c2)) {
                                c1Builder.addConflictPath(group1Dir.relativize(c1).toString());
                                c2Builder.addConflictPath(group2Dir.relativize(c2).toString());
                            } else {
                                compareDirs(group1Dir, group2Dir, c1, c2, c1Builder, c2Builder);
                            }
                        } else if (Files.isDirectory(c2)) {
                            c1Builder.addConflictPath(group1Dir.relativize(c1).toString());
                            c2Builder.addConflictPath(group2Dir.relativize(c2).toString());
                        } else if (!Arrays.equals(hashPath(c1), hashPath(c2))) {
                            c1Builder.addConflictPath(group1Dir.relativize(c1).toString());
                            c2Builder.addConflictPath(group2Dir.relativize(c2).toString());
                        }
                    }
                }
                if (!c2Children.isEmpty()) {
                    for (Path c2 : c2Children.values()) {
                        c2Builder.addUniquePath(group2Dir.relativize(c2).toString());
                    }
                }
            }
        } catch (IOException e) {
            throw new InstallationDescriptionException(Errors.readDirectory(group1Dir));
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

    private void compareDependencies(final GroupDescription fp1Group, final GroupDescription fp2Group,
            final GroupSpecificDescription.Builder fp1GroupDiff, final GroupSpecificDescription.Builder fp2GroupDiff) {
        if(!fp1Group.hasDependencies()) {
            if(fp2Group.hasDependencies()) {
                fp2GroupDiff.addAllDependencies(fp2Group.getDependencies());
            }
        } else {
            if(!fp2Group.hasDependencies()) {
                fp1GroupDiff.addAllDependencies(fp1Group.getDependencies());
            } else {
                final Set<String> fp2Deps = new HashSet<String>(fp2Group.getDependencies());
                for(String dep : fp1Group.getDependencies()) {
                    if(!fp2Deps.remove(dep)) {
                        fp1GroupDiff.addDependency(dep);
                    }
                }
                if(!fp2Deps.isEmpty()) {
                    fp2GroupDiff.addAllDependencies(fp2Deps);
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
                final Set<GAV> fp2Deps = new HashSet<GAV>(fp2Descr.getDependencies());
                for(GAV gav : fp1Descr.getDependencies()) {
                    if(!fp2Deps.remove(gav)) {
                        fp1Diff.addDependency(gav);
                    }
                }
                if(!fp2Deps.isEmpty()) {
                    fp2Diff.addAllDependencies(fp2Deps);
                }
            }
        }
    }
}
