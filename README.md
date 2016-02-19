# Neo4j Traversals

This repository contains the code for a Neo4j unmanaged extension demonstrating a performance regression in the 
traversals between Neo4j 2.2 and 2.3, probably due to the simplification of the cache.

## Installation

1. Clone this repository
1. Build it locally using Maven and a 1.7 JDK:

        mvn clean package
    
1. Copy the resulting artifact to the `plugins` directory in your Neo4j installation:

        cp target/traversal-perfs-1.0-SNAPSHOT.jar /path/to/neo4j/plugins/
        
1. Configure Neo4j to load the unmanage extension by adding in `conf/neo4j-server.properties`:

        org.neo4j.server.thirdparty_jaxrs_classes=com.ekino.neo4j.traversal=/traversal-perfs

## Usage

1. Start your Neo4j instance, if possible with an empty database:

        bin/neo4j start
        
1. Populate the database with a traversable tree.
        
        curl localhost:7474/traversal-perfs/populate
        
    The tree consists of a root node (with the `:Root` and `:A` labels), connected to `:B` nodes through a `HAS_B`
    relationship, connected to more `:A` nodes through a `HAS_A` relationship. By default, the tree has a depth of 5 
    such "levels", and a fanout factor of 4 (the number of children nodes for a parent node), i.e. 1398101 nodes.
    Those parameters can be modified using query parameters, "depth" and "fanout":
     
        curl localhost:7474/traversal-perfs/populate?depth=8&fanout=2
        
     The database needs to be emptied before a new tree can be generated.
        
1. Warm-up the JVM
        
        ab -c 1 -n 100 localhost:7474/traversal-perfs/traverse > /dev/null 2>&1

1. Measure the response time:

        ab -c 1 -n 100 localhost:7474/traversal-perfs/traverse

## Results

The times are in milliseconds.

<table>
  <tr>
    <th></th>
    <th>Neo4j Enterprise 2.2.7</th>
    <th>Neo4j Enterprise 2.3.2</th>
  </tr>
  <tr>
    <td>Mean</td>
    <td>1914</td>
    <td>3109</td>
  </tr>
  <tr>
    <td>50%</td>
    <td>1890</td>
    <td>3088</td>
  </tr>
  <tr>
    <td>90%</td>
    <td>2169</td>
    <td>3358</td>
  </tr>
  <tr>
    <td>95%</td>
    <td>2254</td>
    <td>3470</td>
  </tr>
  <tr>
    <td>Max</td>
    <td>2322</td>
    <td>3707</td>
  </tr>
</table>

### Configurations

MacBook Pro Core i7 2.2 GHz, 16 GB

Oracle 64-bit JDK 1.8.0_74

`data/graph.db` takes 402 MB.

#### Neo4j Enterprise 2.2.7

    wrapper.java.initmemory=2048
    wrapper.java.maxmemory=4096
    dbms.pagecache.memory=500m

#### Neo4j Enterprise 2.3.2

    wrapper.java.initmemory=512
    wrapper.java.maxmemory=512
    dbms.pagecache.memory=500m
    
Note that because of the removal of the object cache, Neo4j 2.3 needs (much) less memory. I did run the same benchmark
with init/max heap parameters of 1024/1024, 2048/2048 and 2048/4096, and it didn't change the performance of 2.3, which
is why I kept the minimal configuration.

Running with a JDK 7 or 8 gives similar results, as does using the G1 or CMS garbage collectors.

------

Licensed under the Apache License, Version 2.0
