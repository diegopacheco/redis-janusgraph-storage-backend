# JanusGraph Examples

The JanusGraph examples show the basics of how to configure and construct
a graph application. It uses [Apache Maven](https://maven.apache.org) to
manage the numerous dependencies required to build the application. The common
application will:

* Open and initialize the graph
* Define the schema
* Build the graph
* Run traversal queries to get data from the graph
* Make updates to the graph
* Close the graph

By using different graph configurations, the same example code can run against
the various supported storage and indexing backends.

## Prerequisites

* Java 8 Development Kit, update 40 or higher. [Oracle Java](https://www.oracle.com/java/index.html) and [OpenJDK](http://openjdk.java.net/) have been tested successfully.
* [Apache Maven](http://maven.apache.org/), version 3.3 or higher

### JanusGraph distribution

Download the latest [JanusGraph release](https://github.com/JanusGraph/janusgraph/releases)
distribution zip file and unzip it.

### Build the examples

The examples are packaged with the JanusGraph distribution zip file in the
`examples` directory. Run the Maven build command from the `examples` directory.

```
mvn clean install
```

## Run the examples

These commands can be run from the `examples` or project's directory.
Please refer to the specific directions for each project as there may be
additional setup and configuration required.

### [Common and In-Memory](example-common/README.md)

```
mvn exec:java -pl :example-common
```

### [Redis](example-redis/README.md)

```
mvn exec:java -pl :example-redis
```

## Drop the graph

After running an example, you may want to drop the graph from storage. Make
sure to stop the application before dropping the graph. These commands can
be run from the `examples` or the project's directory, but please refer to
the specific directions for each project as there may be additional steps
for clean up.

### [Redis](example-redis/README.md)

```
mvn exec:java -pl :example-redis -Dcmd=drop
```