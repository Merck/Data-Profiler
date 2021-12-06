import { isNil } from 'lodash'

/**
 * return the given value with trimmed whitespace or a given default value if undefined
 * @param {*} val
 * @param {*} defaultVal
 * @return {String}
 */
export const trimOrDefault = (val, defaultVal) =>
  val !== undefined ? val.trim() : defaultVal

/**
 * return the given value with trimmed whitespace or an empty string if undefined
 * @param {*} val
 * @return {String}
 */
export const trimOrEmpty = (val) => trimOrDefault(val, '')

/**
 * return the given value as lower case or an empty string if undefined
 * @param {*} val
 * @return {String}
 */
export const lowerCaseOrEmpty = (val) => (val || '').toLowerCase()

/**
 * if given value is nil or empty string, give a more sane lable of '<null>'
 * @param {*} val
 * @return {String}
 */
export const rewriteLabelToNull = (val) =>
  isNil(val) || val === '' ? '<null>' : val

/**
 * remove sql escaped quotes, as well as surrounding quotes
 * this \"is\" foo => this "is" foo
 * "this "is" foo" => this "is" foo
 * @param {String}
 * @return {String}
 */
export const unescapeQuotes = (val) =>
  val
    .replace(/\\"/g, '"')
    .replace(/\\'/g, '"')
    .replace(/^"(.*)"$/, '$1')

/**
 * Return the given value URI encoded with escaping for %2F
 * @param {String} variable
 * @returns {String}
 */
export const encodeVariable = (variable) =>
  encodeURIComponent(variable).replace('%2F', '%252F')
