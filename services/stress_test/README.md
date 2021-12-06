# Stress Test

The stress test simulates a user by calling the API in the same way that a user would when using the Data Profiler. The stress test currently simulates a user browsing data through the Understand Tab.

## Configuration

A configuration file named `config.txt` must be created and placed in the root of the project directory. The configuration file needs to have an API key for the environment that is being tested. `config.sample` is a sample configuration file.

## Execution with Docker

To run with docker, docker must be installed. After docker is installed run the following script

    ./docker_run.sh

Once the container starts the scan can be configured by visiting the locust UI in your browser. By default, it is located [http://localhost:8089/](http://localhost:8089) 

## Execution Locally

### Installation

Before running locally you must install the following requirements

* Requires Python 3
* pip3 install -r python_requirements.txt will install all of the needed Python libraries.

### Running

The stress test can be executed using the following command, where `hostname` is the name of the host that is being tested.

    locust -f locusttest.py --host=<hostname>

Once the locust process starts, the scan can be configured by visiting the locust UI in your browser. By default, it is located [http://localhost:8089/](http://localhost:8089) 