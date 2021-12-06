# Docker

There are a lot of base images that we want to build due to OS package managers not always working correctly over the network. We don't necessairly want to push them to docker hub, so we can push them to our local container registry at `container-registry.com`

You must disconnect from the VPN for the build stage, and then reconnect for the push stage.

### Building all images

If for some reason you want to build and push all the base images (eg: if you need to rebuild the container-registry)  `./build_push_all.sh`

### Building a singular image
```
cd dataprofiler/docker/your_folder
docker build docker build --no-cache --pull -t container-registry.com/your_folder:latest .
docker push container-registry.com/your_folder:latest
```

### Versions

Everything directory with a file named `Dockerfile` will be built with the `latest` tag (ie `container-registry.com/APP:latest`). Some images have very explicit version numbers (ie `jenkins-deploy`). In that case, don't name the Dockerfile `Dockerfile`, and instead tag it with the `docker build -f` flag.