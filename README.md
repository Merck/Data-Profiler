# DataProfiler

## Sub-Projects

The following is the list of projects within this repository.

### ansible

### api-docker-compose.yml

### api-docker-compose.yml.example

### build.py

### data_loading_daemon

### data_profiler_core

### data_preprocessing_scripts

### docker

### entity-detection

### lib

### muster

### row_loader

### web

### wekan

## Building the API

Use the following command to build the API

    cd data-profiler
    cp api-docker-compose.yml.example api-docker-compose.yml 
    COMPOSE_FILE=api-docker-compose.yml docker-compose up --build
