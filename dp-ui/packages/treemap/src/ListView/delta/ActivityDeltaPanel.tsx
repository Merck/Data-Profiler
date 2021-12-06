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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
} from '@material-ui/core'
import { createStyles, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isEmpty, isUndefined, orderBy, upperFirst } from 'lodash'
import moment from 'moment'
import React from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../index'
import DatasetDelta from '../../models/delta/DatasetDelta'
import { DatasetDeltaEnum } from '../../models/delta/DatasetDeltaEnum'
import ReorderCommonMetadata from '../../models/ReorderCommonMetadata'
import DatasetDeltaAdapter from '../../services/delta/DatasetDeltaAdapter'
import DeltaRow from './DeltaRow'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  datasetMetadata: Readonly<ReorderCommonMetadata>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

type Order = 'asc' | 'desc'
type SortFields = keyof DatasetDelta
interface State {
  orderDirection: Order
  orderField: SortFields
}

class ActivityDeltaPanel extends React.Component<Props, State> {
  DEFAULT_ORDER_DIRECTION: Order = 'desc'
  DEFAULT_ORDER_FIELD: SortFields = 'datasetUpdatedOnMillis'

  constructor(props: Props) {
    super(props)
    this.state = {
      orderDirection: this.DEFAULT_ORDER_DIRECTION,
      orderField: this.DEFAULT_ORDER_FIELD,
    }
  }

  linkDeltasAndComments(
    deltas?: Readonly<DatasetDelta[]>
  ): Readonly<DatasetDelta[]> {
    if (!deltas || isEmpty(deltas)) {
      return []
    }

    const { comments } = this.props
    if (!isEmpty(comments)) {
      deltas.forEach((delta) => {
        const commentUUID = delta?.comment.uuid
        const comment = comments.find(
          (comment) => comment?.uuid === commentUUID
        )
        if (comment) {
          delta.comment = {
            ...comment,
          }
        }
      })
    }

    return [...deltas]
  }

  rankDeltas(deltas?: Readonly<DatasetDelta[]>): Readonly<DatasetDelta[]> {
    if (!deltas || isEmpty(deltas)) {
      return []
    }

    const { orderField, orderDirection } = this.state
    return orderBy(deltas, orderField, orderDirection)
  }

  /**
   * TODO: remove this test data
   * @returns
   */
  // genTestDescending(dataset: string, version: string): DatasetDelta[] {
  //   const table = 'dmk-test-table-remove-me'
  //   const column = 'dmk-test-column-remove-me'

  //   const o0 = {
  //     updatedOnMillis: Number.MIN_VALUE,
  //     datasetUpdatedOnMillis: Number.MIN_VALUE,
  //     dataset,
  //     table,
  //     column,
  //     valueFrom: '',
  //     valueTo: '',
  //     fromVersion: version,
  //     targetVersion: '',
  //     comment: {},
  //   }
  //   const o1: DatasetDelta = {
  //     ...o0,
  //     deltaEnum: DatasetDeltaEnum.DATASET_VALUES_DECREASED,
  //   }
  //   const o2: DatasetDelta = {
  //     ...o0,
  //     deltaEnum: DatasetDeltaEnum.TABLE_VALUES_DECREASED,
  //   }
  //   const o3: DatasetDelta = {
  //     ...o0,
  //     deltaEnum: DatasetDeltaEnum.COLUMN_VALUES_DECREASED,
  //   }
  //   const o4: DatasetDelta = {
  //     ...o0,
  //     deltaEnum: DatasetDeltaEnum.TABLE_REMOVED,
  //   }
  //   const o5: DatasetDelta = {
  //     ...o0,
  //     deltaEnum: DatasetDeltaEnum.COLUMN_REMOVED,
  //   }
  //   return [o1, o2, o3, o4, o5]
  // }

  // handleRowClick(
  //   drilldown?: ListViewDrilldown,
  //   event?: React.MouseEvent<Element>
  // ) {
  //   if (event) {
  //     event.preventDefault()
  //   }

  //   // this.props.handleDrilldown(drilldown)
  //   this.props.setListViewDrilldown(drilldown)
  // }

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

  filterOutUnwantedDeltas(deltas: Readonly<DatasetDelta[]>): DatasetDelta[] {
    if (!deltas) {
      return []
    }

    // remove certain granularity counts
    return deltas.filter((el) => {
      const deltaType = DatasetDeltaEnum[el?.deltaEnum]
      if (isUndefined(deltaType)) {
        return false
      }
      const deltaStr = deltaType.toLowerCase()
      // if (
      //   deltaStr.indexOf('column_values') > -1 ||
      //   deltaStr.indexOf('dataset_values') > -1
      // ) {
      //   return false
      // }
      return true
    })
  }

  render(): JSX.Element {
    const { orderDirection, orderField } = this.state
    const { classes, datasetMetadata } = this.props
    const deltaJson = datasetMetadata?.commonMetadata?.properties?.delta
    const adapter = new DatasetDeltaAdapter()
    const completeDelta = adapter.convert(deltaJson)
    const deltas = this.rankDeltas(
      this.linkDeltasAndComments(
        this.filterOutUnwantedDeltas([...(completeDelta?.deltas || [])])
      )
    )
    const columnMap = {
      time: 'updatedOnMillis',
      location: 'table',
      'change detected': 'deltaEnum',
      notes: '',
    }
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    const noop = (e) => {}
    const refreshTimestamp = moment
      .utc(completeDelta?.updatedOnMillis)
      .fromNow()
    return (
      <div className={classes.root}>
        <div className={classes.title}>
          <span>Data Delta</span>
          <span className={classes.timestamp}>
            Last refreshed - {refreshTimestamp}
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
                        key={currentField}
                        sortDirection={
                          orderField === currentField ? orderDirection : false
                        }
                        align="left">
                        <TableSortLabel
                          active={orderField === currentField}
                          direction={
                            orderField === currentField
                              ? orderDirection
                              : this.DEFAULT_ORDER_DIRECTION
                          }
                          onClick={
                            currentField != 'notes' &&
                            currentField != 'location'
                              ? (e) => this.handleSortClick(currentField, e)
                              : noop
                          }>
                          {upperFirst(column)}
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
                {!isEmpty(deltas) &&
                  deltas.map((delta, i) => (
                    <DeltaRow
                      key={`dp-lv-deltarow-${i}`}
                      isStripped={i % 2 === 0}
                      delta={delta}></DeltaRow>
                  ))}
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
const styles = createStyles((theme) => ({
  root: {
    marginTop: 18,
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
  },
  title: {
    fontSize: '1.2em',
    height: '52px',
    display: 'flex',
    alignItems: 'center',
  },
  table: {
    // maxHeight: '65vh',
    overflow: 'auto',
    position: 'relative',
    flex: 1,
    marginTop: '8px',
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
  timestamp: {
    top: 0,
    right: 0,
    opacity: 0.4,
    fontSize: 12,
    marginLeft: 'auto',
  },
}))

const mapStateToProps = (state: StoreState) => ({
  comments: state.comments.comments,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(ActivityDeltaPanel))
