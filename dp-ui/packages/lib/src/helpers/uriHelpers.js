import QS from 'query-string'
import { omit } from 'lodash'

const currentPage = () => window.location.pathname.split('/')[1]

export const encodeIdentifiers = (identifiers) =>
  identifiers.map((identifier) => encodeURIComponent(identifier)).join('/')

export const encodeQueryParams = (queryParams) => QS.stringify(queryParams)

export const parseQueryString = (rawQueryString) => {
  const parseableString =
    rawQueryString[0] === '?' ? rawQueryString.substr(1) : rawQueryString
  return QS.parse(parseableString)
}

export const updateQueryParam = (key, value) => {
  // legacy - please use the use-query-params hook if you're doing something new
  let existing = parseQueryString(window.location.search)
  if (value) {
    existing[key] = value
  } else {
    existing = omit(existing, key)
  }
  if (Object.keys(existing).length > 0) {
    window.history.replaceState(
      {},
      '',
      `${window.location.pathname}?${encodeQueryParams(existing)}`
    )
  } else {
    window.history.replaceState({}, '', window.location.pathname)
  }
  return true
}

export const externalRedirect = (newPath) => {
  console.log(
    `External Redirect Occurring`,
    `Currently on ${currentPage()}`,
    `Redirecting to ${newPath}`
  )
  window.location.replace(newPath)
}
