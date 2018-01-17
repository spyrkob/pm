/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
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

package org.jboss.provisioning.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningDescriptionException;
import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ArtifactCoords.Gav;
import org.jboss.provisioning.config.FeaturePackConfig;
import org.jboss.provisioning.config.FeaturePackDepsConfig;
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public class FpVersionsResolver {

    static void resolveFpVersions(ProvisioningRuntimeBuilder rt) throws ProvisioningException {
        new FpVersionsResolver(rt).assertVersions();
    }

    private final ProvisioningRuntimeBuilder rt;
    private Set<ArtifactCoords.Ga> missingVersions = Collections.emptySet();
    private List<ArtifactCoords.Ga> branch = new ArrayList<>();
    private Map<ArtifactCoords.Ga, Set<ArtifactCoords.Gav>> conflicts = Collections.emptyMap();

    private FpVersionsResolver(ProvisioningRuntimeBuilder rt) {
        this.rt = rt;
    }

    public boolean hasMissingVersions() {
        return !missingVersions.isEmpty();
    }

    public Set<ArtifactCoords.Ga> getMissingVersions() {
        return missingVersions;
    }

    public boolean hasVersionConflicts() {
        return !conflicts.isEmpty();
    }

    public Map<ArtifactCoords.Ga, Set<ArtifactCoords.Gav>> getVersionConflicts() {
        return conflicts;
    }

    private void assertVersions() throws ProvisioningException {
        assertVersions(rt.config);
        if(!missingVersions.isEmpty() || !conflicts.isEmpty()) {
            throw new ProvisioningDescriptionException(Errors.fpVersionCheckFailed(missingVersions, conflicts.values()));
        }
    }

    private void assertVersions(FeaturePackDepsConfig fpDepsConfig) throws ProvisioningException {
        if(!fpDepsConfig.hasFeaturePackDeps()) {
            return;
        }
        final int branchSize = branch.size();
        final Collection<FeaturePackConfig> fpDeps = fpDepsConfig.getFeaturePackDeps();
        Set<ArtifactCoords.Gav> skip = Collections.emptySet();
        for(FeaturePackConfig fpConfig : fpDeps) {
            final Gav gav = fpConfig.getGav();
            if(gav.getVersion() == null) {
                missingVersions = PmCollections.addLinked(missingVersions, gav.toGa());
                continue;
            }
            final FeaturePackRuntime.Builder fp = rt.getFpBuilder(gav, false);
            if(fp != null) {
                if(!fp.gav.equals(gav) && !branch.contains(gav.toGa())) {
                    Set<Gav> versions = conflicts.get(fp.gav.toGa());
                    if(versions != null) {
                        versions.add(gav);
                        continue;
                    }
                    versions = new LinkedHashSet<ArtifactCoords.Gav>();
                    versions.add(fp.gav);
                    versions.add(gav);
                    conflicts = PmCollections.putLinked(conflicts, gav.toGa(), versions);
                }
                skip = PmCollections.add(skip, fp.gav);
                continue;
            }
            rt.loadFpBuilder(gav);
            if(!missingVersions.isEmpty()) {
                missingVersions = PmCollections.remove(missingVersions, gav.toGa());
            }
            branch.add(gav.toGa());
        }
        for(FeaturePackConfig fpConfig : fpDeps) {
            final Gav gav = fpConfig.getGav();
            if(gav.getVersion() == null || skip.contains(gav)) {
                continue;
            }
            assertVersions(rt.getFpBuilder(gav, true).spec);
        }
        for(int i = 0; i < branch.size() - branchSize; ++i) {
            branch.remove(branch.size() - 1);
        }
    }
}
