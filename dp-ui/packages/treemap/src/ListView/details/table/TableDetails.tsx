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
import { isEmpty, orderBy } from 'lodash'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import HighlightSentence from '../../../components/HighlightSentence'
import Sentence from '../../../components/Sentence'
import { ACTION_BTN_COLOR, DARK_GREY } from '../../../dpColors'
import { StoreState } from '../../../index'
import CommonMetadata from '../../../models/CommonMetadata'
import CommonMetadataWithHitCount from '../../../models/CommonMetadataWithHitCount'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../../nicefyNumber'
import { searchFrequencyColorRange } from '../../../searchHitColors'
import AggregratedSearchResultService from '../../../services/AggregatedSearchResultService'
import { clearListViewDrilldown, setListViewDrilldown } from '../../actions'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import TableDetailsSkeleton from './TableDetailsSkeleton'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  tableMetadata: Readonly<CommonMetadata[]>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
  numTables: number
}

// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = DispatchProps & OwnProps

type Order = 'asc' | 'desc'
type SortFields = keyof CommonMetadata
interface State {
  orderDirection: Order
  orderField: SortFields
  tableGridFilter?: string
}

class TableDetails extends React.Component<Props, State> {
  DEFAULT_ORDER_DIRECTION: Order = 'asc'
  DEFAULT_ORDER_FIELD: SortFields = 'tableName'
  tableGridSearchRef = undefined

  constructor(props: Props) {
    super(props)
    this.tableGridSearchRef = React.createRef()
    this.state = {
      orderDirection: this.DEFAULT_ORDER_DIRECTION,
      orderField: this.DEFAULT_ORDER_FIELD,
      tableGridFilter: '',
    }
  }

  rankTableNames(
    tables?: Readonly<CommonMetadata[]>
  ): Readonly<CommonMetadataWithHitCount[]> {
    if (!tables || isEmpty(tables)) {
      return []
    }

    const { filter, hovered } = this.props
    const hits = filter || hovered
    const service = new AggregratedSearchResultService()
    const nonTitleHits = service.filterNoneTitleHits(hits)

    const { orderField, orderDirection, tableGridFilter } = this.state
    const filtered = !isEmpty(tableGridFilter)
      ? tables?.filter(
          (el) => el?.tableName?.toLowerCase()?.indexOf(tableGridFilter) > -1
        )
      : tables
    // augment the common metadata with hit count information for sorting and displaying later
    const withHits: CommonMetadataWithHitCount[] = filtered.map((el) => {
      const { datasetName, tableName } = el
      const columnHitCount = service.calcNumColumns(
        hits,
        datasetName,
        tableName
      )
      const valueHitCount = service.calcValueHitCount(
        nonTitleHits,
        datasetName,
        tableName
      )
      const totalHitCount = valueHitCount + columnHitCount
      return {
        ...el,
        columnHitCount,
        valueHitCount,
        totalHitCount,
      }
    })
    return orderBy(
      withHits || [],
      ['totalHitCount', orderField],
      ['desc', orderDirection]
    )
  }

  handleRowClick(
    drilldown?: SelectedDrilldown,
    event?: React.MouseEvent<Element>
  ) {
    if (event) {
      event.preventDefault()
    }

    // this.props.handleDrilldown(drilldown)
    this.props.setListViewDrilldown(drilldown)
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

  handleTableGridFilter(event?: React.FormEvent): void {
    if (event) {
      event?.preventDefault()
    }

    const tableGridFilter =
      this.tableGridSearchRef?.current?.value?.toLowerCase()?.trim() || ''
    this.setState({
      tableGridFilter,
    })
  }

  render(): JSX.Element {
    const { classes, tableMetadata, hovered, filter, drilldown, numTables } =
      this.props
    const { orderDirection, orderField, tableGridFilter } = this.state
    const tables = this.rankTableNames(tableMetadata)

    if ((!tables || isEmpty(tables)) && isEmpty(tableGridFilter)) {
      // show loading skeleton as long a tables exist
      //  and we didnt clear them all with a filter
      return <TableDetailsSkeleton tables={numTables}></TableDetailsSkeleton>
    }

    const hits = filter || hovered
    const searchToken = hits?.value
    const columnMap = {
      name: 'tableName',
      columns: 'numColumns',
      rows: 'numRows',
      values: 'numValues',
    }
    return (
      <div className={classes.root}>
        <div className={classes.title}>
          <span>Tables ({tables.length})</span>
          <span className={classes.gridFilter}>
            <FormControl
              className={classes.form}
              onSubmit={(event) => this.handleTableGridFilter(event)}>
              <Paper elevation={0} component="form">
                <InputBase
                  autoCorrect="off"
                  spellCheck="false"
                  inputRef={this.tableGridSearchRef}
                  placeholder="Find tables..."
                  className={classes.input}
                  inputProps={{
                    'aria-label': 'table grid search',
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
              maxHeight: 'calc(125vh - 490px)', // height at which overflow no longer occurs
            }}>
            <Table stickyHeader aria-label="tables list">
              <TableHead>
                <TableRow>
                  {Object.keys(columnMap).map((column) => {
                    const currentField = columnMap[column]
                    return (
                      <TableCell
                        className={classes.headCell}
                        key={currentField}
                        sortDirection={
                          orderField === currentField ? orderDirection : false
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
                          onClick={(e) =>
                            this.handleSortClick(currentField, e)
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
                {tables.map((row, i) => {
                  const table = row?.tableName || ''
                  const totalHitCount = row?.totalHitCount || 0
                  const columnHitCount = row?.columnHitCount || 0
                  const valueHitCount = row?.valueHitCount || 0
                  const rowStyle = {
                    cursor: 'pointer',
                  }
                  if (totalHitCount && totalHitCount > 0) {
                    const hitGradient = searchFrequencyColorRange(totalHitCount)
                    rowStyle['background'] = hitGradient
                  }
                  return (
                    <TableRow
                      hover
                      onClick={(event) =>
                        this.handleRowClick({ ...drilldown, table }, event)
                      }
                      tabIndex={-1}
                      key={`${table}`}
                      style={rowStyle}>
                      <TableCell component="th" scope="row">
                        <HighlightSentence
                          content={table}
                          matchTerms={[searchToken, tableGridFilter]}
                          useBold={false}
                          willBreak={false}></HighlightSentence>
                      </TableCell>
                      <TableCell align="right">
                        <div className={classes.valuesCellContent}>
                          <Sentence noBreak={true}>
                            {columnHitCount > 0 && (
                              <span>
                                <b>{nicefyNumber(columnHitCount)}</b> of
                              </span>
                            )}
                          </Sentence>
                          <span>{nicefyNumber(row?.numColumns)}</span>
                        </div>
                      </TableCell>
                      <TableCell align="right">
                        {nicefyNumber(row?.numRows)}
                      </TableCell>
                      <TableCell align="right">
                        <div className={classes.valuesCellContent}>
                          <Sentence noBreak={true}>
                            {valueHitCount > 0 && (
                              <span>
                                <b>{nicefyNumber(valueHitCount)}</b> of
                              </span>
                            )}
                          </Sentence>
                          <span>{nicefyNumber(row?.numValues)}</span>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
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
      marginTop: 18,
      display: 'flex',
      flexDirection: 'column',
      flex: 1,
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
      marginTop: '8px',
    },
    title: {
      fontSize: '1.2em',
      height: '52px',
      display: 'flex',
      alignItems: 'center',
    },
    headCell: {
      background: '#FFFFFF',
      color: '#BEBEBE',
    },
    valuesCellContent: {
      display: 'flex',
      justifyContent: 'flex-end',
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
    gridFilter: {
      marginLeft: '16px',
    },
  })

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setListViewDrilldown,
      clearListViewDrilldown,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(TableDetails))
