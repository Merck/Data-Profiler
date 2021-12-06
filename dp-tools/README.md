# Dataprofiler Tools

The purpose of this container is to build a small alpine base image containing py3 and java8 needed
for many other dataprofiler containers


## Build and Push

Select alpine or Debian slim base image, build, tag and push to our container registry

```bash
#!/usr/bin/env bash

# select slim or alpine
base_img=${1:-alpine}
TAG=container-registry.dataprofiler.com/dataprofiler-"${base_img}":latest

docker build -t $TAG -f "Dockerfile.${base_img}" . && \
  docker push $TAG

```

## To Extend Setup

In your new directory ensure you have `data_profiler_core_jars` and `python_packages` directories

```bash
mkdir -p data_profiler_core_jars
touch data_profiler_core_jars/.keep
mkdir -p python_packages
touch python_packages/.keep
```

Add those directories to `.gitignore`

```text
data_profiler_core_jars/
python_packages/

```

Edit `build.py` and ensure you have an entry for your new directory, this will ensure the script
will copy artifacts over.

## To Extend Dependencies

Build the latest python and java libraries, and have them copied to your directory

```bash
./build.py --api-copy
```

or build and copy manually

```bash
    my_extension="mynewdockerproject"
    cd python_client
    python3 setup.py sdist bdist_wheel
    cp dist/dataprofiler_api-1.0-py3-none-any.whl ../"${my_extension}"/python_packages/.
    cd ../data_profiler_core
    mvn clean install -DskipTests -Plocal
    cp tools/target/dataprofiler-tools-1.jar ../"${my_extension}"/data_profiler_core_jars/dataprofiler-tools-current.jar
```

## To Extend Dockerfile

In your `Dockerfile` add the following to extend this base image to include the
latest [python_client](../python_client) and [data_profiler](../data_profiler_core) tools jar

```Dockerfile
RUN pip3 install --upgrade pip

ENV WHEEL dataprofiler_api-1.0-py3-none-any.whl

WORKDIR /opt/app

ADD python_packages/${WHEEL} .
RUN pip3 install ${WHEEL}
ADD data_profiler_core_jars/dataprofiler-tools-current.jar .
```

Then build and push your new image

```
#!/usr/bin/env bash
TAG=container-registry.dataprofiler.com/dataprofiler-extension:latest
docker build -t $TAG .
docker push $TAG
```