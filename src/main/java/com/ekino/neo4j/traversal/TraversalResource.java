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
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource performing a traversal to count the {@code B} nodes which "value" property is {@code true}.
 */
@Path("/traverse")
@Produces(MediaType.TEXT_PLAIN)
public class TraversalResource {
    private final GraphDatabaseService graphDb;

    public TraversalResource(@Context GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @GET
    public Response traverse(@QueryParam("depthFirst") String depthFirstParameter) {
        boolean depthFirst = depthFirstParameter != null;
        try (Transaction ignored = graphDb.beginTx()) {
            int count = new TrueBNodesCounter(graphDb).count(depthFirst);
            return Response.ok(count + "\n").build();
        }
    }
}
