/*
  Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 
 	Licensed to the Apache Software Foundation (ASF) under one
 	or more contributor license agreements. See the NOTICE file
 	distributed with this work for additional information
 	regarding copyright ownership. The ASF licenses this file
 	to you under the Apache License, Version 2.0 (the
 	"License"); you may not use this file except in compliance
 	with the License. You may obtain a copy of the License at
 
 	http://www.apache.org/licenses/LICENSE-2.0
 
 
 	Unless required by applicable law or agreed to in writing,
 	software distributed under the License is distributed on an
 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 	KIND, either express or implied. See the License for the
 	specific language governing permissions and limitations
 	under the License.
*/
import { Button } from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { isUndefined } from 'lodash'
import React, { Fragment } from 'react'
import ColumnCounts from '../../models/ColumnCounts'
import SelectedDrilldown from '../../drilldown/models/SelectedDrilldown'
import ValueCount from '../../models/ValueCount'
import AggregatedSearchResult from '../../MultiSearch/models/AggregatedSearchResult'
import MultisearchService from '../../MultiSearch/services/MultisearchService'
import AggregatedSearchResultService from '../../services/AggregatedSearchResultService'
import { ColumnCountsService } from '../../services/ColumnCountsService'
import HighlightSentence from '../HighlightSentence'
import PluralizableText from '../PluralizableText'
import ColumnValueRow from './ColumnValueRow'
import { FixedSizeList } from 'react-window'

// props from parent
export interface Props {
  classes: Record<string, any>
  refreshColCounts: (drilldown: SelectedDrilldown) => void
  columnCounts: Readonly<ColumnCounts>
  drilldown: Readonly<SelectedDrilldown>
  filter: Readonly<AggregatedSearchResult>
  hovered?: Readonly<AggregatedSearchResult>
  highlightMatches?: Readonly<string[]>
  numValues?: number
  numUniqueValues?: number
}
interface State {}

/**
 * Component to display the search results and colum counts values
 *  used in a preview drawer, also in the list view column drilldown
 */
class ColumnValueList extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  generateRow(
    el: ValueCount,
    uniqKey?: string,
    relativePercentCount?: number,
    style?: object
  ): JSX.Element {
    return (
      <ColumnValueRow
        style={style}
        filter={this.props.filter}
        hovered={this.props.hovered}
        highlightMatches={this.props.highlightMatches}
        numValues={this.props.numValues}
        relativePercentCount={relativePercentCount}
        key={uniqKey}
        uniqKey={uniqKey}
        rowValue={el}></ColumnValueRow>
    )
  }

  generateTopValueRow(el: ValueCount): JSX.Element {
    const { classes } = this.props
    return (
      <div style={{ marginBottom: '16px' }}>
        <h4 className={classes.header}>Top value</h4>
        {this.generateRow(el, 'top-value')}
      </div>
    )
  }

  generateAggregateValueRow(hits: AggregatedSearchResult): JSX.Element {
    const { classes, numValues } = this.props
    if (!hits || !hits?.elements || hits?.count < 1) {
      return <Fragment />
    }

    return (
      <Fragment>
        <h4 className={classes.header}>Value search results</h4>
        <div>
          {this.generateRow(
            { value: hits?.value, count: hits?.count },
            'aggregate-value',
            numValues
          )}
        </div>
        <br />
      </Fragment>
    )
  }

  generateTitleHitRows(hits: AggregatedSearchResult): JSX.Element {
    if (!hits || !hits?.elements || hits?.count < 1) {
      return <Fragment />
    }

    const { classes, highlightMatches } = this.props
    const extraMatches = highlightMatches || []
    return (
      <Fragment>
        <h4 className={classes.header}>Title search results</h4>
        <div className={classes.titleSearchResultsContainer}>
          {hits.elements.map((el, i) => (
            <span
              key={`title-hit-${i}`}
              className={classes.titleSearchResultRow}>
              "
              <HighlightSentence
                content={el.value}
                matchTerms={[hits?.value, ...extraMatches]}
                useBold={false}
              />
              " was found in title
            </span>
          ))}
        </div>
        <br />
        <br />
      </Fragment>
    )
  }

  filterHitsToDrilldown(): AggregatedSearchResult {
    const { drilldown, filter, hovered } = this.props
    // pick up only the hits related to this drilldown
    const service = new MultisearchService()
    const tmpHits = filter || hovered
    return service.filterAndSum(tmpHits, drilldown)
  }

  loadMoreNote(numValuesShown: number, numUniqueValues: number): JSX.Element {
    const { classes } = this.props
    return (
      <div className={classes.note}>
        <sub>
          <i>
            *Note: showing {numValuesShown} out of {numUniqueValues} values.
          </i>
        </sub>
      </div>
    )
  }

  render(): JSX.Element {
    const {
      classes,
      drilldown,
      columnCounts,
      numUniqueValues,
      highlightMatches,
    } = this.props
    const hits = this.filterHitsToDrilldown()
    const aggregatedResultsService = new AggregatedSearchResultService()
    const nonTitleHits = aggregatedResultsService.filterNoneTitleHits(hits)
    const titleHits = aggregatedResultsService.filterForTitleHits(hits)
    const topValue = { ...columnCounts?.values[0] }
    const globalSearchFilters = !isUndefined(nonTitleHits?.value)
      ? [nonTitleHits?.value]
      : []
    const localFilter = highlightMatches || []
    // use the search results along with the values found in column counts
    // this makes sure we show match term hits, eg. if a hit is not in the col count list
    // it will not make it to the panel
    // and the colcounts will not contain all the values since it could be a large list
    const service = new ColumnCountsService()
    const withSearchHits = service.uniqWithSearchResults(
      columnCounts,
      nonTitleHits,
      drilldown
    )
    // rerank based on global matches and number of hits
    const partitions = service.rerank(
      withSearchHits,
      localFilter,
      globalSearchFilters
    )
    const localAndGlobalMatch = partitions['HAS_LOCAL_AND_GLOBAL_MATCH']
    const localMatch = partitions['HAS_LOCAL_MATCH_ONLY']
    const globalMatch = partitions['HAS_GLOBAL_MATCH_ONLY']
    // filter out nonmatches
    const nonMatch = service.filter(partitions['NO_MATCH'], localFilter[0])

    if (
      !localAndGlobalMatch?.values &&
      !localMatch?.values &&
      !globalMatch?.values &&
      !nonMatch?.values
    ) {
      return <Fragment></Fragment>
    }
    const localAndGlobalMatchValues = localAndGlobalMatch?.values || []
    const localMatchValues = localMatch?.values || []
    const globalMatchValues = globalMatch?.values || []
    const nonmatchedValues = nonMatch?.values || []
    const numberValuesShown =
      localAndGlobalMatchValues.length +
      localMatchValues.length +
      globalMatchValues.length +
      nonmatchedValues.length

    // dedup global and local filter terms to simplify what we display to the user
    // const displayedTrueLocalFilter = service.dedupLocalSearchTerms(
    //   highlightMatches,
    //   globalSearchFilters
    // )
    const LOAD_MORE_RESULTS_HEADER =
      'Partial results shown. Click load more to see additional results.'
    return (
      <div className={classes.root}>
        {this.generateTopValueRow(topValue)}
        <br />
        {this.generateAggregateValueRow(nonTitleHits)}
        {this.generateTitleHitRows(titleHits)}
        {localAndGlobalMatchValues.length > 0 && (
          <h4>
            {/* "{hits?.value}"{' '}
            {!isEmpty(displayedTrueLocalFilter)
              ? ` and "${displayedTrueLocalFilter}"`
              : ''}{' '}
            contained in {localAndGlobalMatchValues.length}{' '}
            <PluralizableText
              pluralizableLabel={'result'}
              count={localAndGlobalMatchValues.length}
            /> */}
            {LOAD_MORE_RESULTS_HEADER}
          </h4>
        )}
        {localAndGlobalMatchValues.map((el, i) =>
          this.generateRow(el, `a-${i}`, topValue?.count)
        )}
        {localAndGlobalMatchValues.length > 0 && <br />}

        {localMatchValues.length > 0 && (
          <h4>
            {/* "{localFilter}" contained in {localMatchValues.length}{' '}
            <PluralizableText
              pluralizableLabel={'result'}
              count={localMatchValues.length}
            /> */}
            {LOAD_MORE_RESULTS_HEADER}
          </h4>
        )}
        {localMatchValues.map((el, i) =>
          this.generateRow(el, `b-${i}`, topValue?.count)
        )}
        {localMatchValues.length > 0 && <br />}

        {globalMatchValues.length > 0 && (
          <h4>
            "{nonTitleHits?.value}" contained in {globalMatchValues.length}{' '}
            <PluralizableText
              pluralizableLabel={'result'}
              count={globalMatchValues.length}
            />
          </h4>
        )}
        {globalMatchValues.map((el, i) =>
          this.generateRow(el, `c-${i}`, topValue?.count)
        )}
        {globalMatchValues.length > 0 && <br />}

        {numberValuesShown < numUniqueValues &&
          this.loadMoreNote(numberValuesShown, numUniqueValues)}
        {/* {nonmatchedValues.length > 0 && <h4>Nonmatched values</h4>} */}
        <FixedSizeList
          height={
            58 * (nonmatchedValues.length < 12 ? nonmatchedValues.length : 12)
          }
          width="100%"
          itemCount={nonmatchedValues.length}
          itemSize={58}
          style={{
            maxHeight: `calc(125vh - ${
              numberValuesShown < numUniqueValues ? '739' : '601'
            }px)`, // 739 and 601 are the points where the list no longer overflows the card height
          }}>
          {({ index, style }) =>
            this.generateRow(
              nonmatchedValues[index],
              `d-${index}`,
              topValue?.count,
              style
            )
          }
        </FixedSizeList>
        {/* 
        add a load more or infinite scroll option, 
        since we cant load all col count values at once, it might be a very large list
         */}
        {numberValuesShown < numUniqueValues && (
          <div>
            {this.loadMoreNote(numberValuesShown, numUniqueValues)}
            <p>
              <Button
                className={classes.fullWidth}
                onClick={(e) => this.props.refreshColCounts(drilldown)}
                variant="outlined">
                Load More
              </Button>
            </p>
          </div>
        )}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
// width: 'calc(100vw - 68px)',
// marginLeft: 'auto',
const styles = (theme: Theme) =>
  createStyles({
    root: {
      // position: 'absolute',
      // left: 0,
      // top: 0,
      // right: 0,
      // overflow: 'auto',
    },
    fullWidth: {
      width: '100%',
    },
    header: {
      marginTop: '12px',
      marginBottom: '12px',
    },
    titleSearchResultsContainer: {
      display: 'flex',
      flexDirection: 'column',
      paddingBottom: 8,
    },
    titleSearchResultRow: {
      marginTop: 12,
    },
    note: {
      marginBottom: 18,
    },
  })

export default withStyles(styles)(ColumnValueList)
