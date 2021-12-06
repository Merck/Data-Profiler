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
import { orderBy } from 'lodash'
import React from 'react'
import CommonMetadata from '../../../models/CommonMetadata'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import NavigationList from '../NavigationList'
import NavigationPanel from '../NavigationPanel'

// props from parent
export interface Props {
  classes: Record<string, any>
  tableMetadata: Readonly<CommonMetadata[]>
  drilldown: Readonly<SelectedDrilldown>
  handleClick: (
    drilldown?: SelectedDrilldown,
    event?: React.MouseEvent<Element>
  ) => void
}

interface State {}

class ColumnDetailsNavigation extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, tableMetadata, drilldown } = this.props

    const selectedTable = drilldown?.table || ''
    const sortedTables = orderBy(tableMetadata || [], 'tableName')
    return (
      <NavigationPanel>
        <div>
          <Button
            className={classes.backBtn}
            onClick={(event) =>
              this.props.handleClick(
                SelectedDrilldown.of({ dataset: drilldown?.dataset }),
                event
              )
            }>
            {'<'} back
          </Button>
        </div>
        <NavigationList>
          {sortedTables?.map((metadata, i) => {
            return {
              value: metadata?.tableName,
              isSelected: () => selectedTable === metadata?.tableName,
              onClick: (event) => {
                this.props.handleClick(
                  SelectedDrilldown.of({
                    dataset: drilldown?.dataset,
                    table: metadata?.tableName,
                  }),
                  event
                )
              },
            }
          })}
        </NavigationList>
      </NavigationPanel>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    selectedNav: {
      fontWeight: 'bold',
    },
    backBtn: {
      paddingLeft: '0px',
      fontSize: '.75em',
      justifyContent: 'left',
    },
  })

export default withStyles(styles)(ColumnDetailsNavigation)
