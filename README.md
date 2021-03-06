# Neo4j Traversals

This repository contains the code for a Neo4j unmanaged extension demonstrating a possible performance regression in the
traversals between Neo4j 2.2 and 2.3, probably related to the removal of the object cache.

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
        
1. Measure the response time.

        # Default breadth-first traversal mode
        ./run.sh
        # or explicit depth-first traversal mode
        ./run.sh --depth-first

## Traversal implementation

The code only has 5 source files, and the most relevant part is 
[`TrueBNodesCounter`](src/main/java/com/ekino/neo4j/traversal/TrueBNodesCounter.java) which
implements a traversal using custom `Evaluator` and `PathExpander`, based on the core Neo4j API: `Node::hasLabel`,
`Node::getProperty`, `Node::getRelationships`.

Profiling (using Yourkit) shows that the cost of all these API calls has increased between 2.2 and 2.3, and that more
garbage is generated by a request.

## Results

The times are in milliseconds.

<table>
  <tr>
    <th>Version</th>
    <th colspan="6">Neo4j Enterprise 2.2.7</th>
    <th colspan="6">Neo4j Enterprise 2.3.2</th>
  </tr>
  <tr>
    <th>Traversal mode</th>
    <th colspan="3">Breadth-First</th>
    <th colspan="3">Depth-First</th>
    <th colspan="3">Breadth-First</th>
    <th colspan="3">Depth-First</th>
  </tr>
  <tr>
    <th>Heap (MB)</th>
    <th>1024</th>
    <th>2048</th>
    <th>4096</th>
    <th>1024</th>
    <th>2048</th>
    <th>4096</th>
    <th>1024</th>
    <th>2048</th>
    <th>4096</th>
    <th>1024</th>
    <th>2048</th>
    <th>4096</th>
  </tr>
  <tr>
    <th>Mean</th>
    <td>6616</td>
    <td>3441</td>
    <td>2045</td>
    <td>2229</td>
    <td>2209</td>
    <td>1276</td>
    <td>2771</td>
    <td>2500</td>
    <td>2465</td>
    <td>1544</td>
    <td>1543</td>
    <td>1540</td>
  </tr>
  <tr>
    <th>50%</th>
    <td>6308</td>
    <td>3281</td>
    <td>2022</td>
    <td>2228</td>
    <td>2208</td>
    <td>1277</td>
    <td>2748</td>
    <td>2487</td>
    <td>2443</td>
    <td>1549</td>
    <td>1517</td>
    <td>1547</td>
  </tr>
  <tr>
    <th>90%</th>
    <td>8520</td>
    <td>3823</td>
    <td>2296</td>
    <td>2406</td>
    <td>2415</td>
    <td>1391</td>
    <td>2981</td>
    <td>2672</td>
    <td>2674</td>
    <td>1679</td>
    <td>1690</td>
    <td>1693</td>
  </tr>
  <tr>
    <th>95%</th>
    <td>8820</td>
    <td>5268</td>
    <td>2353</td>
    <td>2454</td>
    <td>2488</td>
    <td>1424</td>
    <td>3145</td>
    <td>2773</td>
    <td>2709</td>
    <td>1711</td>
    <td>1727</td>
    <td>1716</td>
  </tr>
  <tr>
    <th>Max</th>
    <td>10591</td>
    <td>5807</td>
    <td>2856</td>
    <td>2485</td>
    <td>2795</td>
    <td>1513</td>
    <td>3722</td>
    <td>3321</td>
    <td>2936</td>
    <td>1882</td>
    <td>1771</td>
    <td>1757</td>
  </tr>
</table>

## Benchmarking pitfalls

After some discussion on Slack with Michael Hunger (from Neo Technology) who came up with different results in a
slightly different context, it turns out the benchmark between Neo4j 2.2 and 2.3 was not a fair comparison, as it was
comparing apples and oranges: because the same traversal is called repeatedly (i.e. a synthetic benchmark) and the
dataset fits in memory, the 2.2 run is not actually benchmarking a raw traversal but a traversal with lots of cache
(using the node and relationship caches). In that perfect scenario, the repeated traversal is indeed faster.

For a fairer comparison, I've added a [new service](src/main/java/com/ekino/neo4j/traversal/CacheResource.java) to clear
the caches, then re-run the best-case scenario for 2.2 clearing the cache between each traversal using a new switch in
the [custom script](run.sh):

    ./run.sh --depth-first --clear

The new results show that with an empty object cache, Neo4j 2.2 performs much worse than 2.3. So, is 2.3 actually better
or worse than 2.2? It depends... It's way better when the cache is cold, but worse when it's warm and everything fits
in. Since there's no going back, the increased cost of some operations (e.g. `Node::hasLabel`) will have to be mitigated
at the application level by locally introducing some cache.

## Results

The times are in milliseconds.

<table>
  <tr>
    <th>Version</th>
    <th colspan="2">Neo4j Enterprise 2.2.7, Depth-First</th>
    <th>Neo4j Enterprise 2.3.2, Depth-First</th>
  </tr>
  <tr>
    <th>Heap (MB)</th>
    <th>4096</th>
    <th>4096<br>w/ cache clear</th>
    <th>4096</th>
  </tr>
  <tr>
    <th>Mean</th>
    <td>1276</td>
    <td>2938</td>
    <td>1540</td>
  </tr>
  <tr>
    <th>50%</th>
    <td>1277</td>
    <td>2889</td>
    <td>1547</td>
  </tr>
  <tr>
    <th>90%</th>
    <td>1391</td>
    <td>3178</td>
    <td>1693</td>
  </tr>
  <tr>
    <th>95%</th>
    <td>1424</td>
    <td>3238</td>
    <td>1716</td>
  </tr>
  <tr>
    <th>Max</th>
    <td>1513</td>
    <td>3642</td>
    <td>1757</td>
  </tr>
</table>

## Caching by yourself

It's possible to cache the results of [`Node::hasLabel`](src/main/java/com/ekino/neo4j/traversal/Neo4jLabelOperations.java)
or [`Node::getProperty`](src/main/java/com/ekino/neo4j/traversal/Neo4jPropertyOperations.java) between queries to reduce
the I/O and memory consumption, even if the page cache helps:

    ./run.sh --depth-first --cache label
    # or
    ./run.sh --depth-first --cache property
    # or
    ./run.sh --depth-first --cache label,property

Obviously, this increases the memory footprint of the application (working set), and will have an effect on the duration
of the garbage collection, but because it reduces the I/O and thus its use of buffers, it decreases the garbage
generated per request (i.e. the memory consumed by the request). The test implementations of the caches are unbounded,
and effectively duplicate part of the database in memory.

## Results

The times are in milliseconds.

<table>
  <tr>
    <th>Version</th>
    <th colspan="4">Neo4j Enterprise 2.3.2, Depth-First</th>
  </tr>
  <tr>
    <th>Heap (MB)</th>
    <th>4096</th>
    <th>4096<br>w/ label cache</th>
    <th>4096<br>w/ property cache</th>
    <th>4096<br>w/ label & property caches</th>
  </tr>
  <tr>
    <th>Mean</th>
    <td>1540</td>
    <td>1209</td>
    <td>1387</td>
    <td>1067</td>
  </tr>
  <tr>
    <th>50%</th>
    <td>1547</td>
    <td>1207</td>
    <td>1384</td>
    <td>1053</td>
  </tr>
  <tr>
    <th>90%</th>
    <td>1693</td>
    <td>1229</td>
    <td>1427</td>
    <td>1100</td>
  </tr>
  <tr>
    <th>95%</th>
    <td>1716</td>
    <td>1241</td>
    <td>1432</td>
    <td>1192</td>
  </tr>
  <tr>
    <th>Max</th>
    <td>1757</td>
    <td>1284</td>
    <td>1447</td>
    <td>1248</td>
  </tr>
  <tr>
    <th>Working set (MB)</th>
    <td>24</td>
    <td>42</td>
    <td>40</td>
    <td>58</td>
  </tr>
  <tr>
    <th>Garbage per request (MB)</th>
    <td>76</td>
    <td>46</td>
    <td>73</td>
    <td>43</td>
  </tr>
</table>

### Configurations

MacBook Pro Core i7 2.2 GHz, 16 GB

Oracle 64-bit JDK 1.8.0_74

`data/graph.db` takes 402 MB.

    dbms.pagecache.memory=500m
    
------

Licensed under the Apache License, Version 2.0
