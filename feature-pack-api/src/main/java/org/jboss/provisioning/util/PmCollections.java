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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class PmCollections {

    public static <K,V> Map<K,V> map(Map<K,V> m) {
        return m.size() > 1 ? Collections.unmodifiableMap(m) : m;
    }

    public static <K,V> Map<K,V> put(Map<K,V> m, K k, V v) {
        if(m.isEmpty()) {
            return Collections.singletonMap(k, v);
        }
        if(m.size() == 1) {
            if(m.containsKey(k)) {
                return Collections.singletonMap(k, v);
            }
            final Map.Entry<K, V> first = m.entrySet().iterator().next();
            m = new HashMap<>(2);
            m.put(first.getKey(), first.getValue());
        }
        m.put(k, v);
        return m;
    }

    public static <K,V> Map<K,V> putLinked(Map<K,V> m, K k, V v) {
        if(m.isEmpty()) {
            return Collections.singletonMap(k, v);
        }
        if(m.size() == 1) {
            if(m.containsKey(k)) {
                return Collections.singletonMap(k, v);
            }
            final Map.Entry<K, V> first = m.entrySet().iterator().next();
            m = new LinkedHashMap<>(2);
            m.put(first.getKey(), first.getValue());
        }
        m.put(k, v);
        return m;
    }

    public static <T> List<T> list(List<T> l) {
        return l.size() > 1 ? Collections.unmodifiableList(l) : l;
    }

    public static <T> Set<T> set(Set<T> s) {
        return s.size() > 1 ? Collections.unmodifiableSet(s) : s;
    }

    public static <T> Set<T> add(Set<T> s, T t) {
        if(s.isEmpty()) {
            return Collections.singleton(t);
        }
        if(s.size() == 1) {
            if (s.contains(t)) {
                return s;
            }
            final T first = s.iterator().next();
            s = new HashSet<>(2);
            s.add(first);
        }
        s.add(t);
        return s;
    }

    public static <T> Set<T> addLinked(Set<T> s, T t) {
        if(s.isEmpty()) {
            return Collections.singleton(t);
        }
        if(s.size() == 1) {
            if (s.contains(t)) {
                return s;
            }
            final T first = s.iterator().next();
            s = new LinkedHashSet<>(2);
            s.add(first);
        }
        s.add(t);
        return s;
    }
}
