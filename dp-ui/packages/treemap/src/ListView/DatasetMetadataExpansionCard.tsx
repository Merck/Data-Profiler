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
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
} from '@material-ui/core/'
import { createStyles, withStyles } from '@material-ui/core/styles'
import ExpandMoreIcon from '@material-ui/icons/ExpandMore'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isBoolean, isUndefined } from 'lodash'
import moment from 'moment'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import DatasetChip from '../components/DatasetChip'
import HighlightSentence from '../components/HighlightSentence'
import PluralizableText from '../components/PluralizableText'
import { ACTION_BTN_COLOR, LIGHT_GRAY, TRUE_BLACK } from '../dpColors'
import { DATE_FMT } from '../dpDateFormats'
import { LISTVIEW_TABS_FEATURE_FLAG } from '@dp-ui/parent/src/features'
import { StoreState } from '../index'
import CommonMetadata from '../models/CommonMetadata'
import ReorderCommonMetadata from '../models/ReorderCommonMetadata'
import AggregatedSearchResult from '../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../nicefyNumber'
import {
  clearColumnCounts,
  clearColumnSamples,
  clearListViewDrilldown,
  setListViewDrilldown,
  getMetadataProperties,
  setFetchingProperties,
  fetchQualityTabComments,
  clearQualityTabComments,
} from './actions'
import { updateDatasetMetadata } from '../Drawers/actions'
import DetailsPanel from './details/DetailsPanel'
import SelectedDrilldown from '../drilldown/models/SelectedDrilldown'
import TabbedPanels from './TabbedPanels'
import {
  DEFAULT_CELL_COLOR,
  searchFrequencyColorRange,
} from '../searchHitColors'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
  tableMetadata?: Readonly<CommonMetadata[]>
  drilldown: Readonly<SelectedDrilldown>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  inSearchMode: boolean
  searchToken?: string
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class DatasetMetadataExpansionCard extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  componentDidMount(): void {
    const { datasetMetadata } = this.props
    const datasetName = datasetMetadata.commonMetadata?.datasetName
    const shouldExpand = this.shouldExpand()
    const isMissingBlob = this.isMissingDatasetPropertiesBlob()

    if (shouldExpand) {
      this.props.fetchQualityTabComments(
        SelectedDrilldown.of({ dataset: datasetName }),
        5
      )

      if (isMissingBlob) {
        this.mergePropertiesIntoCurrentMetadata(datasetName)
      }
    }
  }

  shouldExpand(): boolean {
    const { drilldown, datasetMetadata } = this.props
    const curDataset = datasetMetadata.commonMetadata
    const selected = drilldown?.dataset
    const shouldExpand = curDataset?.datasetName === selected
    return shouldExpand
  }

  isMissingDatasetPropertiesBlob(): boolean {
    const { datasetMetadata } = this.props
    const isMissing =
      isUndefined(datasetMetadata) ||
      isUndefined(datasetMetadata.commonMetadata) ||
      isUndefined(datasetMetadata.commonMetadata.properties) ||
      isUndefined(datasetMetadata.commonMetadata.properties.performance)
    return isMissing
  }

  mergePropertiesIntoCurrentMetadata(dataset: string): any {
    const curFetching = this.props.fetchingProperties
    const curSelected = this.props.datasetMetadata.commonMetadata.datasetName

    if (curFetching === curSelected) {
      return false
    }

    this.props.setFetchingProperties(curSelected)

    getMetadataProperties(dataset).then((res) => {
      const curMeta = { ...this.props.datasetMetadata.commonMetadata }
      curMeta.properties = res.metadataByDatasetName.properties
      this.props.updateDatasetMetadata(curMeta)
      this.props.setFetchingProperties('')
    })
  }

  toggleExpansion(
    dataset: string,
    event?: React.ChangeEvent<any>,
    expanded?: boolean
  ): void {
    if (event) {
      event.preventDefault()
    }

    if (this.isMissingDatasetPropertiesBlob()) {
      this.mergePropertiesIntoCurrentMetadata(dataset)
    }

    if (isBoolean(expanded) && !expanded) {
      this.props.clearListViewDrilldown()
      this.props?.clearColumnSamples()
      this.props?.clearColumnCounts()
      this.props?.clearQualityTabComments()
      return
    }

    this.props.setListViewDrilldown(SelectedDrilldown.of({ dataset }))
  }

  shouldDisable(metadata: Readonly<ReorderCommonMetadata>): boolean {
    // no need to disable if we are not actively searching
    if (
      !metadata ||
      (isBoolean(this.props.inSearchMode) && !this.props.inSearchMode)
    ) {
      return false
    }
    return !(metadata.hasSearchHit || false)
  }

  // updateStateKey = (): string => {
  //   const { datasetMetadata, searchToken, drilldown, inSearchMode } = this.props
  //   const hasSearchHits = datasetMetadata.hasSearchHit
  //   const numHits = datasetMetadata.numHits || 0
  //   const { dataset, table, column } = drilldown
  //   const keys = {
  //     curDataset: datasetMetadata?.commonMetadata?.datasetName,
  //     inSearchMode,
  //     searchToken,
  //     hasSearchHits,
  //     numHits,
  //     dataset,
  //     table,
  //     column,
  //   }
  //   const jsonState = JSON.stringify(keys)
  //   const hash = md5(jsonState)
  //   // console.log('card hash ', hash)
  //   return hash
  // }

  render(): JSX.Element {
    const { filter, hovered, tableMetadata, inSearchMode } = this.props
    const { classes, datasetMetadata, searchToken, drilldown } = this.props
    const curDataset = datasetMetadata.commonMetadata
    const hasSearchHits = datasetMetadata.hasSearchHit
    const numHits = datasetMetadata.numHits || 0
    const shouldExpand = this.shouldExpand()
    const shouldDisable = !shouldExpand && this.shouldDisable(datasetMetadata)
    // const key = this.updateStateKey()
    return (
      <div className={classes.root}>
        <Accordion
          onChange={(e, expanded) =>
            this.toggleExpansion(curDataset.datasetName, e, expanded)
          }
          className={`${shouldDisable ? classes.disabled : ''} ${
            classes.accordianRoot
          }`}
          expanded={shouldExpand}
          disabled={shouldDisable}
          defaultExpanded={false}>
          <AccordionSummary
            expandIcon={<ExpandMoreIcon className={classes.actionBtn} />}>
            {hasSearchHits && (
              <div
                className={classes.searchTermTag}
                style={{ background: searchFrequencyColorRange(numHits) }}>
                {nicefyNumber(numHits).trim()}&nbsp;<span>{searchToken}</span>
                &nbsp;found
              </div>
            )}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                flex: '1',
                paddingBottom: 8,
              }}>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  flex: '1 1 500px',
                  minWidth: '40%',
                }}>
                <span
                  className={`${classes.full} ${classes.header}`}
                  style={{ marginBottom: 8 }}>
                  <HighlightSentence
                    content={
                      curDataset.datasetDisplayName ||
                      curDataset.datasetName ||
                      'n/a'
                    }
                    matchTerms={[searchToken]}
                    useBold={false}
                    willBreak={true}
                    willTruncate={true}></HighlightSentence>
                </span>
                <DatasetChip />
              </div>

              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  flex: '1 1 200px',
                }}>
                <span className={classes.statHeader}>Values</span>
                <span className={classes.statContent}>
                  {nicefyNumber(curDataset?.numValues)}
                </span>
              </div>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  flex: '1 1 200px',
                }}>
                <span className={classes.statHeader}>Updated Date</span>
                <span className={classes.statContent}>
                  {moment(curDataset?.updatedOn).format(DATE_FMT)}
                </span>
              </div>

              <div className={classes.subInfo}>
                <span className={classes.subtext}>
                  <PluralizableText
                    pluralizableLabel="Table"
                    count={curDataset.numTables}>
                    <span className={classes.mutatedInfo}>
                      ({nicefyNumber(curDataset.numTables).trim()})
                    </span>
                  </PluralizableText>
                </span>
                <span className={classes.divider}>|</span>
                <span className={classes.subtext}>
                  <PluralizableText
                    pluralizableLabel="Column"
                    count={curDataset.numColumns}>
                    <span className={classes.mutatedInfo}>
                      ({nicefyNumber(curDataset.numColumns).trim()})
                    </span>
                  </PluralizableText>
                </span>
                <span className={classes.divider}>|</span>
                <span className={classes.subtext}>
                  <PluralizableText
                    pluralizableLabel="Value"
                    count={curDataset.numRows}>
                    <span className={classes.mutatedInfo}>
                      ({nicefyNumber(curDataset.numValues).trim()})
                    </span>
                  </PluralizableText>
                </span>
              </div>
            </div>
          </AccordionSummary>
          <AccordionDetails className={classes.details}>
            {LISTVIEW_TABS_FEATURE_FLAG && (
              <TabbedPanels
                inSearchMode={inSearchMode}
                filter={filter}
                hovered={hovered}
                drilldown={drilldown}
                datasetMetadata={datasetMetadata}
                tableMetadata={tableMetadata}
              />
            )}
            {!LISTVIEW_TABS_FEATURE_FLAG && (
              <DetailsPanel
                inSearchMode={inSearchMode}
                filter={filter}
                hovered={hovered}
                drilldown={drilldown}
                datasetMetadata={datasetMetadata}
                tableMetadata={tableMetadata}></DetailsPanel>
            )}
          </AccordionDetails>
        </Accordion>
      </div>
    )
  }
}

const REGULAR_FONT_WEIGHT = 400
// const MAX_CARD_WIDTH = 1440
const MAX_CARD_WIDTH = 1920
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    width: '100%',
    marginBottom: 16,
    maxWidth: `${MAX_CARD_WIDTH}px`,
  },
  accordianRoot: {
    width: '100%',
    overflow: 'hidden',
  },
  disabled: {
    background: '#eee',
  },
  full: {
    flex: '1 1 auto',
  },
  mutatedInfo: {
    color: LIGHT_GRAY,
  },
  subInfo: {
    flex: '0 0 auto',
    width: '30%',
    fontSize: '12pt',
    fontWeight: REGULAR_FONT_WEIGHT,
  },
  details: {
    flexDirection: 'column',
    // height: 'calc(125vh - 343px)',
    marginTop: 32,
  },
  statHeader: {
    fontSize: '12pt',
    fontWeight: 'bold',
    marginBottom: theme.spacing(1),
  },
  statContent: {
    fontSize: '16pt',
    fontWeight: '300',
  },
  divider: {
    marginLeft: theme.spacing.unit,
    marginRight: theme.spacing.unit,
    fontWeight: 300,
  },
  header: {
    fontSize: '24pt',
    fontWeight: '300',
  },
  subheader: {
    fontSize: theme.typography.pxToRem(14),
  },
  subtext: {
    minWidth: '125px',
  },
  // button: {
  //   margin: theme.spacing.unit,
  //   color: ACTION_BTN_COLOR,
  // },
  actionBtn: {
    color: ACTION_BTN_COLOR,
  },
  listItem: {
    flex: '1 1 415px',
  },
  listItemText: {
    paddingLeft: '16px !important',
    marginBottom: theme.spacing.unit,
  },
  tableContainer: {
    overflowX: 'scroll',
    overflowY: 'scroll',
    height: '40vh',
  },
  tableCell: {
    minWidth: 150,
    padding: theme.spacing.unit,
  },
  searchTermTag: {
    position: 'absolute',
    // left: '80%',
    right: '55px',
    top: '0px',
    // left: `${MAX_CARD_WIDTH * 0.8}px`,
    // maxWidth: '175px',
    maxWidth: '25%',
    minWidth: '10%',
    display: 'flex',
    zIndex: 500,
    borderRadius: '0 0 5px 5px',
    // background: CARD_BACKGROUND,
    background: DEFAULT_CELL_COLOR,
    color: TRUE_BLACK,
    fontSize: theme.typography.pxToRem(12),
    padding: '2px 8px',
    whiteSpace: 'nowrap',
    justifyContent: 'center',
    '& span': {
      textOverflow: 'ellipsis',
      overflow: 'hidden',
      // opacity: 0.5,
      fontStyle: 'italic',
    },
  },
}))

const mapStateToProps = (state: StoreState) => ({
  columnMetadata: state.treemap?.selectedTableColumnMetadata,
  samples: state.listview?.samples,
  columnCounts: state.listview?.colCounts,
  fetchingProperties: state.listview.fetchingProperties,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      clearListViewDrilldown,
      clearColumnSamples,
      clearColumnCounts,
      setListViewDrilldown,
      updateDatasetMetadata,
      setFetchingProperties,
      fetchQualityTabComments,
      clearQualityTabComments,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(DatasetMetadataExpansionCard))
