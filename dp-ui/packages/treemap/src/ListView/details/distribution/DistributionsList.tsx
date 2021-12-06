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
import { FormControl, IconButton, InputBase, Paper } from '@material-ui/core'
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import { Search } from '@material-ui/icons'
import { isEmpty } from 'lodash'
import React from 'react'
import ColumnValueList from '../../../components/ColumnValueList'
import { DARK_GREY } from '../../../dpColors'
import ColumnCounts from '../../../models/ColumnCounts'
import CommonMetadata from '../../../models/CommonMetadata'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import DistributionsSkeleton from './DistributionsSkeleton'

// props from parent
export interface Props {
  classes: Record<string, any>
  refreshColCounts: (drilldown: SelectedDrilldown) => void
  columnMetadata: Readonly<CommonMetadata>
  columnCounts: Readonly<ColumnCounts>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
}

interface State {
  distributionFilter?: string
}

class DistributionsList extends React.Component<Props, State> {
  filterRef = undefined

  constructor(props: Props) {
    super(props)
    this.filterRef = React.createRef()
    this.handleFilter = this.handleFilter.bind(this)
    this.state = {
      distributionFilter: '',
    }
  }

  handleFilter(event?: React.FormEvent): void {
    if (event) {
      event?.preventDefault()
    }

    const distributionFilter =
      this.filterRef?.current?.value?.toLowerCase()?.trim() || ''
    this.setState((prev) => {
      return {
        ...prev,
        distributionFilter,
      }
    })
  }

  render(): JSX.Element {
    const {
      classes,
      drilldown,
      filter,
      hovered,
      columnCounts,
      columnMetadata,
    } = this.props
    const { distributionFilter } = this.state
    const filterValue = distributionFilter || ''
    const { dataset, table, column } = drilldown
    return (
      <div style={{ flex: 1 }}>
        <div>
          <FormControl
            className={classes.valuesSearchForm}
            onSubmit={this.handleFilter}>
            <Paper elevation={0} component="form">
              <InputBase
                autoCorrect="off"
                spellCheck="false"
                inputRef={this.filterRef}
                placeholder="Find values..."
                className={classes.input}
                inputProps={{
                  'aria-label': 'values search',
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
        </div>
        <div
          style={{
            position: 'relative',
          }}>
          {!columnCounts ||
            (isEmpty(columnCounts?.values) && (
              <DistributionsSkeleton></DistributionsSkeleton>
            ))}
          {columnCounts && !isEmpty(columnCounts?.values) && (
            <ColumnValueList
              refreshColCounts={this.props.refreshColCounts}
              columnCounts={columnCounts}
              filter={filter}
              hovered={hovered}
              highlightMatches={[filterValue]}
              numUniqueValues={columnMetadata?.numUniqueValues}
              numValues={columnMetadata?.numValues}
              drilldown={SelectedDrilldown.of({
                dataset,
                table,
                column,
              })}></ColumnValueList>
          )}
        </div>
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    valuesSearchForm: {
      border: '2px solid ' + DARK_GREY,
      borderRadius: '5px',
      marginBottom: '24px',
    },
    form: {
      border: '2px solid ' + DARK_GREY,
      borderRadius: '5px',
    },
    input: {
      flex: 1,
      margin: '4px 0px 4px 4px',
      marginLeft: '8px',
    },
  })

export default withStyles(styles)(DistributionsList)
