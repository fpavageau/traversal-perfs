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

import org.neo4j.management.Cache;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

abstract class CacheOperations {
    private static final boolean enterprise = isEnterprise();

    private static boolean isEnterprise() {
        try {
            Class.forName("org.neo4j.management.Cache");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static CacheOperations create() {
        if (enterprise) {
            return new EnterpriseCacheOperations();
        }
        return new CommunityCacheOperations();
    }

    public abstract void clear();

    private static class CommunityCacheOperations extends CacheOperations {
        @Override
        public void clear() {
            // Nothing to do
        }
    }

    private static class EnterpriseCacheOperations extends CacheOperations {
        private final Cache nodeCache;
        private final Cache relationshipCache;

        public EnterpriseCacheOperations() {
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

        @Override
        public void clear() {
            if (nodeCache != null) {
                nodeCache.clear();
            }
            if (relationshipCache != null) {
                relationshipCache.clear();
            }
        }
    }
}
