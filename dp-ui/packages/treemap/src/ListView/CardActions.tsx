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
import { createStyles, withStyles } from '@material-ui/core/styles'
import React from 'react'
import DownloadLauncherButton from '../components/DownloadLauncherButton'
import CommonMetadata from '../models/CommonMetadata'

// props from parent
export interface OwnProps {
  classes: Record<string, any>
  dataset: string
  tableMetadata?: Readonly<CommonMetadata[]>
}
// props from redux state
interface StateProps {}
// props from redux actions
type Props = StateProps & OwnProps
interface State {}

class CardActions extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, dataset, tableMetadata } = this.props

    return (
      <div className={classes.root}>
        <DownloadLauncherButton
          dataset={dataset}
          table={tableMetadata.map((table) => table.tableName)}
        />
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = createStyles((theme) => ({
  root: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'flex-end',
    background: 'white',
    padding: 16,
    margin: -16,
    marginTop: 0,
  },
}))

export default withStyles(styles)(CardActions)
