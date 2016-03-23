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

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

/**
 * Facade for Neo4j operations on labels, to allow caching.
 */
enum Neo4jLabelOperations {
    DEFAULT,
    CACHE {
        private final Long2ByteMap labelCache = new Long2ByteOpenHashMap();

        {
            labelCache.defaultReturnValue((byte) -1);
        }

        @Override
        public boolean hasLabel(Node node, Label label) {
            if (label instanceof Labels) {
                return hasCacheableLabel(node, ((Labels) label));
            }
            return node.hasLabel(label);
        }

        private synchronized boolean hasCacheableLabel(Node node, Labels label) {
            byte val = labelCache.get(node.getId());
            boolean hasLabel;
            if (val < 0) {
                hasLabel = checkLabelAndCache(node, label);
            } else {
                hasLabel = (val & getLabelMask(label)) != 0;
            }
            return hasLabel;
        }

        private boolean checkLabelAndCache(Node node, Labels label) {
            byte val = 0;
            boolean hasLabel = false;
            for (Label nodeLabel : node.getLabels()) {
                Labels appLabel = Labels.getByName(nodeLabel.name());
                // We're only interested in caching the labels in the enum.
                if (appLabel != null) {
                    val |= getLabelMask(appLabel);
                    hasLabel |= appLabel == label;
                }
            }
            labelCache.put(node.getId(), val);
            return hasLabel;
        }

        private byte getLabelMask(Labels label) {
            return (byte) (1 << label.ordinal());
        }

        @Override
        protected synchronized void clear() {
            labelCache.clear();
        }

        @Override
        public String toString() {
            return "CACHE";
        }
    };

    public static Neo4jLabelOperations get(boolean cache) {
        if (cache) {
            return CACHE;
        }
        return DEFAULT;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public boolean hasLabel(Node node, Label label) {
        return node.hasLabel(label);
    }

    protected void clear() {
    }
}
