### Prereqs
- You have docker on your machine

### How to Launch
1) docker-compose up
2) In the `rules-of-use-api_rou_api_1` container, run the `yarn run migrate`
3) To create random, totally unrealistic dummy data `yarn run seed`

### Before Going to Production
```
# Add an API Key
```
kubectl exec -i -t dp-rou-b577f5c78-j7ksr -c rou-db -- psql -U postgres rules_of_use
insert into applications (app,key,created_at,updated_at) values('Data Profiler', 'THE_API_KEY_YOU_WANT', current_timestamp, current_timestamp);
```

# Add a legacy auth
$ echo "insert into attributes (value,created_at,updated_at,is_active) values('LIST.PUBLIC_DATA', current_timestamp, current_timestamp, true);" | docker exec rulesofuseapi_db_1 psql -U postgres rules_of_use
```

### Dumping from other databases
```docker exec -t rulesofuseapi_db_1 pg_dumpall -c -U postgres > /tmp/rou_dump.sql
cat /tmp/rou_dump.sql | docker exec -i rulesofuseapi_db_1 psql -U postgres
```

### Use for Root Expression Tracking
```
# Mark the attribute "HR.Foo.Bar" as active for the root expression
mutation{markAttributeActive(value:"HR.Foo.Bar"){id value is_active}}

# Mark the attribute "HR.Foo.Bar" as inactive for the root expression
mutation{markAttributeInactive(value:"HR.Foo.Bar"){id value is_active}}

# See all active attributes for the root expression
{attributesActive}
```

### Add a user
```
mutation {
  createUpdateUser(
    username: "username",
    attributes: [
      "HR.Foo.Bar",
      "HR.Foo.Bat",
      "HR.Foo.Baz",
      "HR.Something.New"
    ]) {
    id attributes {
      value
    }
  }
}
```

`curl localhost:8081/graphql -H 'Authorization: dp-rou-key' -H 'Content-Type: application/json' -XPOST --data '{"query":"mutation {createUpdateUser(username: \"username\", attributes: [\"system.admin\"]) {id } }"}'`

### Remove an attribute from a user
```
mutation {
  removeAttributesFromUser(
    username: "username",
    attributes: [
      "HR.Something.New"
    ]) {
    id attributes {
      value
    }
  }
}
```

### Remove a user
```
mutation {
  removeUser(
    username: "username"
    ) {
    id 
  }
}
```