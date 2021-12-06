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
import HighlightSentence from '../../../components/HighlightSentence'
import RowViewerLaunchButton from '../../../components/RowViewerLaunchButton'
import TableChip from '../../../components/TableChip'
import { MUTED_GREY } from '../../../dpColors'
import CommonMetadata from '../../../models/CommonMetadata'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../../nicefyNumber'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import NavigationList, { NavigationListElement } from '../NavigationList'
import NavigationPanel from '../NavigationPanel'
import { SELECTED_VIEW_ENUM } from '../../../models/SelectedView'

// props from parent
export interface Props {
  classes: Record<string, any>
  columnMetadata: Readonly<CommonMetadata[]>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
  handleClick: (
    drilldown?: SelectedDrilldown,
    event?: React.MouseEvent<Element>
  ) => void
}

interface State {}

class DistributionsNavigation extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, drilldown, filter, hovered, columnMetadata } = this.props
    const { dataset, table, column } = drilldown
    const hits = filter || hovered
    const searchToken = hits?.value
    const sortedColumns = orderBy(columnMetadata || [], 'columnName')
    const self = this
    return (
      <NavigationPanel className={classes.navPanel}>
        <div>
          <TableChip></TableChip>
        </div>
        <h3 className={classes.tableName}>
          <HighlightSentence
            content={table}
            matchTerms={[searchToken]}
            useBold={false}
            isFullWidth={true}
          />
        </h3>
        <div>
          <Button
            className={classes.backBtn}
            onClick={(event) =>
              this.props.handleClick(
                SelectedDrilldown.of({ dataset, table }),
                event
              )
            }>
            {'<'} back
          </Button>
        </div>
        <div>
          <RowViewerLaunchButton
            dataset={dataset}
            table={table}
            launchedFrom={SELECTED_VIEW_ENUM.LIST}
          />
        </div>
        <p>
          Columns{' '}
          <span className={classes.muted}>
            ({nicefyNumber(columnMetadata?.length)?.trim()})
          </span>
        </p>
        <NavigationList>
          {sortedColumns?.map((metadata): NavigationListElement => {
            return {
              value: metadata?.columnName,
              isSelected: () => column === metadata?.columnName,
              onClick: (event) => {
                self.props.handleClick(
                  SelectedDrilldown.of({
                    dataset,
                    table,
                    column: metadata?.columnName,
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
    root: {
      display: 'flex',
      flexFlow: 'row',
      height: '100%',
      marginRight: '4px',
    },
    navPanel: {
      marginRight: '4px',
    },
    muted: {
      color: MUTED_GREY,
    },
    backBtn: {
      paddingLeft: '0px',
      fontSize: '.75em',
      justifyContent: 'left',
    },
    tableName: {
      overflowWrap: 'break-word',
    },
  })

export default withStyles(styles)(DistributionsNavigation)
