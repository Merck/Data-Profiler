# Job API
This is a super simple API to store a list of jobs in a database and let you get them back out again. It's a pretty
basic port of what we were doing in Accumulo before. Maybe we will move to a full blown job queue in the future. Who
knows. But for now, this should be sufficient.

There should be one of these for all of the environments.

You can get to an interactive editor by going to `/graphiql`on a running server.

Here is what you can do  with this. Create a new job:

mutation {
  createJob (
    environment: "production",
    type: "tableLoad",
    creatingUser: "username",
    details: "{ \"some\": \"other\"}") {
    id
  }
}

All of the guts of the job go into the details as a json blob (documented below).

Get all of the jobs back out (with all of the common fields):

{
  allJobs {
    id
    environment
    status
    statusDetail
    creatingUser
    type
    createdAt
    updatedAt
    details
  }
}

A combination of environment and id can be used as a unique id.

Update the status of a job:

mutation {
  updateJobStatus(id: 1, status: "running") {
    id
    createdAt
    updatedAt
  }
}

Status can be new, running, complete, error, or killed.

Find a job that is ready to be executed:

{
  executableJob(types: ["tableLoad"]) {
    id
    environment
    type
    details
  }
}

### Prereqs
- You have docker on your machine

### How to Launch
1) docker-compose up
2) In the `jobs-api_rou_api_1` container, run the `yarn run migrate`
3) To create random, totally unrealistic dummy data `yarn run seed`

# Job Details

This is the information that goes in the details field per job type.

## tableLoad

 {
    "datasetName": "someName",
    "tableName": "someTable",
    "metadataVersion", "uuid",
    "tableVisibilityExpression": "LIST.PUBLIC_DATA",
    "dataFormat": "format (csv, avro, parquet)",
    "sourceS3Bucket": "bucketName",
    "sourceS3Key": "/some/key/in/bucket",
    "estimatedRows": 12312312,
    "options": {
        "delimiter": ",",
        "escape": "\\",
        "quote": "\"",
        "charset": "utf-8"
    },
    "tableProperties": {
        "key": "value"
    },
    "columnProperties": {
        "key": "values"
    }
}

Options will vary according to dataFormat. The ones listed are valid for CSV.

## bulkIngestAndCommit

{
  "metadataVersion": "uuid",
  "datasetVisibilityExpression": "LIST.PUBLIC_DATA",
  "fullDatasetLoad": false
}
