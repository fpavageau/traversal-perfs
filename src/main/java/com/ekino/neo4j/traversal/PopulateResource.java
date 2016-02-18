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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

/**
 * Resource to populate the database with a tree consisting of multiple levels of nodes connected like
 * {@code (:A)-[:HAS_B]->(:B))-[:HAS_A]->(:A)}.
 *
 * By default, it creates a tree with a depth of 5 and a fanout (number of children per parent) of 4, i.e. 1398101
 * nodes.
 *
 * The "depth" and "fanout" can be given as query parameters.
 */
@Path("/populate")
@Produces(MediaType.TEXT_PLAIN)
public class PopulateResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateResource.class);

    private static final int DEFAULT_DEPTH = 5;
    private static final int DEFAULT_FANOUT = 4;
    private static final int CHUNK_SIZE = 10_000;

    private final GraphDatabaseService graphDb;

    public PopulateResource(@Context GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @GET
    public Response populate(@QueryParam("depth") Integer depthParameter,
                             @QueryParam("fanout") Integer fanoutParameter) {
        int depth = getParameter(depthParameter, DEFAULT_DEPTH),
                fanout = getParameter(fanoutParameter, DEFAULT_FANOUT);

        int created = populate(depth, fanout);
        return Response.ok(String.valueOf(created) + "\n").build();
    }

    private static int getParameter(Integer parameter, int defaultValue) {
        if (parameter != null && parameter > 0) {
            defaultValue = parameter;
        }
        return defaultValue;
    }

    private int populate(int depth, int fanout) {
        // Don't create a new tree if one already exists
        try (Transaction ignored = graphDb.beginTx();
             ResourceIterator<Node> roots = graphDb.findNodes(Labels.Root)) {
            if (roots.hasNext()) {
                return -1;
            }
        }

        Random random = new Random();
        Node root = createRoot(random);
        Collection<Node> startNodes = Collections.singleton(root);
        for (int i = 0; i < depth; i++) {
            startNodes = createLevel(startNodes, fanout, random);
        }

        try (Transaction ignored = graphDb.beginTx();
             Result result = graphDb.execute("MATCH (n) RETURN COUNT(n) AS count")) {
            return ((Long) result.next().get("count")).intValue();
        }
    }

    private Node createRoot(Random random) {
        try (Transaction tx = graphDb.beginTx()) {
            Node root = createNodeWithValue(random, Labels.Root, Labels.A);
            tx.success();
            return root;
        }
    }

    /**
     * Create a new level in the tree: for each existing {@code A} leaf, connect {@code fanout} {@code B} nodes using
     * the {@code HAS_B} relationship, and for each of these {@code B} nodes, connect {@code fanout} {@code A} nodes
     * using the {@code HAS_A} relationship.
     *
     * @param startNodes The nodes to start the new level from, i.e. the current leaves of the tree
     * @param fanout The fanout of the tree
     * @param random The random generator
     * @return The new leaves
     */
    private Collection<Node> createLevel(Collection<Node> startNodes, int fanout, Random random) {
        LOGGER.info("Creating a new level");
        Collection<Node> newStartNodes = new ArrayList<>();
        Iterator<Node> startNodeIterator = startNodes.iterator();
        while (startNodeIterator.hasNext()) {
            try (Transaction tx = graphDb.beginTx()) {
                newStartNodes.addAll(createLevelChunk(startNodeIterator, fanout, random));
                tx.success();
            }
        }
        return newStartNodes;
    }

    /**
     * Create a chunk of the new level, to limit the size of a single transaction.
     *
     * @param startNodeIterator An iterator on the nodes to start the new level from
     * @param fanout The fanout of the tree
     * @param random The random generator
     * @return The leaves created in this chunk
     */
    private Collection<Node> createLevelChunk(Iterator<Node> startNodeIterator, int fanout, Random random) {
        LOGGER.info("Creating a new level chunk");
        Collection<Node> newStartNodes = new ArrayList<>();
        int createdNodes = 0;
        while (createdNodes < CHUNK_SIZE && startNodeIterator.hasNext()) {
            Node startNode = startNodeIterator.next();
            for (int i = 0; i < fanout; i++) {
                Node bNode = createChildNode(startNode, RelationshipTypes.HAS_B, Labels.B, random);
                createdNodes++;

                for (int j = 0; j < fanout; j++) {
                    newStartNodes.add(createChildNode(bNode, RelationshipTypes.HAS_A, Labels.A, random));
                    createdNodes++;
                }
            }
        }
        return newStartNodes;
    }

    private Node createChildNode(Node parentNode, RelationshipType relationshipType, Label label, Random random) {
        Node childNode = createNodeWithValue(random, label);
        parentNode.createRelationshipTo(childNode, relationshipType);
        return childNode;
    }

    private Node createNodeWithValue(Random random, Label... labels) {
        Node node = graphDb.createNode(labels);
        node.setProperty("value", random.nextBoolean());
        return node;
    }
}
