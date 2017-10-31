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

package org.jboss.provisioning.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class Unmodifiable {

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> map(Map<? extends K, ? extends V> m) {
        return m.size() > 1 ? Collections.unmodifiableMap(m) : (Map<K, V>) m;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> list(List<? extends T> l) {
        return l.size() > 1 ? Collections.unmodifiableList(l) : (List<T>) l;
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> set(Set<? extends T> s) {
        return s.size() > 1 ? Collections.unmodifiableSet(s) : (Set<T>) s;
    }
}
