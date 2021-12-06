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
import { FormControl, InputLabel, MenuItem, Select } from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { bindActionCreators, Dispatch } from '@reduxjs/toolkit'
import React, { Fragment } from 'react'
import { connect, ConnectedProps } from 'react-redux'
import { StoreState } from '../../index'
import { resetOrderBy, setOrderBy } from '../../orderby/actions'
import { DEFAULT_ORDER_BY, ORDER_BY_ENUM } from '../../orderby/models/OrderBy'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
}
// props from redux state
interface StateProps {}
// props from redux actions
type DispatchProps = ConnectedProps<typeof connector>
type Props = StateProps & DispatchProps & OwnProps
interface State {}

class OrderBySelection extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.handleChange = this.handleChange.bind(this)
  }

  handleChange(event?: React.ChangeEvent<HTMLInputElement>): void {
    if (event) {
      event.preventDefault()
    }

    const val = event.target.value
    const selected = { orderBy: val as ORDER_BY_ENUM }
    // set user selected order by preference
    this.props.setOrderBy(selected)
  }

  render(): JSX.Element {
    const { classes, selectedView, selectedOrderBy } = this.props
    const options = Object.keys(ORDER_BY_ENUM).map((key) => {
      return ORDER_BY_ENUM[key]
    })
    const view = selectedView['view']
    if (view !== 'list') {
      return <Fragment></Fragment>
    }
    const currentOrderBy = selectedOrderBy?.orderBy
      ? (selectedOrderBy.orderBy as ORDER_BY_ENUM)
      : DEFAULT_ORDER_BY
    return (
      <div>
        <FormControl className={classes.formControl}>
          {/* <InputLabel id="order-by-selection-label">Sort by</InputLabel> */}
          <Select
            disableUnderline
            labelId="order-by-selection-label"
            id="order-by-selection-id"
            defaultValue={DEFAULT_ORDER_BY}
            value={currentOrderBy}
            onChange={this.handleChange}>
            {options?.map((option) => (
              <MenuItem key={option} value={option}>
                {option}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    formControl: {
      margin: theme.spacing(1),
      minWidth: 120,
      maxWidth: 300,
    },
  })

const mapStateToProps = (state: StoreState) => ({
  selectedOrderBy: state.orderby.selectedOrderBy,
  selectedView: state.treemap.selectedView,
})

const mapDispatchToProps = (dispatch: Dispatch) =>
  bindActionCreators(
    {
      setOrderBy,
      resetOrderBy,
    },
    dispatch
  )

const connector = connect(mapStateToProps, mapDispatchToProps)
export default withStyles(styles)(connector(OrderBySelection))
