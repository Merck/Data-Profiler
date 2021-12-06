# DataProfiler Standalone

This development tool gives a developer the ability to run the DataProfiler application
stack on their local machines via Minikube and Docker Desktop. 

The following containerized deployments are currently supported:

    dp-accumulo - MiniAccumuloWithData
    dp-postgres - postgres database
    dp-api - data_profiler_api
    dp-rou - rules_of_use
    dp-spark-sql-controller - Spark SQL Thriftserver Instance Controller
    dp-ui - DataProfiler User Interface

### Requirements

* Python 3
* Docker Desktop
* Minikube
* Helm

### Quick Start / Notes

Starting Minikube:
```bash
$ minikube start --cpus <num of cpus> --memory <memory>
```

Configure environment to use minikube's docker daemon:
```bash
$ eval `minikube docker-env`
```

Minikube dashboard:
```bash
$ minikube dashboard
```

## standalone.py

### Usage

```bash
$ ./bin/standalone.py -h        
usage: standalone.py [-h] {clean,run,build,status,push} ...

optional arguments:
-h, --help            show this help message and exit

sub-commands:
{deploy,destroy,build,status,push,expose}
```

Sub-Commands
* clean - delete cluster deployments
* build - builds libraries & images used in deployments
* run - create cluster deployments
* status - display deployment status


### Deploying / Starting Local Cluster

```bash
$ ./bin/standalone.py deploy --help

usage: standalone.py deploy [-h] [--branch BRANCH] [--app {dp-accumulo,dp-postgres,dp-api,dp-rou,dp-spark-sql-controller,dp-ui}]

optional arguments:
  -h, --help            show this help message and exit
  --branch BRANCH       branch to build from
  --app {dp-accumulo,dp-postgres,dp-api,dp-rou,dp-ui}
                        deployment name
```

Examples:

Deploy all available components and expose a few endpoints:

```bash
$ ./bin/standalone.py run

* Deploying Components
    dp-accumulo........................deployed
    dp-postgres........................deployed
    dp-api.............................deployed
    dp-rou.............................deployed
    dp-spark-sql-controller............deployed
    dp-ui..............................deployed
* Configuring Rules Of Use
  * Created api key as 'dp-rou-key'
  * Activating ROU attributes
    > activating system.admin
    > activating system.login
    > activating system.seen_joyride
    > activating LIST.PUBLIC_DATA
    > activating LIST.Public_Data
    > activating LIST.HR
    > activating LIST.EMPLOYEE
    > activating LIST.TECHMANAGER
    > activating LIST.SALESMANAGER
  * Updating ROU attributes for 'developer'

Standalone Minikube Status
* service default/dp-ui has no node port
* Starting tunnel for service dp-ui.
|-----------|-------|-------------|------------------------|
| NAMESPACE | NAME  | TARGET PORT |          URL           |
|-----------|-------|-------------|------------------------|
| default   | dp-ui |             | http://127.0.0.1:59209 |
|-----------|-------|-------------|------------------------|
http://127.0.0.1:59209
dp-api available at http://localhost:9000
dp-rou available at http://localhost:8081
dp-spark-sql-controller available at http://localhost:7999
```

Deploy a specific component:

```
$ ./bin/standalone.py run --app dp-ui

Deploying:
* dp-ui
  minikube cluster status
* service default/dp-ui has no node port
* Starting tunnel for service dp-ui.
  |-----------|-------|-------------|------------------------|
  | NAMESPACE | NAME  | TARGET PORT |          URL           |
  |-----------|-------|-------------|------------------------|
  | default   | dp-ui |             | http://127.0.0.1:59560 |
  |-----------|-------|-------------|------------------------|
  http://127.0.0.1:59560
  dp-api available at http://localhost:9000
  dp-rou available at http://localhost:8081
  dp-spark-sql-controller available at http://localhost:7999
```

#### API

Try hitting the api with the following curl command:
```bash
curl http://localhost:9000/v1/datasets -H 'Authorization: dp-rou-key' -H 'Accept: application/json' -H 'X-Authorization-Token: local-developer' -H '\"LIST.Public_Data\",\"LIST.PUBLIC_DATA\"'
```


#### Supplying data

Data files found in the root 'data' directory will be loaded by the containerized dp-accumulo deployment.


### Destroying / Terminating 

Delete all deployments in the local kubernetes cluster

```
$ ./bin/standalone.py deploy
```

Delete the specified <APP> deployment

```
$ ./bin/standalone.py deploy --app <APP>
```

### Building deployments:
Build all docker images only
```
./bin/standalone.py build
```
Build dataprofiler jars and all build docker images
```
./bin/standalone.py build --jars
```
Build libraries and docker images for a specific component:
```
./bin/standalone.py build --jars --app <component>
```


## Directory Structure
```
├── README.md
├── bin
│   ├── custom_formatter.py
│   ├── lib
│   ├── scripts
│   └── standalone.py
├── conf
│   ├── dp-accumulo
│   ├── dp-api
│   ├── dp-data-loading-daemon
│   ├── dp-postgres
│   ├── dp-rou
│   └── dp-ui
├── data
│   ├── basic_test_data.csv
│   ├── cell_level_visibilities_test_data.csv
│   └── tiny_test_data.csv
├── lib
│   ├── iterators
│   ├── python_packages
│   └── tools
├── sbin
│   ├── accumulo-shell.sh
│   ├── cluster-shell.sh
│   ├── postgres-shell.sh
│   └── spark-shell.sh
└── venv
```

* bin - main executables
* conf - kubernetes deployment configurations as well as any extra settings required to run on the local cluster
* data - seed data for the Accumulo instance
* sbin - helper scripts for use with the local cluster

## Utilities

### Accumulo Shell

```bash
./sbin/accumulo-shell.sh

root@miniInstance>
```

### Postgres Shell

```bash
./sbin/postgres-shell.sh

psql (13.2 (Debian 13.2-1.pgdg100+1))
Type "help" for help.

postgres=#
```

### Misc

After some time, the process of constantly building images may fill up cache space.
Run the following to free up space:

```bash
minikube ssh -- docker system prune
```


