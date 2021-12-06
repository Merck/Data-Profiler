# dp_external_client

This client is a link to the data profiler API.

### Running local tests

The first time you run tests, you'll need to login to our container registry.
```
docker login container-registry.dataprofiler.com
```


You'll also need to increase your Docker Memory to allow the MiniAccumulo cluster to run.
```
Docker > Settings/Preferences > Resources > Memory > Increase to at least 8GB 
```


To run the tests locally
```
docker-compose -f test/build.yml run build ./build.py --api-copy
docker-compose -f test/test-runner-compose.yml up --build
docker-compose -f test/test-runner-compose.yml run ext_client python3 test/test.py
docker-compose -f test/test-runner-compose.yml down
```

If you make changes and want to run the tests again
```
docker-compose -f test/test-runner-compose.yml build ext_client && docker-compose -f test/test-runner-compose.yml run ext_client python3 test/test.py
```