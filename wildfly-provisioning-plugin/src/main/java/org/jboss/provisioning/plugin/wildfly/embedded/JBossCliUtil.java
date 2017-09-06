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

package org.jboss.provisioning.plugin.wildfly.embedded;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jboss.provisioning.Errors;
import org.jboss.provisioning.ProvisioningException;

/**
 * @author Alexey Loubyansky
 *
 */
public class JBossCliUtil {

    private static Method launchMethod;
    private static Object launcher;

    public static void runCliScript(Path installDir, boolean silent, boolean echoCommand, List<String> cmds) throws ProvisioningException {
        initLauncher(installDir);
        try {
            launchMethod.invoke(launcher,
                    new File[] { installDir.resolve("modules").resolve("system").resolve("layers").resolve("base").toFile() },
                    new Object[] {installDir.resolve("jboss-cli.xml"), silent, echoCommand}, new Object[] {cmds});
        } catch (Exception e) {
            throw new ProvisioningException("Failed to execute a CLI task", e);
        }
    }

    private static void initLauncher(Path installDir) throws ProvisioningException {
        if (launcher == null) {
            final Path jbossModulesJar = installDir.resolve("jboss-modules.jar");
            if (!Files.exists(jbossModulesJar)) {
                throw new ProvisioningException(Errors.pathDoesNotExist(jbossModulesJar));
            }

            final URL[] pluginURLs = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();
            final URL[] newURLs = new URL[pluginURLs.length + 1];
            try {
                newURLs[0] = jbossModulesJar.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new ProvisioningException("Failed to transform " + jbossModulesJar + " to URL", e);
            }
            System.arraycopy(pluginURLs, 0, newURLs, 1, pluginURLs.length);

            URLClassLoader cl = null;
            try {
                cl = new URLClassLoader(newURLs, null);
                final Class<?> cls = cl.loadClass("org.jboss.provisioning.plugin.wildfly.embedded.JBossCliLauncher");
                launchMethod = cls.getMethod("launch", File[].class, Object[].class, Object[].class);
                launcher = cls.newInstance();
            } catch (Exception e) {
                if (cl != null) {
                    try {
                        cl.close();
                    } catch (IOException e1) {
                    }
                }
                throw new ProvisioningException("Failed to init JBoss Modules environment", e);
            }
        }
    }
}
