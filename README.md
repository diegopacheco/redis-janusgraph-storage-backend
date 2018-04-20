# Redis Storage Backend for JanusGraph

> [JanusGraph](http://janusgraph.org) is a highly scalable graph database optimized for storing and querying large graphs with billions of vertices and edges distributed across a multi-machine cluster. JanusGraph is a transactional database that can support thousands of concurrent users, complex traversals, and analytic graph queries.

[janusgraph-redis](janusgraph-redis) is an [initial and non-optimal implementation](#implementation) of a Redis [JanusGraph storage backend](http://docs.janusgraph.org/latest/storage-backends.html).

## Getting started

See [janusgraph-examples’s README](janusgraph-examples/README.md) for instructions to build and run the example adapted from JanusGraph’s.

## Features

*   Unordered Scan
*   Ordered Scan
*   Multi Queries
*   Batch Mutations
*   Optimistic Locking

Does not currently support locking nor transactions.

## Implementation

Implements `KeyColumnValueStorageManager` and therefore `KeyColumnValueStore`, and uses [Lattuce](https://lettuce.io) to interface with Redis.

Employs a key-indexed `HASH` (`KCV` covering index), a column-indexed `SET` (`CK` covering index), an `all-keys` covering index `SET`, and an `all-columns` covering index `SET` with internal Redis transactions—to ensure consistent updates—and asynchronous index (`all-keys` and `all-columns`) deletions.

`getKeys(KeyRangeQuery query, StoreTransaction txh)` and `getKeys(SliceQuery query, StoreTransaction txh)` are implemented with `SSCAN` on the `all-columns` index SET with _late-filtering_ rather than _pre-filtering_ (range scan or range read with ordered values).

More efficient iterations may want to drop the `all-keys` covering index, use `ZSET`s instead of `SET`s for both the `all-columns` covering index and the `CK` covering index with `ZRANGEBYLEX`-based in-Redis pre-filtering instead of in-app late-filtering with `matches(StaticBuffer start, StaticBuffer end, StaticBuffer item)`—take into account that `start` is inclusive and `end` is exclusive. [`SCAN`](https://redis.io/commands/scan#the-match-option) and derivates should not be used as `MATCH` does not support byte comparisons and does not pre-filter.

Currently a `KeyIterator` calls `HGET` synchronously on `next()` instead of prefetching results—consider alternatives.

Also, distributed locking and [Redis transactions](https://redis.io/topics/transactions) extended to the `KeyColumnValueStorageManager` may be necessary or desired.

## Purpose

The original goal was to extend JanusGraph to support [Redis Labs’ CRDB](https://redislabs.com/redis-enterprise-documentation/concepts-architecture/intercluster-replication/) in order to have a geo-distributed alternative to the wonderful [RedisGraph](http://redisgraph.io) by [Roi Lipman](https://github.com/swilly22) and [contributors](https://github.com/swilly22/redis-graph/graphs/contributors).
