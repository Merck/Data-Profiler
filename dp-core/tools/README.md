# Data Profiler Tools

The Tools project is a collection of tools used in the Data Profiler.

## Delete Data

Delete data is an application that can be used to remove data from the Data Profiler. To use the delete data application, use the following command.

    java -cp dataprofiler-tools-1.jar DeleteData


## Generate Metadata

Generate Metadata is an applicaiton that generates useful metadata that is used by the front-end. To use the generate metatdata application, use the following command.

    java -cp dataprofiler-tools-1.jar com.dataprofiler.metadata.RebuildMetadata


## Generate Samples

Generate samples generates _n_ samples of each data source to allow previews to be displayed in the front-end.
To use the generate sample application, use the following command.

    java -cp dataprofiler-tools-1.jar CreateColumnCountSamples

