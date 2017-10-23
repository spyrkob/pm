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

package org.jboss.provisioning.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.provisioning.config.FeatureConfig;
import org.jboss.provisioning.config.FeatureGroupConfig;


/**
 *
 * @author Alexey Loubyansky
 */
public abstract class ConfigItemContainerBuilder<B extends ConfigItemContainerBuilder<B>> {

    protected List<ConfigItem> items = Collections.emptyList();

    protected ConfigItemContainerBuilder(ConfigItemContainerBuilder<B> src) {
        if(src.items.isEmpty()) {
            items = Collections.emptyList();
        } else if(src.items.size() == 1) {
            ConfigItem item = src.items.get(0);
            if(!item.isGroup()) {
                item = new FeatureConfig((FeatureConfig) item);
            }
            items = Collections.singletonList(item);
        } else {
            final List<ConfigItem> tmp = new ArrayList<>(src.items.size());
            for(ConfigItem item : src.items) {
                if(!item.isGroup()) {
                    item = new FeatureConfig((FeatureConfig) item);
                }
                tmp.add(item);
            }
            items = Collections.unmodifiableList(tmp);
        }
    }

    protected ConfigItemContainerBuilder() {}

    @SuppressWarnings("unchecked")
    public B addConfigItem(ConfigItem item) {
        switch (items.size()) {
            case 0:
                items = Collections.singletonList(item);
                break;
            case 1:
                items = new ArrayList<>(items);
            default:
                items.add(item);
        }
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addFeatureGroup(FeatureGroupConfig dep) {
        addConfigItem(dep);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B addFeature(FeatureConfig feature) {
        addConfigItem(feature);
        return (B) this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigItemContainerBuilder<?> other = (ConfigItemContainerBuilder<?>) obj;
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        return true;
    }
}
