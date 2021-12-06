# Cluster Shell

This container will automatically build and inject the python and java libraries.

# Build and Push

Run the build_and_push.sh file from the cluster-shell directory, it will automatically build the app and the python package.

# Execution

To run the container, use the kube-run.sh script. It takes 2 paramaters _image_ and _namespace_. Also it will take an optional parameters _tag_. To run on development, execute the following:

    kube-run.sh cluster-shell development

This should return a shell to you, but, worst case, you can run the following to get another

    kubectl exec -n development -it cluster-shell -- /bin/bash

# Notes

This is based on Dockerfile.alpine and should be relatively small. When building, on my machine, took about 8min (522s according to docker).
