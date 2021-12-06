# Data Profiler Iterators

The `dataprofiler-iterators` project is a collection of iterators for that Data Profiler.

## Building the Iterators

The iterators can be built with maven using the following command.

    mvn clean install

The previous command will create several files in the `target` directory. The file `dataprofiler-iterators.jar` only contains the iterators, it does not include any required libraries. The jars in the `required_jars` directory are the libraries that must be copied with the iterator jar to the Accumulo servers.

## Adding the Iterators to Accumulo

To add the iterators to Accumulo, the iterator jar, `dataprofiler-iterators.jar`, and jars under the `required_jars` directory need to be placed under Accumulo's `lib/ext` directory on every tablet server.

Accumulo's class loader should load the jars automatically, so Accumulo does not need to be rebooted.

## Including the Iterators in a Project

To include the iterators in a project copy the following dependency to the `pom.xml` of your project.

    <dependency>
      <groupId>com.dataprofiler</groupId>
      <artifactId>dataprofiler-iterators</artifactId>
      <version>1</version>
    </dependency>

