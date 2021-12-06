# Data Loading Daemon

The purpose of this container monitor for new jobs and kick them off.

## Installation

This container contains two requirements:

* Data Profiler Python client distribution packages

The Python distribution packages can be generated from within the `python_client` directory. Run the following command from within the `python_client` directory to generate distribution packages and copy the wheel package to the `python_packages` directory.

    python3 setup.py sdist bdist_wheel

After the requirements are met, an image can be built and pushed to our internal container registry using the `build_and_push.sh` script.
