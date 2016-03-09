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
import org.neo4j.management.Cache;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.util.Locale;

/**
 * Resource to manage the object caches in Neo4j.
 */
@Path("/cache")
@Produces(MediaType.TEXT_PLAIN)
public class CacheResource {
    private final Cache nodeCache;
    private final Cache relationshipCache;

    public CacheResource(@Context GraphDatabaseService graphDb) {
        nodeCache = getCache("NodeCache");
        relationshipCache = getCache("RelationshipCache");
    }

    private static Cache getCache(String cacheType) {
        try {
            ObjectName objectName = new ObjectName("org.neo4j:instance=kernel#0,name=Cache,name0=" + cacheType);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            if (mBeanServer.isRegistered(objectName)) {
                return JMX.newMBeanProxy(mBeanServer, objectName, Cache.class);
            }
        } catch (MalformedObjectNameException e) {
            // Shouldn't happen, return null anyway
        }
        return null;
    }

    @GET
    @Path("/clear")
    public String clear() {
        if (nodeCache == null) {
            return "No cache\n";
        }

        long nHitCount = nodeCache.getHitCount();
        long nTotal = nodeCache.getMissCount() + nHitCount;
        long rHitCount = relationshipCache.getHitCount();
        long rTotal = relationshipCache.getMissCount() + rHitCount;

        nodeCache.clear();
        relationshipCache.clear();

        return String.format(Locale.ENGLISH, "Hit ratio (nodes) = %.6f\nHit ratio (relationships) = %.6f\n",
                nTotal > 0 ? 1.0 * nHitCount / nTotal : Double.NaN,
                rTotal > 0 ? 1.0 * rHitCount / rTotal : Double.NaN);
    }
}
