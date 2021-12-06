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
import React from 'react'
import { NAV_BORDER_COLOR } from '../../dpColors'

// props from parent
export interface Props {
  classes: Record<string, any>
  className?: Record<string, any>
}

interface State {}

class NavigationPanel extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props)
  }

  render(): JSX.Element {
    const { classes, className } = this.props
    return (
      <div className={`${classes.navPanel} ${className}`}>
        {this.props.children}
      </div>
    )
  }
}

// if using typescript, make sure to use createStyles
//  https://material-ui.com/guides/typescript/
const styles = (theme: Theme) =>
  createStyles({
    root: {},
    navPanel: {
      position: 'sticky',
      top: 0,
      overflow: 'auto',
      display: 'flex',
      width: '15%',
      maxWidth: '15%',
      minWidth: '15%',
      height: '100%',
      borderRight: `1px solid ${NAV_BORDER_COLOR}`,
      flexDirection: 'column',
      justifyContent: 'start',
    },
  })

export default withStyles(styles)(NavigationPanel)
