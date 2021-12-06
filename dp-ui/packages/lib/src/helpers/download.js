import api from '../api'
import { uniqBy, isEmpty } from 'lodash'

export const doDownloadBlob = (data, name) =>
  doDownload(URL.createObjectURL(data), name)

export const doDownload = (link, name = null) => {
  const downloadAnchorNode = document.createElement('a')
  // downloadAnchorNode.setAttribute('target', "_blank")
  downloadAnchorNode.setAttribute('href', link)
  downloadAnchorNode.setAttribute('hidden', true)
  if (name) {
    downloadAnchorNode.setAttribute('download', name)
  }
  document.body.appendChild(downloadAnchorNode) // required for firefox
  downloadAnchorNode.click()
  downloadAnchorNode.remove()
}

export const downloadJsObj = (storageObj, name) => {
  const dataStr =
    'data:text/json;charset=utf-8,' +
    encodeURIComponent(JSON.stringify(storageObj))
  doDownloadBlob(dataStr, name)
}

export const downloadKVPArrayAsCsv = (storageObj, name) => {
  // this should be an array of objects with similar keys
  // [{foo:1, bar: 2},{foo:3, bar: 4}]
  if (!storageObj || storageObj.length === 0) {
    return
  }
  const headers = Object.keys(storageObj[0])
  const body = storageObj.map((row) => headers.map((head) => row[head]))
  const dataStr =
    'data:text/csv;charset=utf-8,' +
    headers.join(',') +
    '\n' +
    body.map((e) => e.join(',')).join('\n')
  doDownloadBlob(dataStr, name)
}

export const transformS3PresignedToProxy = (url, name = null) => {
  const { base_url_path } = api()
  const urlObj = new URL(url)
  const bucket = urlObj.hostname
  const path = urlObj.href.substring(
    urlObj.href.indexOf(bucket) + bucket.length + 1
  )
  return {
    originalUrl: url,
    proxiedUrl: `${base_url_path}/s3/${bucket}/${path}`,
  }
}

export const transformS3PresignedToProxyAndDownload = (url, name = null) => {
  const urls = transformS3PresignedToProxy(url, name)

  // the api proxies the s3 calls via "proxiedUrl". The goal is to try the
  // proxied url first, but if we're not running in nginx, we default to try
  // the raw direct to s3 url.
  // S3 returns a 403 on HEAD, our API returns 404

  return api()
    .head({ rawUrl: urls.proxiedUrl })
    .ok((res) => res.status < 500)
    .then((res) => {
      res.status === 404
        ? doDownload(urls.originalUrl, name)
        : doDownload(urls.proxiedUrl, name)
    })
}

export const transformS3PresignedToProxyAndDownloadLocal = (
  url,
  name = null
) => {
  const urls = transformS3PresignedToProxy(url, name)

  // the api proxies the s3 calls via "proxiedUrl". The goal is to try the
  // proxied url first, but if we're not running in nginx, we default to try
  // the raw direct to s3 url.
  // S3 returns a 403 on HEAD, our API returns 404

  return api()
    .head({ rawUrl: urls.proxiedUrl })
    .ok((res) => res.status < 500)
    .then((res) => (res.status === 404 ? urls.originalUrl : urls.proxiedUrl))
    .then((rawUrl) => api().get({ rawUrl, isDownloadable: true }))
    .then((res) => doDownloadBlob(res.body, name))
}
