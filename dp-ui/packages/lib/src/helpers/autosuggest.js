import { trimOrEmpty, rewriteLabelToNull } from './strings'
import { groupBy, sum, take, orderBy } from 'lodash'

/**
 * format auto suggestion filter results, take the top N
 * @param {*} data
 * @param {*} takeTopN
 */
export const aggregateSortAndTake = (
  data,
  takeTopN = 250,
  labelName = 'values'
) => {
  const grouped = groupBy(data, (e) => trimOrEmpty(e.value))
  const reduced = Object.keys(grouped).reduce((acc, value) => {
    const summation = sum(grouped[value].map((e) => e.count))
    return [
      ...acc,
      {
        value: rewriteLabelToNull(value),
        label: `${value} (${summation} ${labelName})`,
        labelName,
        summation,
        underlyingElements: grouped[value],
      },
    ]
  }, [])
  return take(orderBy(reduced, 'summation', 'desc'), takeTopN)
}
