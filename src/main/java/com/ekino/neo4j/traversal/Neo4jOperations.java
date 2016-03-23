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

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

/**
 * Facade for some Neo4j operations, to allow caching.
 */
class Neo4jOperations {
    private static final Neo4jOperations DEFAULT = new Neo4jOperations(
            Neo4jLabelOperations.get(false),
            Neo4jPropertyOperations.get(false));

    private final Neo4jLabelOperations labelOperations;
    private final Neo4jPropertyOperations propertyOperations;

    private Neo4jOperations(Neo4jLabelOperations labelOperations, Neo4jPropertyOperations propertyOperations) {
        this.labelOperations = labelOperations;
        this.propertyOperations = propertyOperations;
    }

    public static Neo4jOperations get(String cache) {
        if (cache == null || cache.isEmpty()) {
            return DEFAULT;
        }
        return new Neo4jOperations(
                Neo4jLabelOperations.get(cache.contains("label")),
                Neo4jPropertyOperations.get(cache.contains("property")));
    }

    public static void clearCache() {
        Neo4jLabelOperations.clearCache();
        Neo4jPropertyOperations.clearCache();
    }

    public boolean hasLabel(Node node, Label label) {
        return labelOperations.hasLabel(node, label);
    }

    public Object getProperty(Node node, String property) {
        return propertyOperations.getProperty(node, property);
    }

    @Override
    public String toString() {
        return "Neo4jOperations(" + labelOperations + ", " + propertyOperations + ")";
    }
}
