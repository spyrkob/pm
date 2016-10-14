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

package org.jboss.provisioning.test.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.ArtifactCoords;
import org.jboss.provisioning.ArtifactResolutionException;
import org.jboss.provisioning.ArtifactResolver;
import org.jboss.provisioning.Errors;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackRepoManager implements ArtifactResolver {

    public static FeaturePackRepoManager newInstance(Path repoHome) {
        return new FeaturePackRepoManager(repoHome);
    }

    public class Installer {

        private List<FeaturePackBuilder> fps = Collections.emptyList();

        private Installer() {
        }

        public FeaturePackBuilder newFeaturePack() {
            return newFeaturePack(null);
        }

        public FeaturePackBuilder newFeaturePack(ArtifactCoords.Gav gav) {
            final FeaturePackBuilder fp = FeaturePackBuilder.newInstance(this);
            if(gav != null) {
                fp.setGav(gav);
            }
            addFeaturePack(fp);
            return fp;
        }

        public Installer addFeaturePack(FeaturePackBuilder fp) {
            switch(fps.size()) {
                case 0:
                    fps = Collections.singletonList(fp);
                    break;
                case 1:
                    fps = new ArrayList<>(fps);
                default:
                    fps.add(fp);
            }
            return this;
        }

        public Path install() {
            final Path p;
            if(repoHome == null) {
                p = TestFiles.mkRandomTmpDir();
            } else {
                p = repoHome;
                TestFiles.mkdirs(p);
            }
            for(FeaturePackBuilder fp : fps) {
                fp.build(p);
            }
            return p;
        }
    }

    private final Path repoHome;

    private FeaturePackRepoManager(Path repoHome) {
        this.repoHome = repoHome;
    }

    public Installer installer() {
        return new Installer();
    }

    @Override
    public Path resolve(ArtifactCoords coords) throws ArtifactResolutionException {
        final Path path = FeaturePackBuilder.getFeaturePackArtifactPath(repoHome, coords.toGav());
        if(!Files.exists(path)) {
            throw new ArtifactResolutionException(Errors.unknownFeaturePack(coords.toGav()));
        }
        return path;
    }
}
