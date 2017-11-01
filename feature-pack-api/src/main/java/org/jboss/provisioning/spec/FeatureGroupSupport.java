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
import org.jboss.provisioning.util.PmCollections;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class FeatureGroupSupport extends PackageDepsSpec implements ConfigItemContainer {

    abstract static class Builder<T extends FeatureGroupSupport, B extends Builder<T, B>> extends PackageDepsSpecBuilder<B> implements ConfigItemContainerBuilder<B> {

        String name;
        protected List<ConfigItem> items = Collections.emptyList();
        boolean resetFeaturePackOrigin;

        protected Builder() {
        }

        protected Builder(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public B setResetFeaturePackOrigin(boolean resetOrigin) {
            this.resetFeaturePackOrigin = resetOrigin;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B setName(String name) {
            this.name = name;
            return (B) this;
        }

        @Override
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((items == null) ? 0 : items.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + (resetFeaturePackOrigin ? 1231 : 1237);
            return result;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Builder other = (Builder) obj;
            if (items == null) {
                if (other.items != null)
                    return false;
            } else if (!items.equals(other.items))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (resetFeaturePackOrigin != other.resetFeaturePackOrigin)
                return false;
            return true;
        }

        public abstract T build();
    }

    protected final String name;
    protected final boolean resetFeaturePackOrigin;

    protected final List<ConfigItem> items;

    protected FeatureGroupSupport(FeatureGroupSupport copy) {
        super(copy);
        name = copy.name;
        resetFeaturePackOrigin = copy.resetFeaturePackOrigin;

        if(copy.items.isEmpty()) {
            items = Collections.emptyList();
        } else if(copy.items.size() == 1) {
            ConfigItem item = copy.items.get(0);
            if(!item.isGroup()) {
                item = new FeatureConfig((FeatureConfig) item);
            }
            items = Collections.singletonList(item);
        } else {
            final List<ConfigItem> tmp = new ArrayList<>(copy.items.size());
            for(ConfigItem item : copy.items) {
                if(!item.isGroup()) {
                    item = new FeatureConfig((FeatureConfig) item);
                }
                tmp.add(item);
            }
            items = Collections.unmodifiableList(tmp);
        }
    }

    protected FeatureGroupSupport(Builder<?, ?> builder) {
        super(builder);
        name = builder.name;
        resetFeaturePackOrigin = builder.resetFeaturePackOrigin;
        this.items = PmCollections.list(builder.items);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean hasItems() {
        return !items.isEmpty();
    }

    @Override
    public List<ConfigItem> getItems() {
        return items;
    }

    @Override
    public boolean isResetFeaturePackOrigin() {
        return resetFeaturePackOrigin;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (resetFeaturePackOrigin ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureGroupSupport other = (FeatureGroupSupport) obj;
        if (items == null) {
            if (other.items != null)
                return false;
        } else if (!items.equals(other.items))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (resetFeaturePackOrigin != other.resetFeaturePackOrigin)
            return false;
        return true;
    }
}