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

package org.jboss.provisioning.test;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.provisioning.ProvisioningException;
import org.jboss.provisioning.ProvisioningManager;
import org.jboss.provisioning.descr.ProvisionedInstallationDescription;
import org.jboss.provisioning.test.util.FeaturePackRepoManager;
import org.jboss.provisioning.test.util.TestUtils;
import org.jboss.provisioning.util.IoUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author Alexey Loubyansky
 */
public class FeaturePackRepoTestBase {

    private static final String PATH_DOES_NOT_EXIST = "path does not exist";
    private static final String PATH_EXISTS = "path exists";
    private static final String PATH_IS_DIR = "path is a directory";
    private static final String PATH_IS_NOT_DIR = "path is not a directory";

    private static Path repoHome;

    @BeforeClass
    public static void beforeClass() throws Exception {
        repoHome = TestUtils.mkRandomTmpDir();
        doBeforeClass();
    }

    protected static void doBeforeClass() throws Exception {
    }

    @AfterClass
    public static void afterClass() throws Exception {
        doAfterClass();
        IoUtils.recursiveDelete(repoHome);
    }

    protected static void doAfterClass() throws Exception {
    }

    protected static FeaturePackRepoManager getRepoManager() {
        return FeaturePackRepoManager.newInstance(repoHome);
    }

    private Path installHome;

    @Before
    public void before() throws Exception {
        installHome = TestUtils.mkRandomTmpDir();
        doBefore();
    }

    protected void doBefore() throws Exception {
    }

    @After
    public void after() throws Exception {
        doAfter();
        IoUtils.recursiveDelete(installHome);
    }

    protected void doAfter() throws Exception {
    }

    protected ProvisioningManager getPm() {
        return ProvisioningManager.builder().setArtifactResolver(getRepoManager()).setInstallationHome(installHome).build();
    }

    protected Path resolve(String relativePath) {
        return installHome.resolve(relativePath);
    }

    protected void assertSpec(ProvisionedInstallationDescription spec) throws ProvisioningException {
        assertSpec(getPm(), spec);
    }

    protected static void assertSpec(ProvisioningManager pm, ProvisionedInstallationDescription spec)
            throws ProvisioningException {
        assertSpec(pm, spec, false);
    }

    protected static void assertSpec(ProvisioningManager pm, ProvisionedInstallationDescription spec,
            boolean includeDependencies) throws ProvisioningException {
        Assert.assertEquals(spec, pm.getCurrentState(includeDependencies));
    }

    protected void assertDoesNotExist(String relativePath) {
        final Path p = resolve(relativePath);
        Assert.assertFalse(PATH_DOES_NOT_EXIST, Files.exists(p));
    }

    protected void assertContent(String content, String relativePath) {
        final Path path = resolve(relativePath);
        Assert.assertTrue(PATH_EXISTS, Files.exists(path));
        Assert.assertTrue(PATH_IS_NOT_DIR, !Files.isDirectory(path));
        Assert.assertEquals(content, TestUtils.read(path));
    }
}