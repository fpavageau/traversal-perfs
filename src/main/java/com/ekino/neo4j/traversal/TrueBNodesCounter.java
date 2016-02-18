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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

class TrueBNodesCounter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrueBNodesCounter.class);

    private final GraphDatabaseService graphDb;

    public TrueBNodesCounter(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public int count() {
        try (ResourceIterator<Node> roots = graphDb.findNodes(Labels.Root)) {
            if (roots.hasNext()) {
                return count(roots.next());
            }
        }
        return -1;
    }

    private int count(Node root) {
        LOGGER.info("Traversing the whole tree");
        TraversalDescription td = graphDb.traversalDescription()
                .uniqueness(Uniqueness.NONE)
                .breadthFirst()
                .evaluator(TrueBEvaluator.INSTANCE)
                .expand(CustomPathExpander.INSTANCE);

        int count = 0;
        for (Path ignored : td.traverse(root)) {
            count++;
        }
        return count;
    }

    private enum TrueBEvaluator implements Evaluator {
        INSTANCE;

        @Override
        public Evaluation evaluate(Path path) {
            Node endNode = path.endNode();
            return Evaluation.ofIncludes(includes(endNode));
        }

        private static boolean includes(Node endNode) {
            return endNode.hasLabel(Labels.B) && (Boolean) endNode.getProperty("value");
        }

        @Override
        public String toString() {
            return "TrueBEvaluator()";
        }
    }

    private enum CustomPathExpander implements PathExpander<Object> {
        INSTANCE;

        @Override
        public Iterable<Relationship> expand(Path path, BranchState<Object> state) {
            Node endNode = path.endNode();
            if (endNode.hasLabel(Labels.A)) {
                return endNode.getRelationships(Direction.OUTGOING, RelationshipTypes.HAS_B);
            } else if (endNode.hasLabel(Labels.B)) {
                return endNode.getRelationships(Direction.OUTGOING, RelationshipTypes.HAS_A);
            }
            return Collections.emptyList();
        }

        @Override
        public PathExpander<Object> reverse() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "CustomPathExpander()";
        }
    }
}
