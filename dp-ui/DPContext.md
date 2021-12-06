# DPContext

The goal of DPContext is to allow developers to develop as quickly as possible using the tools most familiar to them. The only requirement for building packages/modules is that you wrap your UI with React Components.

## Usage

DPContext injects props into your component that we consider "important enough" that all components have access to do for basic dataprofiler development

```
import React from 'react'
import {DPContext} from 'dataprofiler-lib'

const MyComponent = props => {

  /* By wrapping your component with DPContext, the following props will be added to your component

  props.api -> 
    This is the dataprofiler api; preconfigured with the current users authentication/visibilities.
    Basic usage is `api.get({resource: 'v1/datasets'}).then(res => console.log(res.body))`
    The api is based off of superagent, and returns promises
    The full documentation can be found in ui/packages/dataprofiler-lib/src/api/README.md
  
  props.router -> 
    navigate -> 
      props.router.navigate('/foo/bar') will change the UI address to whatever you pass as an argument
    navigateHard -> 
      props.router.navigateHard('/foo/bar') will change the UI address to whatever you pass as an argument, but with a window.location replacement. This is useful when you're doing non-react things
    location -> 
      props.router.location is just the standard javascript location object

  props.dataprofiler -> 
    props.dataprofiler.isAdmin -> boolean value that says if the current user is an admin
    props.dataprofiler.canDownload -> boolean value that says if the current user is allowed to download
    props.dataprofiler.effectiveAttributes -> string array of all the visibility/capability/attributes for the user. If a user has refused visibilities, they will not appear in this list. You can easily check if a user has a visibility by doing `Boolean(props.dataprofiler.effectiveAttributes.includes("LIST.SOME_VISIBILITY"))`
    props.dataprofiler.state -> 
      app -> 
        initialized -> boolean value. do not do anything in your components until initialized is true 
        datasets -> this is an object that will have all the datasets a user has access to
      session -> 
        username -> string of the current username
        (there are many other session keys, but they are not for use in downstream components)
      preferences -> 
        general key,value pair object to store preferences. these will persist for a user in their browser, but are not saved centrally anywhere
    setDPState -> 
      (namespace: string, key: string, value: any) function that sets state for a single key. 
      for example props.dataprofiler.setDPState('preferences','enableBetaFeature',true) will set props.dataprofiler.state.preferences.enableBetaFeature to be true
    bulkSetDPState -> 
      (namespace: string, newState: object) same as setDPState, but instead of passing a single value, you pass a key value object, and it will get merged with the state
    setModal -> 
      (message: string, title: string) that will throw a modal window with an OK button in front of the entire application. Actions are not customizable
    setErrorModal -> 
      (message: string) that will throw a modal window in front of the entire application with an OK button. actions are not customizable
  */

  return ......
}

export default DPContext(MyComponent)
```

DPDownload is a separate context for the api to handle asynchronous file downloads

```
import React from 'react'
import {DPDownload} from 'dataprofiler-lib'

const MyComponent = props => {

  /* By wrapping your component with DPContext, the following props will be added to your component

  props.dataprofiler -> 
    props.dataprofiler.download -> 
      downloads -> collection of active downloads in object form
      addDownload -> (jobId: string) after submitting a job to the api, pass the job id to this function
      removeDownload -> (jobId: string) remove a download id from props.dataprofiler.download.downloads
      refreshDownload -> (jobId: string) calls the api to get updated job status and update the downloads collection
                         You shouldn't need to call refreshDownload manually, the download handler refreshes it every five seconds automatically
  */

  return ......
}

export default DPDownload(MyComponent)
```

## Basic Guidence

- Each package is responsible for managing it's own state. Do not store package-specific state in DPContext outside of perferences. We recommend using redux or react hooks.
- The parent-ui will define a 'root path' (ie parent-ui/treemap). You can build everything under the root, or you can extend your root path by using react-router-dom's <Switch> component. (ie parent-ui/treemap/foo and parent-ui/treemap/bar)

## URL's and Navigating

DPContext passes `router` into the root props. Use `router.navigate` to change pages.

```
import React from 'react'
import {DPContext} from 'dataprofiler-lib'

const MyLink = props => <div><a href onClick={() => props.router.navigate('/foo/bar/')}>My Link</a></div>

export default DPContext(MyLink)
```

If you want to use query params, please use the [use-query-params hook](https://github.com/pbeshai/use-query-params)


## D3

We recommend using a useRef hook for adding a D3 component. See ui/packages/dp-treemap/src/UniverseTreemap.jsx
