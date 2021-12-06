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
  FormControl,
  IconButton,
  InputBase,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
} from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { Search } from '@material-ui/icons'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isEmpty, isUndefined, orderBy, take } from 'lodash'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import ColumnPreviewChart from '../../../components/ColumnPreviewChart'
import HighlightSentence from '../../../components/HighlightSentence'
import Sentence from '../../../components/Sentence'
import { ACTION_BTN_COLOR, DARK_GREY } from '../../../dpColors'
import { upperFirstLetter } from '../../../helpers/StringHelper'
import { StoreState } from '../../../index'
import ColumnSample from '../../../models/ColumnSample'
import CommonMetadata from '../../../models/CommonMetadata'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../../nicefyNumber'
import AggregratedSearchResultService from '../../../services/AggregatedSearchResultService'
import { ColumnSampleService } from '../../../services/ColumnSampleService'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import ColumnDetailsHeader from './ColumnDetailsHeader'
import ColumnDetailsNavigation from './ColumnDetailsNavigation'
import ColumnDetailsSkeleton from './ColumnDetailsSkeleton'
import { setListViewDrilldown, clearListViewDrilldown } from '../../actions'
import { searchFrequencyColorRange } from '../../../searchHitColors'
import CommonMetadataWithHitCount from '../../../models/CommonMetadataWithHitCount'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  tableMetadata: Readonly<CommonMetadata[]>
  columnMetadata: Readonly<CommonMetadata[]>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
  samples: Readonly<ColumnSample[]>
}

type DispatchProps = ConnectedProps<typeof connector>
type Props = DispatchProps & OwnProps

type Order = 'asc' | 'desc'
type SortFields = keyof CommonMetadata
interface State {
  orderDirection: Order
  orderField: SortFields
  columnGridFilter?: string
}

class ColumnDetails extends React.Component<Props, State> {
  DEFAULT_ORDER_DIRECTION: Order = 'asc'
  DEFAULT_ORDER_FIELD: SortFields = 'tableName'
  columnGridSearchRef = undefined

  constructor(props: Props) {
    super(props)
    this.columnGridSearchRef = React.createRef()
    this.state = {
      orderDirection: this.DEFAULT_ORDER_DIRECTION,
      orderField: this.DEFAULT_ORDER_FIELD,
      columnGridFilter: '',
    }

    this.handleDrilldownClick = this.handleDrilldownClick.bind(this)
  }

  rankColumnNames(
    columns?: Readonly<CommonMetadata[]>
  ): Readonly<CommonMetadataWithHitCount[]> {
    if (!columns || isEmpty(columns)) {
      return []
    }

    const { orderField, orderDirection, columnGridFilter } = this.state
    const filtered = !isEmpty(columnGridFilter)
      ? columns?.filter(
          (el) => el?.columnName?.toLowerCase()?.indexOf(columnGridFilter) > -1
        )
      : columns
    const service = new AggregratedSearchResultService()
    const { filter, hovered } = this.props
    const hits = filter || hovered
    // augment the common metadata with hit count information for sorting and displaying later
    const withHits: CommonMetadataWithHitCount[] = filtered.map((el) => {
      const { datasetName, tableName, columnName } = el
      const valueHitCount = service.calcValueHitCount(
        hits,
        datasetName,
        tableName,
        columnName
      )
      return {
        ...el,
        valueHitCount,
        totalHitCount: valueHitCount,
      }
    })
    return orderBy(
      withHits || [],
      ['totalHitCount', orderField],
      ['desc', orderDirection]
    )
  }

  handleDrilldownClick(
    drilldown?: SelectedDrilldown,
    event?: React.MouseEvent<Element>
  ): void {
    if (event) {
      event.preventDefault()
    }
    if (!drilldown) {
      return
    }
    this.props?.clearListViewDrilldown()
    this.props?.setListViewDrilldown(drilldown)
  }

  handleSortClick(column: SortFields, event?: React.MouseEvent<Element>): void {
    if (event) {
      event.preventDefault()
    }
    const { orderField, orderDirection } = this.state
    if (orderField === column) {
      // change sort order for current field
      this.setState({
        orderDirection: orderDirection === 'desc' ? 'asc' : 'desc',
      })
      return
    }

    // change sort field with default sort order
    this.setState({
      orderField: column,
      orderDirection: this.DEFAULT_ORDER_DIRECTION,
    })
  }

  handleColumnGridFilter(event?: React.FormEvent): void {
    if (event) {
      event?.preventDefault()
    }

    const columnGridFilter =
      this.columnGridSearchRef?.current?.value?.toLowerCase()?.trim() || ''
    this.setState({
      columnGridFilter,
    })
  }

  render(): JSX.Element {
    const {
      classes,
      columnMetadata,
      tableMetadata,
      hovered,
      filter,
      drilldown,
      samples,
      isFetchingSamples,
    } = this.props
    const columns = this.rankColumnNames(columnMetadata)
    const numColumns = tableMetadata.find(
      (table) => table.tableName === drilldown?.table
    )?.numColumns

    const samplesService = new ColumnSampleService()

    const dataset = drilldown?.dataset || ''
    const hits = filter || hovered
    const searchToken = hits?.value
    const { orderDirection, orderField, columnGridFilter } = this.state
    const columnMap = {
      name: 'columnName',
      samples: 'samples',
      'unique values': 'numUniqueValues',
      type: 'dataType',
      values: 'numValues',
    }
    return (
      <div className={classes.root}>
        <div className={classes.navAndTable}>
          <ColumnDetailsNavigation
            tableMetadata={tableMetadata}
            drilldown={drilldown}
            handleClick={this.handleDrilldownClick}
          />
          <div className={classes.content}>
            {isFetchingSamples || !samples || isEmpty(samples) ? (
              <ColumnDetailsSkeleton
                columns={numColumns}></ColumnDetailsSkeleton>
            ) : (
              <>
                <div className={classes.header}>
                  <ColumnDetailsHeader
                    tableMetadata={tableMetadata}
                    drilldown={drilldown}
                    hits={hits}></ColumnDetailsHeader>
                </div>
                <div className={classes.title}>
                  <span>Columns ({columns?.length})</span>
                  <span className={classes.gridFilter}>
                    <FormControl
                      className={classes.form}
                      onSubmit={(event) => this.handleColumnGridFilter(event)}>
                      <Paper elevation={0} component="form">
                        <InputBase
                          autoCorrect="off"
                          spellCheck="false"
                          inputRef={this.columnGridSearchRef}
                          placeholder="Find columns..."
                          className={classes.input}
                          inputProps={{
                            'aria-label': 'column grid search',
                            variant: 'outlined',
                          }}
                        />
                        <IconButton
                          type="submit"
                          className={`${classes.iconButton} ${classes.actionBtn}`}
                          aria-label="search">
                          <Search />
                        </IconButton>
                      </Paper>
                    </FormControl>
                  </span>
                </div>
                <div className={classes.table}>
                  <TableContainer
                    style={{
                      maxHeight: 'calc(125vh - 633px)', // height at which overflow no longer occurs
                    }}>
                    <Table stickyHeader aria-label="tables list">
                      <TableHead>
                        <TableRow>
                          {Object.keys(columnMap)?.map((column) => {
                            const currentField = columnMap[column]
                            return (
                              <TableCell
                                className={classes.headCell}
                                key={currentField}
                                sortDirection={
                                  orderField === currentField
                                    ? orderDirection
                                    : false
                                }
                                align={column !== 'name' ? 'right' : 'left'}>
                                <TableSortLabel
                                  className={classes.headCell}
                                  active={orderField === currentField}
                                  direction={
                                    orderField === currentField
                                      ? orderDirection
                                      : this.DEFAULT_ORDER_DIRECTION
                                  }
                                  onClick={(event) =>
                                    this.handleSortClick(currentField, event)
                                  }>
                                  {column}
                                  {orderField === currentField ? (
                                    <span className={classes.visuallyHidden}>
                                      {orderDirection === 'desc'
                                        ? 'sorted descending'
                                        : 'sorted ascending'}
                                    </span>
                                  ) : null}
                                </TableSortLabel>
                              </TableCell>
                            )
                          })}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {columns?.map((row, i) => {
                          const table = row?.tableName || ''
                          const column = row?.columnName || ''
                          const uniqueValueHitCount = 0
                          const totalHitCount = row?.totalHitCount || 0
                          const valueHitCount = row?.valueHitCount || 0
                          const filtered = samplesService.filterAndRerank(
                            dataset,
                            table,
                            column,
                            samples
                          )
                          const topSamples = take(
                            filtered.filter(
                              (el) => !isUndefined(el) && !isEmpty(el)
                            ),
                            5
                          ).map((el) => el.value)

                          const rowStyle = {
                            cursor: 'pointer',
                          }
                          if (totalHitCount && totalHitCount > 0) {
                            const hitGradient =
                              searchFrequencyColorRange(totalHitCount)
                            rowStyle['background'] = hitGradient
                          }
                          return (
                            <TableRow
                              hover
                              onClick={(event) =>
                                this.handleDrilldownClick(
                                  SelectedDrilldown.of({
                                    dataset,
                                    table,
                                    column,
                                  }),
                                  event
                                )
                              }
                              tabIndex={-1}
                              key={`${column}`}
                              style={rowStyle}>
                              <TableCell component="th" scope="row">
                                <HighlightSentence
                                  content={column}
                                  matchTerms={[searchToken, columnGridFilter]}
                                  useBold={false}
                                  willBreak={false}></HighlightSentence>
                              </TableCell>
                              <TableCell align="right">
                                <div
                                  className={`${classes.valuesCellContent} ${classes.samplesCellContent}`}>
                                  {topSamples?.length > 0 &&
                                    topSamples
                                      ?.filter(
                                        (el) => !isUndefined(el) && !isEmpty(el)
                                      )
                                      ?.map((sample, i) => {
                                        return (
                                          <HighlightSentence
                                            key={`${column}-${i}`}
                                            content={`${sample}${
                                              i < topSamples?.length - 1
                                                ? ','
                                                : ''
                                            }`}
                                            matchTerms={[
                                              searchToken,
                                              columnGridFilter,
                                            ]}
                                            useBold={true}></HighlightSentence>
                                        )
                                      })}
                                </div>
                              </TableCell>
                              <TableCell align="right">
                                <div className={classes.valuesCellContent}>
                                  <Sentence noBreak={true}>
                                    {uniqueValueHitCount > 0 && (
                                      <span>
                                        <b>
                                          {nicefyNumber(
                                            uniqueValueHitCount
                                          )?.trim()}
                                        </b>{' '}
                                        of
                                      </span>
                                    )}
                                  </Sentence>
                                  <span>
                                    {nicefyNumber(row?.numUniqueValues)?.trim()}
                                  </span>
                                  <span>
                                    <ColumnPreviewChart samples={filtered} />
                                  </span>
                                </div>
                              </TableCell>
                              <TableCell align="right">
                                {upperFirstLetter(row?.dataType?.trim())}
                              </TableCell>
                              <TableCell align="right">
                                <div className={classes.valuesCellContent}>
                                  <Sentence noBreak={true}>
                                    {valueHitCount > 0 && (
                                      <span>
                                        <b>
                                          {nicefyNumber(valueHitCount)?.trim()}
                                        </b>{' '}
                                        of
                                      </span>
                                    )}
                                  </Sentence>
                                  <span>
                                    {nicefyNumber(row?.numValues)?.trim()}
                                  </span>
                                </div>
                              </TableCell>
                            </TableRow>
                          )
                        })}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {
      marginTop: '24px',
      overflowY: 'auto',
      height: '100%',
    },
    form: {
      border: '2px solid ' + DARK_GREY,
      borderRadius: '5px',
    },
    table: {
      // maxHeight: '65vh',
      overflow: 'auto',
      position: 'relative',
      flex: 1,
    },
    title: {
      fontSize: '1.2em',
      height: '52px',
      display: 'flex',
      alignItems: 'center',
      marginBottom: '8px',
    },
    headCell: {
      background: '#FFFFFF',
      color: '#BEBEBE',
    },
    valuesCellContent: {
      display: 'flex',
      justifyContent: 'flex-end',
      wordSpacing: 'nowrap',
    },
    samplesCellContent: {
      // maxWidth: '40vw',
      flexWrap: 'wrap',
    },
    visuallyHidden: {
      border: 0,
      clip: 'rect(0 0 0 0)',
      height: 1,
      margin: -1,
      overflow: 'hidden',
      padding: 0,
      position: 'absolute',
      top: 20,
      width: 1,
    },
    input: {
      flex: 1,
      margin: '4px 0px 4px 4px',
      marginLeft: '8px',
    },
    iconButton: {
      padding: 10,
    },
    actionBtn: {
      color: ACTION_BTN_COLOR,
    },
    backBtn: {
      paddingBottom: '12px',
    },
    gridFilter: {
      marginLeft: '16px',
    },
    navAndTable: {
      display: 'flex',
      height: '100%',
    },
    content: {
      marginLeft: '12px',
      display: 'flex',
      flexFlow: 'column',
      flex: '1 1 auto',
      width: '85%',
    },
    comingSoon: {
      margin: '0 0 auto',
    },
  })

const mapStateToProps = (state: StoreState) => ({
  isFetchingSamples: state.listview.isFetchingSamples,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setListViewDrilldown,
      clearListViewDrilldown,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(ColumnDetails))
