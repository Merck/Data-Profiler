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
import { ChevronRight } from '@material-ui/icons'
import React from 'react'
import { NAV_BORDER_COLOR } from '../../dpColors'

export interface NavigationListElement {
  value: string
  onClick: (event?: React.MouseEvent) => void
  isSelected: () => boolean
}
// props from parent
export interface Props {
  classes: Record<string, any>
  children?: NavigationListElement[]
}
interface State {}

class NavigationList extends React.PureComponent<Props, State> {
  private selectedRow = React.createRef<HTMLDivElement>()
  private listContainer = React.createRef<HTMLDivElement>()
  constructor(props: Props) {
    super(props)
    this.selectedRow = React.createRef()
  }

  executeScroll = () => {
    this.listContainer.current.scrollTop = Math.abs(
      this.listContainer.current?.getBoundingClientRect().top -
        this.selectedRow.current?.getBoundingClientRect().top
    )
  }

  componentDidMount() {
    this.executeScroll()
  }

  render(): JSX.Element {
    const { classes } = this.props
    const children = this.props?.children
    // const children = React.Children.toArray(this.props.children)
    //         {React.Children.map<JSX.Element, NavigationListElement>(
    return (
      <div className={classes.navListContainer} ref={this.listContainer}>
        <div className={classes.navList}>
          {children?.map((item, i) => {
            const { value, isSelected, onClick } = item
            return (
              <div
                onClick={onClick}
                className={`${classes.navElement} ${
                  i === 0 ? classes.navTop : ''
                } ${isSelected() ? classes.selectedNav : ''}`}
                key={`nav-list-${i}`}
                ref={isSelected() ? this.selectedRow : null}>
                <span className={`${classes.navElementLeft}`}>{value}</span>
                <span className={classes.navElementRight}>
                  <ChevronRight></ChevronRight>
                </span>
              </div>
            )
          })}
        </div>
      </div>
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
    navListContainer: {
      flex: 1,
      position: 'relative',
      overflow: 'auto',
      minHeight: '15vh',
    },
    navList: {
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      position: 'absolute',
      top: 0,
      left: 0,
      right: 0,
    },
    navTop: {
      borderTop: `1px solid ${NAV_BORDER_COLOR}`,
    },
    navElement: {
      display: 'flex',
      fontSize: '.8em',
      '&:hover': {
        background: '#F5F5F5',
        cursor: 'pointer',
      },
    },
    navElementLeft: {
      textOverflow: 'ellipsis',
      overflow: 'hidden',
      whiteSpace: 'nowrap',
      textAlign: 'justify',
      minHeight: '48px',
      lineHeight: '48px',
      paddingLeft: '4px',
      borderBottom: `1px solid ${NAV_BORDER_COLOR}`,
      flex: '1 1 auto',
    },
    navElementRight: {
      minHeight: '24px',
      minWidth: '24px',
      marginTop: '12px',
      borderBottom: `1px solid ${NAV_BORDER_COLOR}`,
      color: NAV_BORDER_COLOR,
    },
  })

export default withStyles(styles)(NavigationList)
