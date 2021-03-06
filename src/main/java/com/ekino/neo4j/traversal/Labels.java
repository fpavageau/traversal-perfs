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

import java.util.HashMap;
import java.util.Map;

enum Labels implements Label {
    Root,
    A,
    B;

    private static final Map<String, Labels> byNameMap = new HashMap<>();

    static {
        for (Labels label : Labels.values()) {
            byNameMap.put(label.name(), label);
        }
    }

    public static Labels getByName(String name) {
        return byNameMap.get(name);
    }
}
