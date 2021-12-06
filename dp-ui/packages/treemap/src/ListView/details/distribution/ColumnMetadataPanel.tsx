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
import { createStyles, Theme, withStyles } from '@material-ui/core/styles'
import moment from 'moment'
import React from 'react'
import ColumnChip from '../../../components/ColumnChip'
import HighlightSentence from '../../../components/HighlightSentence'
import { MUTED_GREY } from '../../../dpColors'
import { DATE_TIME_GROUP_FMT } from '../../../dpDateFormats'
import { upperFirstLetter } from '../../../helpers/StringHelper'
import CommonMetadata from '../../../models/CommonMetadata'
import AggregatedSearchResult from '../../../MultiSearch/models/AggregatedSearchResult'
import { nicefyNumber } from '../../../nicefyNumber'
import SelectedDrilldown from '../../../drilldown/models/SelectedDrilldown'
import NavigationPanel from '../NavigationPanel'

// props from parent
export interface Props {
  classes: Record<string, any>
  columnMetadata: Readonly<CommonMetadata>
  filter: Readonly<AggregatedSearchResult>
  hovered: Readonly<AggregatedSearchResult>
  drilldown: Readonly<SelectedDrilldown>
}

interface State {}

class ColumnMetadataPanel extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, drilldown, filter, hovered, columnMetadata } = this.props
    const { column } = drilldown
    const hits = filter || hovered
    const searchToken = hits?.value
    return (
      <NavigationPanel className={classes.root}>
        <div>
          <ColumnChip></ColumnChip>
        </div>
        <h3 className={classes.columnName}>
          <HighlightSentence
            content={column}
            matchTerms={[searchToken]}
            useBold={false}
            isFullWidth={true}
          />
        </h3>
        <div>
          <p>
            Updated Date{' '}
            <span className={classes.muted}>
              ({moment(columnMetadata?.updatedOn).format(DATE_TIME_GROUP_FMT)})
            </span>
          </p>
          <p>
            Values{' '}
            <span className={classes.muted}>
              ({nicefyNumber(columnMetadata?.numValues)?.trim()})
            </span>
          </p>
          <p>
            Unique Values{' '}
            <span className={classes.muted}>
              ({nicefyNumber(columnMetadata?.numUniqueValues)?.trim()})
            </span>
          </p>
          <p>
            Type{' '}
            <span className={classes.muted}>
              ({upperFirstLetter(columnMetadata?.dataType?.trim())})
            </span>
          </p>
        </div>
      </NavigationPanel>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {
      margin: '0 12px',
    },
    muted: {
      color: MUTED_GREY,
    },
    columnName: {
      overflowWrap: 'break-word',
    },
  })

export default withStyles(styles)(ColumnMetadataPanel)
