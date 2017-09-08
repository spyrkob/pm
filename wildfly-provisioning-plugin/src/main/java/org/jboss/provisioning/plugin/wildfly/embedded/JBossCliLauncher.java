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
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import org.jboss.modules.LocalModuleFinder;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

import __redirected.__JAXPRedirected;

/**
 * @author Alexey Loubyansky
 *
 */
public class JBossCliLauncher {

    private static final String JAVAX_XML_STREAM_XML_OUTPUT_FACTORY = "javax.xml.stream.XMLOutputFactory";
    private static final Class<?>[] CTOR_TYPES = new Class[] {Path.class, boolean.class, boolean.class};
    private static final String MODULE_NAME = "org.jboss.as.cli";

    private URLClassLoader newCl;
    private Method taskMethod;
    private Object task;

    public void launch(File[] repoRoots, Object[] ctorArgs, Object[] methodArgs) throws Exception {

        if(task == null) {
            init(repoRoots, ctorArgs);
        }

        final URLClassLoader originalCl = (URLClassLoader) getClass().getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(newCl);
            taskMethod.invoke(task, methodArgs);
        } catch (Exception e) {
            throw new Exception("Failed to execute JBoss Modules task", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    private void init(File[] repoRoots, Object[] ctorArgs) throws Exception {
        final ModuleFinder mf = new LocalModuleFinder(repoRoots);
        final ModuleLoader ml = new ModuleLoader(new ModuleFinder[] {mf});
        final String origXmlOutFactory = System.getProperty(JAVAX_XML_STREAM_XML_OUTPUT_FACTORY);
        __JAXPRedirected.initAll();
        Module cliModule;
        try {
            cliModule = ml.loadModule(MODULE_NAME);
            final URLClassLoader originalCl = (URLClassLoader) getClass().getClassLoader();
            newCl = new URLClassLoader(originalCl.getURLs(), cliModule.getClassLoader());
            final Class<?> taskClass = newCl.loadClass(JBossCli.class.getName());
            taskMethod = taskClass.getMethod("execute", new Class[] {List.class});
            task = taskClass.getConstructor(CTOR_TYPES).newInstance(ctorArgs);
        } catch (ModuleLoadException e) {
            throw new Exception("Failed to load CLI module", e);
        } catch (ClassNotFoundException e) {
            throw new Exception("Failed to load " + JBossCli.class, e);
        } catch (Exception e) {
            throw new Exception("Failed to initialize an instance of " + JBossCli.class);
        } finally {
            __JAXPRedirected.restorePlatformFactory();
            if(origXmlOutFactory == null) {
                if(System.getProperty(JAVAX_XML_STREAM_XML_OUTPUT_FACTORY) != null) {
                    System.clearProperty(JAVAX_XML_STREAM_XML_OUTPUT_FACTORY);
                }
            } else if(!origXmlOutFactory.equals(System.getProperty(JAVAX_XML_STREAM_XML_OUTPUT_FACTORY))) {
                System.setProperty(JAVAX_XML_STREAM_XML_OUTPUT_FACTORY, origXmlOutFactory);
            }
        }
    }
}
