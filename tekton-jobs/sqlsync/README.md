# SQLSync Container

The purpose of this container is to write an accumulo table into a redshift or RDS table using the sqlsync-pipeline in tekton.

## Installation

This container contains two requirements:

* Data Profiler Python client distribution packages
* Data Profiler Tools JAR

The Python distribution packages can be generated from within the `python_client` directory. Run the following command from within the `python_client` directory to generate distribution packages and copy the wheel package to the `python_packages` directory.

    python3 setup.py sdist bdist_wheel

The Data Profiler Tools JAR can be built by running the following command from the `data_profiler_core` directory.

    mvn clean install -DskipTests -Plocal

This will create the required JAR in `data_profiler_core/tools/target/`. Copy `dataprofiler-tools-current.jar` to the `data_profiler_core_jars` directory

After the requirements are met, an image can be built and pushed to our internal container registry using the `build_and_push.sh` script.
