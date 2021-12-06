export default {
  niceifyNumber: (num) => {
    if (num === null || num === undefined) {
      return null
    } // terminate early
    if (num === 0) {
      return '0'
    } // terminate early
    if (num < 100) {
      return `${num}`
    } // allow small numbers
    const fixed = 0
    const b = num.toPrecision(2).split('e') // get power
    // floor at decimals, ceiling at trillions
    const k = b.length === 1 ? 0 : Math.floor(Math.min(b[1].slice(1), 14) / 3)
    // divide by power
    const c =
      k < 1 ? num.toFixed(0 + fixed) : (num / 10 ** (k * 3)).toFixed(1 + fixed)
    const d = c < 0 ? c : Math.abs(c) // enforce -0 is 0
    const e = d + ['', 'K', 'M', 'B', 'T'][k] // append power
    return e
  },
  commafyNumber: (num) => num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ','),
  determineForegroundColorFromBackground: (backgroundColor) => {
    const hexColor = backgroundColor.replace(/^.*#/, '')
    const r = parseInt(hexColor.substr(0, 2), 16)
    const g = parseInt(hexColor.substr(2, 2), 16)
    const b = parseInt(hexColor.substr(4, 2), 16)
    const yiq = (r * 299 + g * 587 + b * 114) / 1000
    return yiq >= 100 ? '#161B21' : '#FFF'
  },
  shadeColor: (color, userPct) => {
    const percent = userPct || -0.2
    const f = parseInt(color.slice(1), 16)
    const t = percent < 0 ? 0 : 255
    const p = percent < 0 ? percent * -1 : percent
    const R = f >> 16 // eslint-disable-line no-bitwise
    const G = (f >> 8) & 0x00ff // eslint-disable-line no-bitwise, no-mixed-operators
    const B = f & 0x0000ff // eslint-disable-line no-bitwise
    return `#${(
      0x1000000 +
      (Math.round((t - R) * p) + R) * 0x10000 +
      (Math.round((t - G) * p) + G) * 0x100 +
      (Math.round((t - B) * p) + B)
    )
      .toString(16)
      .slice(1)}` // eslint-disable-line no-bitwise, no-mixed-operators
  },
  truncate: (text, num) => {
    const filterNum = num || 40
    const filterText = text || ''
    if (filterText.length < filterNum) return text
    return `${filterText.substring(0, filterNum - 3)}...`
  },
  hexToRgbA: (hex, a) => {
    let c
    if (/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)) {
      c = hex.substring(1).split('')
      if (c.length === 3) {
        c = [c[0], c[0], c[1], c[1], c[2], c[2]]
      }
      c = `0x${c.join('')}`
      return `rgba(${[(c >> 16) & 255, (c >> 8) & 255, c & 255].join(
        ','
      )},${a})` // eslint-disable-line no-bitwise
    }
    throw new Error('Bad Hex')
  },
  hexToRgb: (hex) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result
      ? `(${parseInt(result[1], 16)},${parseInt(result[2], 16)},${parseInt(
          result[3],
          16
        )},0.15)`
      : '(255,255,255,1)' // eslint-disable-line max-len
  },
  prettySize: (size, config) => {
    // lifted from npm module prettysize
    const sizes = ['Bytes', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB']
    const { nospace, one, places } = config
    let mysize

    sizes.forEach((unit, id) => {
      if (one) {
        unit = unit.slice(0, 1)
      }
      const s = Math.pow(1024, id) // eslint-disable-line no-restricted-properties
      let fixed
      if (size >= s) {
        fixed = String((size / s).toFixed(places))
        if (fixed.indexOf('.0') === fixed.length - 2) {
          fixed = fixed.slice(0, -2)
        }
        mysize = fixed + (nospace ? '' : ' ') + unit
      }
    })

    // zero handling
    // always prints in Bytes
    if (!mysize) {
      const unit = one ? sizes[0].slice(0, 1) : sizes[0]
      mysize = `0${nospace ? '' : ' '}${unit}`
    }

    return mysize
  },
  middleTruncate: (fullStr, strLen, separator) => {
    const theStrLen = strLen || 20
    if (fullStr.length <= theStrLen) return fullStr
    const theSeparator = separator || '...'
    const sepLen = theSeparator.length
    const charsToShow = theStrLen - sepLen
    const frontChars = Math.ceil(charsToShow / 2)
    const backChars = Math.floor(charsToShow / 2)
    return `${fullStr.substr(0, frontChars)}${theSeparator}${fullStr.substr(
      fullStr.length - backChars
    )}`
  },
  capitalize: (s) => {
    if (typeof s !== 'string') return ''
    return s.charAt(0).toUpperCase() + s.slice(1)
  },
}
