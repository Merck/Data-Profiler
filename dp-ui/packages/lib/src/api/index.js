import request from 'superagent'
import intercept from 'superagent-intercept'
import { isEqual, get, isEmpty } from 'lodash'
import QS from 'query-string'
import superThrottle from './superThrottle'
import { setErrorModal } from '../context/DPContext'

/*
  TODO - Refactor this trashpile of a file.
*/

const { USER_FACING_API_HTTP_PATH, PUBLIC_URL } = process.env

const BASE_URL = USER_FACING_API_HTTP_PATH || 'http://localhost:9000'
// const BASE_URL = 'http://10.0.2.2:9000' // This is for running on virtualbox
const AUTH_TOKEN_HEADER = 'X-Authorization-Token'
const USERNAME_HEADER = 'X-Username'
const SYSTEM_ATTRIBUTES_HEADER = 'X-Assigned-Attributes'
const ATTRIBUTES_TO_REJECT_HEADER = 'X-Attributes-To-Reject'
const SPECIAL_ATTRIBUTES_HEADER = 'X-Special-Attributes-To-Apply'

const deadline = 120000

const navigateHard = (path) =>
  isEmpty(PUBLIC_URL)
    ? window.location.replace(path)
    : window.location.replace(`${PUBLIC_URL}${path}`)

const encodeIdentifiers = (identifiers) =>
  identifiers.map((identifier) => encodeURIComponent(identifier)).join('/')
const encodeQueryParams = (queryParams) => QS.stringify(queryParams)
const currentPage = () => window.location.pathname.split('/')[1]

const authenticationInterceptor = intercept((err, res) => {
  if (err) console.log(err)
  if (res) {
    if (
      res.unauthorized &&
      !['authcallback', 'login'].includes(currentPage())
    ) {
      navigateHard(`/logout`)
    } else if (res.forbidden && res.body) {
      setErrorModal(res.body)
    }
  }
})

const visibilityInterceptor = intercept((_, res) => {
  // When we download from S3, apiReq will be false
  const apiReq =
    get(res, 'req.url', BASE_URL).startsWith(BASE_URL) &&
    !get(res, 'req.url', BASE_URL).startsWith(`${BASE_URL}/_`)

  if (res && res.ok && apiReq) {
    const { getSession, sessionDispatch } = window['dataprofiler'] || {
      getSession: undefined,
      sessionDispatch: undefined,
    }
    if (getSession && sessionDispatch) {
      const systemAttributes = getSession()['systemAttributes']
      let apiSystemAttributes = []
      try {
        apiSystemAttributes = JSON.parse(
          res.header[SYSTEM_ATTRIBUTES_HEADER.toLowerCase()]
        ).sort()
        checkRedirectToLandingPage(apiSystemAttributes)
      } catch (e) {}
      if (!isEqual(systemAttributes, apiSystemAttributes)) {
        sessionDispatch({ systemAttributes: apiSystemAttributes })
      }
    }
  }
})

const checkRedirectToLandingPage = (attrs) => {
  if (isEqual('/landing', location.pathname)) {
    console.log('skip landing page redirect check')
    return
  }
  // viz are not empty and are missing seen_joyride
  const shouldShowLandingPage =
    !isEmpty(attrs) &&
    !attrs.map((el) => el.toLowerCase()).includes('system.seen_joyride')
  if (shouldShowLandingPage) {
    console.log('redirecting to /landing')
    navigateHard('/landing')
  }
}

const errorDefinition = (res) => res.status < 500 && res.status !== 400

const throttle = new superThrottle({
  active: true,
  rate: 1,
  ratePer: 1000,
  concurrent: 1,
})

export const formURI = (reqParams) => {
  const { resource, identifiers, queryParams, rawUrl } = reqParams
  if (rawUrl) {
    return rawUrl
  }
  const qps = queryParams ? `?${encodeQueryParams(queryParams)}` : ''
  const ret = identifiers
    ? `${BASE_URL}/${resource}/${encodeIdentifiers(identifiers)}${qps}`
    : `${BASE_URL}/${resource}${qps}`
  return ret.trim()
}

const setReq = (
  req,
  reqParams = {},
  username,
  authToken,
  attributesToReject
) => {
  const {
    specialVisibilities,
    timeout,
    useThrottle,
    postObject,
    isDownloadable,
    withoutAuth,
    overrideContentType,
  } = reqParams
  req.ok(errorDefinition)
  req.timeout(timeout ? { deadline: timeout } : { deadline })
  if (!withoutAuth) {
    if (!username || !authToken) {
      navigateHard('/logout')
    }
    req.use(authenticationInterceptor)
    req.use(visibilityInterceptor)
    req.set(ATTRIBUTES_TO_REJECT_HEADER, JSON.stringify(attributesToReject))
    req.set(USERNAME_HEADER, username)
    req.set(AUTH_TOKEN_HEADER, authToken)
  }
  if (specialVisibilities) {
    req.set(SPECIAL_ATTRIBUTES_HEADER, JSON.stringify(specialVisibilities))
  }
  if (overrideContentType) {
    req.set('Accept', overrideContentType)
    req.set('Content-Type', overrideContentType)
  }
  if (useThrottle) {
    req.use(throttle.plugin(null))
  }
  if (postObject) {
    req.send(postObject)
  }
  if (isDownloadable) {
    req.responseType('blob')
  }
  return req
}

const loadFromLocalStorage = (key) => {
  const getSession =
    (window['dataprofiler'] && window['dataprofiler'].getSession) || undefined
  if (getSession) {
    return getSession()[key]
  }
  const ls = localStorage.getItem('session')
  if (!ls) return null
  return JSON.parse(ls)[key]
}

const api = (_username, _authToken, _attributesToReject) => {
  const username = _username || loadFromLocalStorage('username')
  const authToken = _authToken || loadFromLocalStorage('authToken')
  const attributesToReject =
    _attributesToReject || loadFromLocalStorage('attributesToReject') || []

  return {
    base_url_path: BASE_URL, // eslint-disable-line @typescript-eslint/camelcase
    head: (reqParams) => {
      const head = request.head(formURI(reqParams))
      return setReq(head, reqParams, username, authToken, attributesToReject)
    },
    get: (reqParams) => {
      const get = request.get(formURI(reqParams))
      return setReq(get, reqParams, username, authToken, attributesToReject)
    },
    delete: (reqParams) => {
      const del = request.delete(formURI(reqParams))
      return setReq(del, reqParams, username, authToken, attributesToReject)
    },
    put: (reqParams) => {
      const put = request.put(formURI(reqParams))
      return setReq(put, reqParams, username, authToken, attributesToReject)
    },
    post: (reqParams) => {
      const post = request.post(formURI(reqParams))
      return setReq(post, reqParams, username, authToken, attributesToReject)
    },
  }
}

export default api
