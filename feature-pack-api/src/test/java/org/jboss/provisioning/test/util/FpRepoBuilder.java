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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.provisioning.ArtifactCoords;

/**
 *
 * @author Alexey Loubyansky
 */
public class FpRepoBuilder {

    public static FpRepoBuilder newInstance() {
        return new FpRepoBuilder();
    }

    private Path repoHome;
    private List<FpBuilder> fps = Collections.emptyList();

    private FpRepoBuilder() {
    }

    public FpRepoBuilder setHome(Path p) {
        repoHome = p;
        return this;
    }

    public FpBuilder newFeaturePack() {
        return newFeaturePack(null);
    }

    public FpBuilder newFeaturePack(ArtifactCoords.GavPart gav) {
        final FpBuilder fp = FpBuilder.newInstance(this);
        if(gav != null) {
            fp.setGav(gav);
        }
        addFeaturePack(fp);
        return fp;
    }

    public FpRepoBuilder addFeaturePack(FpBuilder fp) {
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

    public Path write() {
        final Path p;
        if(repoHome == null) {
            p = TestFiles.mkRandomTmpDir();
        } else {
            p = repoHome;
            TestFiles.mkdirs(p);
        }
        for(FpBuilder fp : fps) {
            fp.write(p);
        }
        return p;
    }
}
