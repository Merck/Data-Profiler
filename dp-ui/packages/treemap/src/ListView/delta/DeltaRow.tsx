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
import { TableCell, TableRow } from '@material-ui/core'
import { createStyles, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import { isUndefined } from 'lodash'
import moment from 'moment'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { DeltaIcon } from '../../components/DeltaIcon'
import { StoreState } from '../../index'
import DatasetDelta from '../../models/delta/DatasetDelta'
import DatasetDeltaEnhanced from '../../models/delta/DatasetDeltaEhanced'
import {
  DatasetDeltaEnum,
  isColumnEnum,
  isDatasetEnum,
  isTableEnum,
  normalizedDeltaEnum,
} from '../../models/delta/DatasetDeltaEnum'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  delta: Readonly<DatasetDeltaEnhanced>
  isStripped: boolean
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

interface DeltaCellProps {
  classes?: Record<string, any>
  delta: DatasetDelta
}

function LocationDeltaCell(props: DeltaCellProps) {
  const { classes, delta } = props

  if (!delta || !delta.deltaEnum) {
    return <Fragment />
  }

  const { deltaEnum, dataset, table, column } = delta
  const deltaType = DatasetDeltaEnum[deltaEnum]
  if (isUndefined(deltaType)) {
    console.log('unknown deltaType ' + deltaEnum)
    return <Fragment />
  }

  if (isDatasetEnum(deltaEnum)) {
    return (
      <div>
        {dataset && (
          <div className={classes.locationCell}>
            <span className={classes.locationName}>{dataset}</span>
          </div>
        )}
      </div>
    )
  } else if (isTableEnum(deltaEnum)) {
    return (
      <div>
        {table && (
          <div className={classes.locationCell}>
            <span className={classes.locationName}>{table}</span>
          </div>
        )}
      </div>
    )
  } else if (isColumnEnum(deltaEnum)) {
    return (
      <div>
        {column && (
          <div className={classes.locationCell}>
            <span className={classes.locationName}>
              {table} {column}
            </span>
          </div>
        )}
      </div>
    )
  } else {
    return <div>Unknown Location</div>
  }
}

function ChangedDetectedDeltaCell(props: DeltaCellProps) {
  const { delta, classes } = props

  if (!delta || !delta.deltaEnum) {
    return <Fragment />
  }

  const { deltaEnum } = delta
  const change = normalizedDeltaEnum(deltaEnum)
  if (isUndefined(change)) {
    return <div />
  }

  return (
    <div className={classes.changeCell}>
      <span className={classes.changeIcon}>
        <DeltaIcon deltaEnum={deltaEnum}></DeltaIcon>
      </span>
      <span className={classes.changeText}>{change}</span>
    </div>
  )
}

class DeltaRow extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, delta, isStripped } = this.props

    const { updatedOnMillis, isCurrentVersion } = delta
    const updatedFromNow = moment.utc(updatedOnMillis).fromNow()
    /**
     *   "{normalizedDeltaEnum(delta?.deltaEnum)} from '{delta?.valueFrom}' to
     *    '{delta?.valueTo}'" --- <i>Dataprofiler system</i>
     */
    return (
      <TableRow
        hover
        // onClick={(event) => this.handleRowClick(event)}
        tabIndex={-1}
        // key={`${delta?.dataset}-${delta?.deltaEnum}-${delta?.updatedOnMillis}`}
        style={{ cursor: 'pointer' }}
        className={`${isStripped ? classes.stripped : ''}`}>
        <TableCell
          className={`${isCurrentVersion ? classes.isCurrentVersion : ''}`}
          scope="row">
          <i>{updatedFromNow}</i>
        </TableCell>
        <TableCell align="left">
          <LocationDeltaCell delta={delta} classes={classes} />
        </TableCell>
        <TableCell align="left">
          <ChangedDetectedDeltaCell delta={delta} classes={classes} />
        </TableCell>
        <TableCell align="left">
          {delta?.comment?.note && (
            <span>
              "{delta?.comment.note}" -- <i>{delta?.comment.createdBy}</i>
            </span>
          )}
          {!delta?.comment && <span></span>}
        </TableCell>
      </TableRow>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {},
  stripped: {
    background: '#fff',
  },
  locationCell: {
    verticalAlign: 'middle',
    display: 'flex',
    flex: '1 1 auto',
    flexFlow: 'row',
    maxHeight: 32,
    alignItems: 'center',
  },
  locationName: {
    marginRight: 8,
    minWidth: 212,
  },
  changeCell: {
    verticalAlign: 'middle',
    display: 'flex',
    flex: '1 1 auto',
    flexFlow: 'row',
    maxHeight: 32,
    alignItems: 'center',
  },
  changeIcon: {
    marginRight: 14,
  },
  changeText: {
    marginRight: 8,
    minWidth: 212,
  },
  isCurrentVersion: {
    borderLeft: '3px solid #00857C',
  },
}))

const mapStateToProps = (state: StoreState) => ({})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators({}, dispatch)

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(DeltaRow))
