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
    private final Neo4jOperations neo4jOperations;

    public TrueBNodesCounter(GraphDatabaseService graphDb, Neo4jOperations neo4jOperations) {
        this.graphDb = graphDb;
        this.neo4jOperations = neo4jOperations;
    }

    public int count(boolean depthFirst) {
        try (ResourceIterator<Node> roots = graphDb.findNodes(Labels.Root)) {
            if (roots.hasNext()) {
                return count(roots.next(), depthFirst);
            }
        }
        return -1;
    }

    private int count(Node root, boolean depthFirst) {
        LOGGER.info("Traversing the whole tree ({})", depthFirst ? "depth-first" : "breadth-first");
        TraversalDescription td = graphDb.traversalDescription()
                .uniqueness(Uniqueness.NONE)
                .evaluator(new TrueBEvaluator(neo4jOperations))
                .expand(new CustomPathExpander(neo4jOperations));
        if (depthFirst) {
            td = td.depthFirst();
        } else {
            td = td.breadthFirst();
        }

        int count = 0;
        for (Path ignored : td.traverse(root)) {
            count++;
        }
        return count;
    }

    private static class TrueBEvaluator implements Evaluator {
        private final Neo4jOperations neo4jOperations;

        public TrueBEvaluator(Neo4jOperations neo4jOperations) {
            this.neo4jOperations = neo4jOperations;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Node endNode = path.endNode();
            return Evaluation.ofIncludes(includes(endNode));
        }

        private boolean includes(Node endNode) {
            String propName = "value";
            return neo4jOperations.hasLabel(endNode, Labels.B) &&
                    (Boolean) neo4jOperations.getProperty(endNode, propName);
        }

        @Override
        public String toString() {
            return "TrueBEvaluator()";
        }
    }

    private class CustomPathExpander implements PathExpander<Object> {
        private final Neo4jOperations neo4jOperations;

        public CustomPathExpander(Neo4jOperations neo4jOperations) {
            this.neo4jOperations = neo4jOperations;
        }

        @Override
        public Iterable<Relationship> expand(Path path, BranchState<Object> state) {
            Node endNode = path.endNode();
            if (neo4jOperations.hasLabel(endNode, Labels.A)) {
                return endNode.getRelationships(Direction.OUTGOING, RelationshipTypes.HAS_B);
            } else if (neo4jOperations.hasLabel(endNode, Labels.B)) {
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
