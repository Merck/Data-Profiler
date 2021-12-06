
# reqParams
Pass these parameters as an object into the api method found in `web/ui/src/connectors/api/index.js`

### resource (*)
```
String
Required
- this is the base of the url request.
- ie google.com/foo resource would be 'foo'
```


### identifiers
```
Array<String>
- these are appended to the resource above
- the main use of identifiers are to substitute variables into the url
- ie google.com/foo/bar/1/2/3 could have resource = 'foo', and identifiers = ['bar',1,2,3])
- ie google.com/foo/bar/1/2/3 also could have resource = 'foo/bar', and identifiers = [1,2,3])
```

### queryParams
```
Object
- these are standard query strings appended to the search part of the URI
- ie google.com?foo=1&bar=2 queryParams would be {foo:1, bar:2})
```

### specialVisibilities
```
Array<String>
- these is a special appended visibility to grab certain data with a special tag)
- they should start with "special." otherwise the api will reject thiem
```

### timeout
```
Integer
- this will timeout/fail a request if it takes more than this time
- default 120000
```

### useThrottle
```
Boolean
- if the requests are done in a component in an interval, this will prevent from more requests launching than have completed
```

### postObject
```
Object
- this will be send as the put/post body in the request
- by default is application/json, but can be overridden with overrideContentType
```

### withoutAuth
```
Boolean
- send the request without identification/authentication
```

### isDownloadable
```
Boolean
- will signal to the browser that the result should be downloaded
```

### overrideContentType
```
String
- set an alternate Accept and Content-Type MIME type
- default is application/json
```
