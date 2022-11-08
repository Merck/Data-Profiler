
# This will be updated more - but this is the first cut and the load job formated. It needs to be changed.

{
    "jobId": "uuid",
    "datasetName": "someName",
    "status": "string (new, in-progress, completed, cancelled, error)",
    "submissionTimeStamp": 13123123,
    "creatingUser": "username",
    "datasetVisibilityExpression": "LIST.PUBLIC_DATA - can be left empty if dataset is not new",
    "deleteDatasetBeforeReload": false,
    "datasetProperties": {
        "key": "value"
    },
    "tables": [
        {
            "tableName": "someTable",
            "tableVisibilityExpression": "LIST.PUBLIC_DATA",
            "dataFormat": "format (csv, avro, parquet)",
            "sourceS3Bucket": "bucketName",
            "sourceS3Key": "/some/key/in/bucket",
            "estimatedRows": 12312312,
            "csvOptions": {
                "delimiter": ",",
                "escape": "\\",
                "quote": "\"",
                "charset": "utf-8"
            },
            "status": "string (new, in-progress, completed, cancelled, error)",
            "tableProperties": {
                "key": "value"
            },
            "columnProperties": {
                "key": "values"
            }
        }
    ]
}
