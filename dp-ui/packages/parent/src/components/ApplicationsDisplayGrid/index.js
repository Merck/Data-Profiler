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
import { DPContext } from '@dp-ui/lib'
import { Button, Divider, Paper, Typography } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles'
import React from 'react'
import { MMD_QUALITY_FEATURE_FLAG } from '../../features'

const cardWidth = 369
const cardHeight = 320
const marginLR = 26
const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'flex-start',
    overflow: 'hidden',
    paddingTop: 32,
    maxWidth: 1200,
    flexDirection: 'column',
  },
  header: {
    marginLeft: 12,
    marginBottom: 18,
    fontWeight: 1.2,
  },
  cards: {
    display: 'flex',
    justifyContent: 'flex-start',
    flexWrap: 'wrap',
  },
  paper: {
    color: '#4B5D66',
    flex: '1 1 500px',
    borderRadius: 10,
    minHeight: cardHeight,
    width: cardWidth,
    minWidth: cardWidth,
    maxWidth: cardWidth,
    margin: 10,
    '&:hover': {
      cursor: 'pointer',
    },
  },
  divider: {
    marginTop: theme.spacing(1),
    marginBottom: theme.spacing(2),
    marginLeft: marginLR,
    marginRight: marginLR,
  },
  contactSquare: {
    background:
      'linear-gradient(90deg, rgba(21,75,68,1) 0%, rgba(43,132,124,1) 100%)',
    color: '#FFF',
    '&:hover': {
      cursor: 'unset',
    },
  },
  contactSquareContent: {
    marginTop: marginLR,
    marginRight: marginLR,
    marginLeft: marginLR,
  },
  contactSquareText: {
    fontSize: '2.125rem',
    fontWeight: 200,
    lineHeight: 1.235,
  },
  contactButton: {
    marginTop: 24,
    color: '#FFF',
    border: '1px solid #FFF',
    fontWeight: 'bold',
  },
  contactLink: {
    textDecoration: 'none',
  },
  title: {
    height: 98,
    minHeight: 98,
    marginLeft: marginLR,
    whiteSpace: 'nowrap',
    '& h4': {
      lineHeight: '98px',
    },
  },
  body: {
    marginRight: marginLR,
    marginLeft: marginLR,
    marginBottom: 10,
    fontSize: 12,
  },
  productTitle: {
    '&:first-child': {
      fontFamily: 'Monoton, "Monoton Regular", sans-serif',
    },
  },
  productTitle: {
    '&:first-child': {
      fontWeight: 'bolder',
    },
  },
}))

const applications = []
const emailAddr = ''

function CardTitle(props) {
  const { classes, app } = props
  if (!app) {
    return <></>
  }

  const variant = 'h4'
  const title = app?.title || ''
  const useArtDecoTitleFont = app?.useArtDecoTitleFont || false
  const titleArr = title.trim().split(/[ ]+/)
  if (!useArtDecoTitleFont && titleArr.length < 2) {
    return <Typography variant={variant}>{title}</Typography>
  }

  const firstWord = titleArr[0]
  const rest = titleArr.slice(1, titleArr.length)
  return (
    <Typography variant={variant}>
      <span
        className={
          useArtDecoTitleFont ? classes.productTitle : classes.productTitle
        }>
        {firstWord}
      </span>{' '}
      <span>{rest}</span>
    </Typography>
  )
}

function ApplicationsDisplayGrid(props) {
  const classes = useStyles()
  return (
    <div className={classes.root}>
      <div className={classes.header}>
        <Typography variant={'h6'}>Applications</Typography>
      </div>
      <div className={classes.cards}>
        {applications.map((app) => {
          if (app?.disable) {
            return false
          }
          return (
            <Paper
              key={app.title}
              className={classes.paper}
              onClick={() => {
                if (
                  app.url &&
                  (app.url.startsWith('https://') ||
                    app.url.startsWith('http://'))
                ) {
                  window.open(app.url, 'dataprofiler ui3 external app link')
                  return
                }
                props.router.navigate(app.url)
              }}>
              <div className={classes.title}>
                <CardTitle {...props} classes={classes} app={app}></CardTitle>
              </div>
              <Divider className={classes.divider} />
              <div className={classes.body}>{app.description}</div>
            </Paper>
          )
        })}

        <Paper className={`${classes.paper} ${classes.contactSquare}`}>
          <div className={classes.contactSquareContent}>
            <div className={classes.contactSquareText}>
              Want to leverage Data Profiler to build your own application?
            </div>
            <a className={classes.contactLink} href={`mailto:${emailAddr}`}>
              <Button className={classes.contactButton} variant="outlined">
                Contact Us
              </Button>
            </a>
          </div>
        </Paper>
      </div>
    </div>
  )
}

export default DPContext(ApplicationsDisplayGrid)
