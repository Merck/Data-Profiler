import React, { createContext, useReducer } from 'react'
import api from '../api'
import { includes, omit, isEmpty } from 'lodash'
import {
  useMemoizedReducer,
  hydrate,
  persistingDispatch,
} from '../helpers/reducers'
import { writeStorage } from '@rehooks/local-storage'

interface DataprofilerContext {
  isAdmin: boolean
  canDownload: boolean
  effectiveAttributes: string[]
  state: {
    app: {}
    session: {
      authToken: string
      username: string
    }
    preferences: {}
  }
  setDPState: any
  bulkSetDPState: any
  setModal: any
  setErrorModal: any
  api: any
  router: any
  history: any
}

const initialAppState = {
  initialized: false,
  datasets: null,
}

const initialSessionState = { authToken: null, username: null }
const initialPreferencesState = {}

const assigningReducer = (state, action) => ({ ...state, ...action })

export const setModal = (message, title = null) => {
  writeStorage('modal', { message, title })
}

export const setErrorModal = (error) => {
  let message = `. Please refresh the page and try again.`

  if (typeof error === 'string') {
    message = `${error}${message}`
  } else {
    console.error(error)
    message = `${error.toString()}${message}`
  }
  setModal(message, 'Application Error')
}

export const SetDPContext = ({ children, history }) => {
  const [session, sessionDispatch, getSession] = useMemoizedReducer(
    persistingDispatch(assigningReducer, 'session'),
    hydrate('session', initialSessionState)
  )
  const [preferences, preferencesDispatch] = useReducer(
    persistingDispatch(assigningReducer, 'preferences'),
    hydrate('preferences', initialPreferencesState)
  )
  const [app, appDispatch] = useMemoizedReducer(
    assigningReducer,
    initialAppState
  )

  window['dataprofiler'] = { getSession, sessionDispatch }

  const setKVInState = (namespace: string, key: string, value: any) => {
    if (namespace === 'session') {
      sessionDispatch({ [key]: value })
    } else if (namespace === 'preferences') {
      preferencesDispatch({ [key]: value })
    } else if (namespace === 'app') {
      appDispatch({ [key]: value })
    } else {
      throw new Error(`Save State Error: Bad Namespace ${name}`)
    }
  }

  const bulkSetKVInState = (namespace: string, obj: any) => {
    if (namespace === 'session') {
      sessionDispatch(obj)
    } else if (namespace === 'preferences') {
      preferencesDispatch(obj)
    } else if (namespace === 'app') {
      appDispatch(obj)
    } else {
      throw new Error(`Save State Error: Bad Namespace ${name}`)
    }
  }

  const setDPState = (namespace: string, key: string, value: any) =>
    setKVInState(namespace, key, value)

  const bulkSetDPState = (namespace: string, obj: any) =>
    bulkSetKVInState(namespace, obj)

  const router = {
    location: {
      ...window.location,
      path: window.location.pathname.replace(/^\//, ''),
    },
    navigate: (path: string) => history.push(path),
    navigateHard: (path: string) =>
      isEmpty(process.env.PUBLIC_URL)
        ? window.location.replace(path)
        : window.location.replace(`${process.env.PUBLIC_URL}${path}`),
  }

  const cleanAttributesToReject = (
    (session && session.attributesToReject) ||
    []
  ).map((e) => e.toUpperCase())

  const cleanSystemAttributes = (
    (session && session.systemAttributes) ||
    []
  ).map((e) => e.toUpperCase())

  const effectiveAttributes = cleanSystemAttributes.filter(
    (el) => !cleanAttributesToReject.includes(el)
  )

  const effectiveSystemAttributes = effectiveAttributes.filter((el) =>
    el.startsWith('SYSTEM.')
  )

  const dataProfilerContext: DataprofilerContext = {
    state: {
      app,
      session,
      preferences,
    },
    effectiveAttributes: effectiveAttributes,
    isAdmin: includes(effectiveSystemAttributes, 'SYSTEM.ADMIN'),
    canDownload: includes(effectiveSystemAttributes, 'SYSTEM.DOWNLOAD'),
    setDPState,
    bulkSetDPState,
    setModal,
    setErrorModal,
    api: api(
      session.username,
      session.authToken,
      preferences.attributesToReject
    ),
    router,
    history,
  }

  return (
    <DPContext.Provider value={dataProfilerContext}>
      {children}
    </DPContext.Provider>
  )
}

export const DPContext = createContext({})

interface ContextConfig {
  requireAdmin: boolean
}

const defaultConfig: ContextConfig = {
  requireAdmin: false,
}

export default (InputComponent, config = defaultConfig) =>
  (props) =>
    (
      <DPContext.Consumer>
        {(dataProfilerContext: DataprofilerContext) => {
          if (config.requireAdmin && !dataProfilerContext.isAdmin)
            return <div>Access Denied: You Must be an Admin</div>
          return (
            <InputComponent
              {...props}
              api={dataProfilerContext.api}
              router={dataProfilerContext.router}
              history={dataProfilerContext.history}
              dataprofiler={{
                ...props.dataprofiler,
                ...omit(dataProfilerContext, ['api', 'router', 'history']),
              }}
            />
          )
        }}
      </DPContext.Consumer>
    )
