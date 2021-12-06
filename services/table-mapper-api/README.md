# Table Mapper API

This is an API and storage mechanism that allows tracking of tables exported to an external SQL database. This API also tracks the associated users enabling authorization.

## Creating and Updating a Table

When creating a new table, at least one user must be assigned to the table.

```
mutation {
  createUpdateTable(
      environment: "production",
      datasetname: "dataset name",
      tablename: "table name",
      visibility: "LIST.PUBLIC_DATA",
      users: [
        "user1",
        "user2"
      ]) {
    id
    environment
    datasetname
    tablename
    visibility
    users {
      id
      username
    }
  }
}
```

More users can be added and the table can updated through the same API call.

## Removing User(s) from a Table

To remove users from a table, use the `removeUsersFromTable` mutation.

```
mutation {
  removeUsersFromTable(
      environment: "production",
      datasetname: "dataset name",
      tablename: "table name",
      visibility: "LIST.PUBLIC_DATA",
      users: [
        "user2"
      ]) {
    id
    environment
    datasetname
    tablename
    visibility
    users {
      id
      username
    }
  }
}
```

## Listing All tables and Associated Users

The following query will list all of the tables currently being tracked and all of the users that are able to view the table.

```
query {
  allTables {
    id
    environment
    datasetname
    tablename
    visibility
    users {
      id
      username
    }
  }
}
```

## Deleting a Table

To delete a table the `deleteTables` mutaiton can be used. Be careful as this will delete all matching tables and associated users.

mutation {
  deleteTables(
    environment: "production",
    datasetname: "dataset name",
    tablename: "table name",
    visibility: "LIST.PUBLIC_DATA",
  )
}

## Deleting Everything

If things really go wrong, everything be be deleted with a call to `deleteAllTables`.

mutation {
  deleteAllTables {
    id
    environment
    datasetname
    tablename
    visibility
    users {
      id
      username
    }
  }
}
