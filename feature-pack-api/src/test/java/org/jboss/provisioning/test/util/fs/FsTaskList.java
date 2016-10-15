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

package org.jboss.provisioning.test.util.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class FsTaskList implements FsTask {

    public static FsTaskList newList() {
        return new FsTaskList();
    }

    private List<FsTask> tasks = Collections.emptyList();

    private FsTaskList() {
    }

    public FsTaskList write(String content, String relativeTarget) {
        return add(new StringToFile(content, relativeTarget));
    }

    public FsTaskList copy(Path src, String relativeTarget) {
        return add(new PathCopy(src, relativeTarget));
    }

    public FsTaskList copyDir(Path src, String relativeTarget, boolean contentOnly) {
        return add(new DirCopy(src, relativeTarget, contentOnly));
    }

    public FsTaskList add(FsTask task) {
        switch(tasks.size()) {
            case 0:
                tasks = Collections.singletonList(task);
                break;
            case 1:
                tasks = new ArrayList<>(tasks);
            default:
                tasks.add(task);
        }
        return this;
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    @Override
    public void execute(FsTaskContext ctx) throws IOException {
        for(FsTask task : tasks) {
            task.execute(ctx);
        }
    }
}
