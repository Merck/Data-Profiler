# Data Profiler Tools

This projects houses all of the tools required for the Data Profiler. Descriptions of the individual projects can be found in their project directories.

## Building All Sub-Projects

To build all sub-projects maven can be used.

    mvn clean install -DskipTests -Plocal

This will compile all sub projects and output jars in the sub projects respective `target` directory.

## Sub-Projects

### Util

A description of the `dataprofiler-util` project can be found [here](util/README.md)

### Iterators

A description of the `dataprofiler-iterators` project can be found [here](iterators/README.md)

### Tools

A description of the `dataprofiler-tools` project can be found [here](tools/README.md)

## Adding a Sub-project

If you are adding a sub-project to this project make sure you specify the parent in the `pom.xml`.

    <parent>
      <groupId>com.dataprofiler</groupId>
      <artifactId>dataprofiler-parent</artifactId>
      <version>1</version>
    </parent>

