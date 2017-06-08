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
package org.jboss.provisioning.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.plugin.FpMavenErrors;

/**
 *
 * @author Alexey Loubyansky
 */
class ArtifactResolverImpl implements ArtifactResolver {

    private static final ArtifactResolver INSTANCE = new ArtifactResolverImpl();

    static ArtifactResolver getInstance() {
        return INSTANCE;
    }

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession session;

    private ArtifactResolverImpl() {
        repoSystem = Util.newRepositorySystem();
        session = Util.newRepositorySession(repoSystem);
    }

    @Override
    public Path resolve(ArtifactCoords coords) throws ArtifactResolutionException {
        final ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(),
                coords.getExtension(), coords.getVersion()));
        final ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(session, request);
        } catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
            throw new ArtifactResolutionException(FpMavenErrors.artifactResolution(coords), e);
        }
        if (!result.isResolved()) {
            throw new ArtifactResolutionException(FpMavenErrors.artifactResolution(coords));
        }
        if (result.isMissing()) {
            throw new ArtifactResolutionException(FpMavenErrors.artifactMissing(coords));
        }
        return Paths.get(result.getArtifact().getFile().toURI());
    }

}
