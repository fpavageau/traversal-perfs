/*
 * Copyright 2016 Frank Pavageau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ekino.neo4j.traversal;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.neo4j.graphdb.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade for Neo4j operations on properties, to allow caching.
 */
enum Neo4jPropertyOperations {
    DEFAULT,
    CACHE {
        private final Long2ObjectMap<Map<String, Object>> propertyCache = new Long2ObjectOpenHashMap<>();

        {
            propertyCache.defaultReturnValue(Collections.<String, Object>emptyMap());
        }

        @Override
        public synchronized Object getProperty(Node node, String property) {
            Map<String, Object> properties = propertyCache.get(node.getId());
            Object value = properties.get(property);
            if (value == null) {
                // Absent values won't be cached, Neo4j will always be called for those (and throw an exception). This
                // could be handled.
                value = node.getProperty(property);
                cacheProperty(node, property, value, properties);
            }
            return value;
        }

        private void cacheProperty(Node node, String property, Object value, Map<String, Object> properties) {
            Map<String, Object> newProperties;
            if (properties.isEmpty()) {
                // Optimize the memory usage when a single property is cached
                newProperties = Collections.singletonMap(property, value);
            } else {
                if (properties.size() == 1) {
                    newProperties = new HashMap<>(properties);
                } else {
                    newProperties = properties;
                }
                newProperties.put(property, value);
            }
            if (newProperties != properties) {
                propertyCache.put(node.getId(), newProperties);
            }
        }

        @Override
        protected synchronized void clear() {
            propertyCache.clear();
        }

        @Override
        public String toString() {
            return "CACHE";
        }
    };

    public static Neo4jPropertyOperations get(boolean cache) {
        if (cache) {
            return CACHE;
        }
        return DEFAULT;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public Object getProperty(Node node, String property) {
        return node.getProperty(property);
    }

    protected void clear() {
    }
}
