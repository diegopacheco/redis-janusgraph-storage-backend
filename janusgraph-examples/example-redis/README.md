# Redis Storage

## About Redis

[Redis](https://redis.io) is an in-memory data structure store, used as a database, cache and message broker. It supports data structures such as strings, hashes, lists, sets, sorted sets with range queries, bitmaps, hyperloglogs and geospatial indexes with radius queries.

## JanusGraph configuration

[`jgex-redis.properties`](conf/jgex-redis.properties) contains
the configurations for Redis and Lucene.

Refer to the JanusGraph [configuration reference](http://docs.janusgraph.org/latest/config-ref.html)
for additional properties.

## Dependencies

The required Maven dependency for Redis:

```
        <dependency>
            <groupId>flagello.janusgraph</groupId>
            <artifactId>janusgraph-redis</artifactId>
            <version>${janusgraph.version}</version>
            <scope>runtime</scope>
        </dependency>
```

## Run the example

This command can be run from the `examples` or the project's directory.

```
mvn exec:java -pl :example-redis
```

## Drop the graph

After running an example, you may want to drop the graph from storage. Make
sure to stop the application before dropping the graph. This command can be
run from the `examples` or the project's directory.

```
mvn exec:java -pl :example-redis -Dcmd=drop
```

The configuration uses the application name `jgex` as the root directory
for the Redis directories. The directory is safe to remove
after running the drop command.

```
rm -rf jgex/
```
