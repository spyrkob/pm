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

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackDescriptionDiffs {

    private final FeaturePackSpecificDescription fp1;
    private final FeaturePackSpecificDescription fp2;

    FeaturePackDescriptionDiffs(FeaturePackSpecificDescription fp1, FeaturePackSpecificDescription fp2) {
        this.fp1 = fp1;
        this.fp2 = fp2;
    }

    public FeaturePackSpecificDescription getFeaturePackDiff1() {
        return fp1;
    }

    public FeaturePackSpecificDescription getFeaturePackDiff2() {
        return fp2;
    }
}
