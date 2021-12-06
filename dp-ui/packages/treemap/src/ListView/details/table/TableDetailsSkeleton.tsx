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
import Skeleton from '@material-ui/lab/Skeleton'
import _ from 'lodash'
import React from 'react'

// props from parent
export interface Props {
  classes: Record<string, any>
  tables: number
}

interface State {}

class TableDetailsSkeleton extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    return (
      <div
        style={{
          display: 'flex',
          height: '100%',
          flexDirection: 'column',
          marginTop: 18,
        }}>
        <Skeleton
          key={`dp-list-drawer-skeleton-header`}
          animation={false}
          variant={'rect'}
          height={52}></Skeleton>
        <Skeleton
          key={`dp-list-drawer-skeleton-table`}
          animation={false}
          variant={'rect'}
          height={58 + 53 * _.clamp(this.props.tables, 1, 10)}
          style={{ marginTop: 8 }}></Skeleton>
      </div>
    )
  }
}
// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
  })

export default withStyles(styles)(TableDetailsSkeleton)
