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
import React from 'react'
import { withStyles } from '@material-ui/core/styles'
import { Button, Grid } from '@material-ui/core'
import ArrowRightIcon from '@material-ui/icons/ArrowRight'
import { DPContext } from '@dp-ui/lib'
import './styles.css'


const styles = {
  root: {
    marginTop: 15,
    display: 'flex',
    alignItems: 'center',
  },
  separator: {
    display: 'inline-flex',
    alignItems: 'center',
    '& svg': {
      opacity: 0.54,
    },
  },
  currentButton: {
    '&:disabled': {
      color: '#000',
    },
  },
}

const Tutorial = (props) => {
  const handleClick = (e, id) => {
    e.preventDefault()
    document
      .querySelector(`.anchor[name="${id}"]`)
      ?.scrollIntoView({ behavior: 'smooth' })
  }

  return (
    <div id="api-tutorial" className="pt-5 mx-auto" style={{ maxWidth: 1200 }}>
      <Grid item xs={12} className={props.classes.root}>
        <Button onClick={() => props.router.navigate('/landing')}>
          About Us
        </Button>
        <span className={props.classes.separator}>
          <ArrowRightIcon />
        </span>
        <Button className={props.classes.currentButton} disabled>
          Tutorial
        </Button>
      </Grid>
      <h1>Data Profiler API Wrapper Tutorial</h1>
      <p>{/*kg-card-begin: html*/}</p>
      <div className="dp-tutorial">
        <div className="dp-menu">
          <a
            href="javascript:void()"
            onClick={(e) => handleClick(e, 'getting-started')}>
            Getting Started
          </a>
          <br />
          <a
            href="javascript:void()"
            onClick={(e) => handleClick(e, 'dataset-class')}>
            Working with Datasets
          </a>
          <br />
          <a
            href="javascript:void()"
            onClick={(e) => handleClick(e, 'table-class')}>
            Working with Tables
          </a>
          <br />
          <a
            href="javascript:void()"
            onClick={(e) => handleClick(e, 'column-class')}>
            Working with Columns
          </a>
        </div>
        {/*kg-card-end: html*/}
        {/*kg-card-begin: html*/}
        <a className="anchor" name="getting-started" />
        {/*kg-card-end: html*/}
        {/*kg-card-begin: markdown*/}
        <h1 id="gettingstarted">Getting Started</h1>
        <h2 id="installpythonsdk">Install Python SDK</h2>
        <h5 id="downloaddataprofilerclient">Download Data Profiler Client</h5>
        <p>
          <a href="/clients/DataProfilerClientV2.0.zip">Version 2.0</a>
          <br />
          <a href="/clients/DataProfilerClientV1.1.zip">Version 1.1</a>
          <br />
          <a href="/clients/DataProfilerClientV1.0.zip">Version 1.0</a>
        </p>
        <p>
          To install the Python SDK, download the zip file from the link above.
          Once downloaded, unzip then run the setup.py script with the two
          following commands:
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">python setup</span>
            <span className="pun">.</span>
            <span className="pln">py build{'\n'}python setup</span>
            <span className="pun">.</span>
            <span className="pln">py install</span>
          </code>
        </pre>
        <p>
          which will install dp_external_client as a package on your local
          environment that can then be imported. If you get a permission error
          when running the install above, add --user to the end of the install
          command.
        </p>
        <p>
          Initialize Python SDK
          <br />
          Once you have the SDK installed you can connect to the API.
          <br />A simple way to test your connection is by requesting
          environment metadata. Contact the Data Profiler team to request an API
          key for a particular Data Profiler Environment (
          )
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # import instructions will depend on where you have installed the
              client
            </span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> sys{'\n'}sys</span>
            <span className="pun">.</span>
            <span className="pln">path</span>
            <span className="pun">.</span>
            <span className="pln">append</span>
            <span className="pun">(</span>
            <span className="str">".."</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> dp_external_client </span>
            <span className="kwd">as</span>
            <span className="pln">
              {' '}
              dpec{'\n'}
              {'\n'}data_profiler_url{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              'https://{'{'}environment{'}'}-api.dataprofiler.com'
            </span>
            <span className="pln">{'\n'}api_key </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              '{'{'}your-api-key{'}'}'
            </span>
            <span className="pln">
              {'\n'}
              {'\n'}env{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Environment</span>
            <span className="pun">(</span>
            <span className="pln">api_key</span>
            <span className="pun">=</span>
            <span className="pln">api_key</span>
            <span className="pun">,</span>
            <span className="pln"> url</span>
            <span className="pun">=</span>
            <span className="pln">data_profiler_url</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}env</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetList</span>
            <span className="pun">()[:</span>
            <span className="lit">5</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="str">
              'Minority Health Indicators-Medical School Graduates By Race'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'PDAM'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-Mortality'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'CMS - Service Market Saturation and Utilization 2018-04-13'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CHEMBL-ebi-ac-uk'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">env</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetCount</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="lit">66</span>
          </code>
        </pre>
        <h2 id="accessingrowdata">Accessing row data</h2>
        <p>
          If you have a dataset and one or multiple tables you are interested in
          working with the SDK can create a reference to that data.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Table</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> table_name</span>
            <span className="pun">=</span>
            <span className="str">"athlete_attributes"</span>
            <span className="pun">)</span>
          </code>
        </pre>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getTableMetadata</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">{'{'}</span>
            <span className="str">'visibility'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'LIST.PUBLIC_DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'timestamp'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1592796975675</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'metadata_level'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'columnFamily'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'VERSIONED_METADATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'version_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'ff7c50a1-9946-45b4-909b-c731618cc6ea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'data_type'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'string'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'load_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'update_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_tables'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_columns'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">15630</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_unique_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'pat_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_num'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">-</span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'properties'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">{'{'}</span>
            <span className="str">'origin'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'upload'</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deleted'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deletedData'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">{'}'}</span>
          </code>
        </pre>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # getTableInfo() returns metadata for every column in the table
            </span>
            <span className="pln">{'\n'}</span>
            <span className="com">
              # "fid" is a column name and a key for the getTableInfo()
              dictionary
            </span>
            <span className="pln">{'\n'}athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getTableInfo</span>
            <span className="pun">()[</span>
            <span className="str">"weight"</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">{'{'}</span>
            <span className="str">'visibility'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'LIST.PUBLIC_DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'timestamp'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1592796975675</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'metadata_level'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'columnFamily'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'VERSIONED_METADATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'version_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'ff7c50a1-9946-45b4-909b-c731618cc6ea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'weight'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'weight'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'data_type'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'string'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'load_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'update_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_tables'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_columns'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1563</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_unique_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">137</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'pat_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_num'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">-</span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'properties'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">
              {'{'}
              {'}'},
            </span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deleted'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deletedData'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">{'}'}</span>
          </code>
        </pre>
        <p>
          Now you can load the row data for this table. The SDK stores this as a
          pandas data frame.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_df </span>
            <span className="pun">=</span>
            <span className="pln"> athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}athlete_attributes_df</span>
            <span className="pun">.</span>
            <span className="pln">shape</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> ALL ROWS</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">,</span>
            <span className="pln"> athlete_attributes </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.589404</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
              {'\n'}
              {'\n'}
              {'\n'}
              {'\n'}
            </span>
            <span className="pun">(</span>
            <span className="lit">1563</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">)</span>
          </code>
        </pre>
        <h2 id="limitingrowdataresultsforlargertables">
          Limiting row data results for larger tables
        </h2>
        <p>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
          </code>{' '}
          is experimental and will default to fetch all rows for a table. This
          can cause long wait times and it is often more realistic to limit the
          results. The SDK allows for result limiting by setting value filters.
          The following call to{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
          </code>{' '}
          will apply the filter before returning the results.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # filter down to rows where "weight" column matches "170"
            </span>
            <span className="pln">{'\n'}athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">setFilters</span>
            <span className="pun">({'{'}</span>
            <span className="str">"weight"</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">"170"</span>
            <span className="pun">]{'}'})</span>
            <span className="pln">{'\n'}filtered_results </span>
            <span className="pun">=</span>
            <span className="pln"> athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}filtered_results</span>
            <span className="pun">.</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> FILTERED TABLE</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">|</span>
            <span className="pln"> athlete_attributes {'\n'}</span>
            <span className="typ">Filter</span>
            <span className="pun">(</span>
            <span className="pln">s</span>
            <span className="pun">)</span>
            <span className="pln"> applied</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="pun">{'{'}</span>
            <span className="str">'weight'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'170'</span>
            <span className="pun">]{'}'}</span>
            <span className="pln"> </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.080578</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>2245</td>
                <td>United States</td>
                <td>3/20/82</td>
                <td>Granite City, Illinois</td>
                <td>Robbie Lawler</td>
                <td>American Top Team</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Robbie-Lawler-2245</td>
                <td>71</td>
              </tr>
              <tr>
                <th>1</th>
                <td>19726</td>
                <td>United States</td>
                <td>5/17/83</td>
                <td>San Diego, California</td>
                <td>Paul Bradley</td>
                <td>Alliance MMA</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Paul-Bradley-19726</td>
                <td>69</td>
              </tr>
              <tr>
                <th>2</th>
                <td>59608</td>
                <td>United States</td>
                <td>2/11/83</td>
                <td>Simpsonville, South Carolina</td>
                <td>Stephen Thompson</td>
                <td>Team Upstate Karate</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Stephen-Thompson-59608</td>
                <td>72</td>
              </tr>
              <tr>
                <th>3</th>
                <td>6765</td>
                <td>United States</td>
                <td>4/26/84</td>
                <td>Albuquerque, New Mexico</td>
                <td>Carlos Condit</td>
                <td>Jackson-Wink MMA</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Carlos-Condit-6765</td>
                <td>73</td>
              </tr>
              <tr>
                <th>4</th>
                <td>24539</td>
                <td>United States</td>
                <td>9/12/83</td>
                <td>Ada, Oklahoma</td>
                <td>Johny Hendricks</td>
                <td>Team Takedown</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Johny-Hendricks-24539</td>
                <td>69</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <h2 id="accessingvaluefrequencycountsforacolumn">
          Accessing value frequency counts for a column
        </h2>
        <p>
          Often it is convenient to use a frequency table to calculate summary
          statstics. The alternative is to load all of the rows for a given
          table.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com"># column reference</span>
            <span className="pln">{'\n'}weight_column </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Column</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> table_name</span>
            <span className="pun">=</span>
            <span className="str">"athlete_attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> column_name</span>
            <span className="pun">=</span>
            <span className="str">"weight"</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}weight_column</span>
            <span className="pun">.</span>
            <span className="pln">listColumnCounts</span>
            <span className="pun">()[:</span>
            <span className="lit">5</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'170'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">229</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'155'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">209</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'185'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">166</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'145'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">133</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'205'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">123</span>
            <span className="pun">{'}'}]</span>
          </code>
        </pre>
        <h2 id="calculatemeanwithcolumn_counts">
          Calculate mean with column_counts
        </h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">weight_column_sum </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pln">{'\n'}count </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">for</span>
            <span className="pln"> i </span>
            <span className="kwd">in</span>
            <span className="pln"> weight_column</span>
            <span className="pun">.</span>
            <span className="pln">listColumnCounts</span>
            <span className="pun">():</span>
            <span className="pln">
              {'\n'}
              {'    '}
            </span>
            <span className="kwd">try</span>
            <span className="pun">:</span>
            <span className="pln">
              {'\n'}
              {'        '}weight_column_sum{' '}
            </span>
            <span className="pun">+=</span>
            <span className="pln"> int</span>
            <span className="pun">(</span>
            <span className="pln">i</span>
            <span className="pun">[</span>
            <span className="str">"value"</span>
            <span className="pun">])</span>
            <span className="pln"> </span>
            <span className="pun">*</span>
            <span className="pln"> i</span>
            <span className="pun">[</span>
            <span className="str">"count"</span>
            <span className="pun">]</span>
            <span className="pln">
              {'\n'}
              {'    '}
            </span>
            <span className="kwd">except</span>
            <span className="pln"> </span>
            <span className="typ">ValueError</span>
            <span className="pun">:</span>
            <span className="pln">
              {'\n'}
              {'        '}
            </span>
            <span className="kwd">pass</span>
            <span className="pln">
              {'\n'}
              {'\n'}weight_column_mean{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> weight_column_sum </span>
            <span className="pun">/</span>
            <span className="pln"> athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getTableInfo</span>
            <span className="pun">()[</span>
            <span className="str">"weight"</span>
            <span className="pun">][</span>
            <span className="str">"num_values"</span>
            <span className="pun">]</span>
            <span className="pln">{'\n'}weight_column_mean</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="lit">172.06589891234805</span>
          </code>
        </pre>
        <p>
          There are several nuances to notice. The returned list of dictionaries
          from{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">listColumnCounts</span>
            <span className="pun">()</span>
          </code>{' '}
          has two types,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="kwd">string</span>
          </code>{' '}
          for the{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="str">"value"</span>
          </code>{' '}
          key and{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="kwd">int</span>
          </code>{' '}
          for the{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="str">"count"</span>
          </code>{' '}
          key. As such, the user must explicitly interpret the type as shown
          above{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="kwd">int</span>
            <span className="pun">(</span>
            <span className="pln">i</span>
            <span className="pun">[</span>
            <span className="str">"value"</span>
            <span className="pun">])</span>
          </code>
          . Data Profiler occasionally recognizes types but most often this
          example holds true.
        </p>
        {/*kg-card-end: markdown*/}
        {/*kg-card-begin: html*/}
        <a className="anchor" name="dataset-class" />
        {/*kg-card-end: html*/}
        {/*kg-card-begin: markdown*/}
        <h1 id="workingwithdatasets">Working with Datasets</h1>
        <h2 id="initialize">Initialize</h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # import instructions will depend on where you have installed the
              client
            </span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> sys{'\n'}sys</span>
            <span className="pun">.</span>
            <span className="pln">path</span>
            <span className="pun">.</span>
            <span className="pln">append</span>
            <span className="pun">(</span>
            <span className="str">".."</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> dp_external_client </span>
            <span className="kwd">as</span>
            <span className="pln">
              {' '}
              dpec{'\n'}
              {'\n'}data_profiler_url{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              'https://{'{'}environment{'}'}-api.dataprofiler.com'
            </span>
            <span className="pln">{'\n'}api_key </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              '{'{'}your-api-key{'}'}'
            </span>
          </code>
        </pre>
        <h2 id="creatingadatasetobject">Creating a Dataset object</h2>
        <p>
          Creating a Dataset object requires you have a valid Dataset name for
          your chosen Data Profiler Environment.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_dataset </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Dataset</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">)</span>
          </code>
        </pre>
        <p>
          For environments that you are less familiar with or to ensure you have
          the proper dataset name string, use{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">env</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetList</span>
            <span className="pun">()</span>
          </code>
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">env</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetList</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="str">
              'Minority Health Indicators-Medical School Graduates By Race'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'PDAM'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-Mortality'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'CMS - Service Market Saturation and Utilization 2018-04-13'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CHEMBL-ebi-ac-uk'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'GeneTox'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'PREMIER'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'butterfly_modified'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Data Profiler'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CDC Vaccines'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'2018 CORE Challenge'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Public Data'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CMS'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'IPPS Provider Summary - Top 100 Diagnosis'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Women Health Disparities'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'synthea_10000'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Oecd'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Demographics'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-Births'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CMS - Medicare Disparities Study'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'ClinVar Genetic Variants'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MSD Spain HH'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'THIN'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Wh Mexico'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Survey on Drug Use and Health'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CPRD'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Employment Hours and Earnings'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Docgraph'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Oklahoma'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'TRUVEN_EARLYVIEW'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'OPENSNP'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Health Coverage'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'US Cancer Statistics'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CERNER DIM'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Austin Traffic Counts'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Dc'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'butterfly'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Bigfoot Sightings'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'National Cancer Institute'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'FLATIRON'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MEDICARE GOV DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Imss'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Mental Health Services Survey'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'icd9'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MEDSTAT'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Healthcare.gov Community Health Status Indicators'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Shul_Tool_Output'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Inegi'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB1'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Data Healthcare Gov'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'OMDB FLATIRON Q4'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Health Status'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'TRUVEN MEDICAID'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB3'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB2'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'synthea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'FDA Drug Database'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Baltimore'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'zach'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'BigFoot Sightings'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CCRIS NCI'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Clinical Trials GOV Studies'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-HIV AIDS'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'VEEVA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Frequent Itemsets'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <p>A more advanced approach could leverage the Omnisearch class.</p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">dataset_search </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Omnisearch</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> search_str</span>
            <span className="pun">=</span>
            <span className="str">"ath"</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}dataset_search</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetMatches</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[{'{'}</span>
            <span className="str">'dataset'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">{'}'}]</span>
          </code>
        </pre>
        <h2 id="datasetmetadata">Dataset metadata</h2>
        <p>
          The SDK gives access to all of the metadata available through the Data
          Profiler API.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_dataset</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetMetadata</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">{'{'}</span>
            <span className="str">'visibility'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'LIST.PUBLIC_DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'timestamp'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1592796975675</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'metadata_level'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'columnFamily'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'VERSIONED_METADATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'version_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'ff7c50a1-9946-45b4-909b-c731618cc6ea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'data_type'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'string'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'load_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'update_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_tables'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_columns'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">15630</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_unique_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'pat_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_num'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">-</span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'properties'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">{'{'}</span>
            <span className="str">'origin'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'upload'</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deleted'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deletedData'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">{'}'}</span>
          </code>
        </pre>
        <p>
          Additionally, there are helpers for parsing specific metadata fields.
        </p>
        <p>
          See{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getPullTimestamp</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getUpdateDate</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getUploaadDate</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getVisibility</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getValueCount</span>
          </code>
        </p>
        <h2 id="gettableandcolumninfo">Get table and column info</h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="kwd">print</span>
            <span className="pun">(</span>
            <span className="str">"Num columns in all tables: "</span>
            <span className="pln"> </span>
            <span className="pun">+</span>
            <span className="pln"> str</span>
            <span className="pun">(</span>
            <span className="pln">athlete_attributes_dataset</span>
            <span className="pun">.</span>
            <span className="pln">getColumnCount</span>
            <span className="pun">()))</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">(</span>
            <span className="str">"Num tables: "</span>
            <span className="pln"> </span>
            <span className="pun">+</span>
            <span className="pln"> str</span>
            <span className="pun">(</span>
            <span className="pln">athlete_attributes_dataset</span>
            <span className="pun">.</span>
            <span className="pln">getTableCount</span>
            <span className="pun">()))</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">(</span>
            <span className="str">"Table names: "</span>
            <span className="pln"> </span>
            <span className="pun">+</span>
            <span className="pln"> str</span>
            <span className="pun">(</span>
            <span className="pln">athlete_attributes_dataset</span>
            <span className="pun">.</span>
            <span className="pln">getTableList</span>
            <span className="pun">()))</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Num</span>
            <span className="pln"> columns </span>
            <span className="kwd">in</span>
            <span className="pln"> all tables</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Num</span>
            <span className="pln"> tables</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Table</span>
            <span className="pln"> names</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <h2 id="loadingrowdataforadataset">Loading row data for a Dataset</h2>
        <p>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">importDataset</span>
          </code>{' '}
          loads every table from a dataset, returning a dictionary with table
          names as keys and pandas dataframes as values.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">all_table_dfs </span>
            <span className="pun">=</span>
            <span className="pln"> athlete_attributes_dataset</span>
            <span className="pun">.</span>
            <span className="pln">importDataset</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}all_table_dfs</span>
            <span className="pun">[</span>
            <span className="str">"athlete_attributes"</span>
            <span className="pun">].</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> ALL ROWS</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">,</span>
            <span className="pln"> athlete_attributes </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.593543</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>29688</td>
                <td>Ireland</td>
                <td>7/14/88</td>
                <td>Dublin</td>
                <td>Conor McGregor</td>
                <td>SBG Ireland</td>
                <td>145</td>
                <td>Featherweight</td>
                <td>/fighter/Conor-McGregor-29688</td>
                <td>68</td>
              </tr>
              <tr>
                <th>1</th>
                <td>27944</td>
                <td>United States</td>
                <td>7/19/87</td>
                <td>Rochester, New York</td>
                <td>Jon Jones</td>
                <td>Jackson-Wink MMA</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Jon-Jones-27944</td>
                <td>76</td>
              </tr>
              <tr>
                <th>2</th>
                <td>75125</td>
                <td>United States</td>
                <td>10/17/81</td>
                <td>Albuquerque, New Mexico</td>
                <td>Holly Holm</td>
                <td>Jackson-Wink MMA</td>
                <td>135</td>
                <td>Bantamweight</td>
                <td>/fighter/Holly-Holm-75125</td>
                <td>68</td>
              </tr>
              <tr>
                <th>3</th>
                <td>12107</td>
                <td>United States</td>
                <td>9/3/85</td>
                <td>San Diego, California</td>
                <td>Dominick Cruz</td>
                <td>Alliance MMA</td>
                <td>134</td>
                <td>Bantamweight</td>
                <td>/fighter/Dominick-Cruz-12107</td>
                <td>68</td>
              </tr>
              <tr>
                <th>4</th>
                <td>45452</td>
                <td>United States</td>
                <td>8/13/86</td>
                <td>Kirkland, Washington</td>
                <td>Demetrious Johnson</td>
                <td>AMC Pankration</td>
                <td>125</td>
                <td>Flyweight</td>
                <td>/fighter/Demetrious-Johnson-45452</td>
                <td>63</td>
              </tr>
            </tbody>
          </table>
        </div>
        {/*kg-card-end: markdown*/}
        {/*kg-card-begin: html*/}
        <a className="anchor" name="table-class" />
        {/*kg-card-end: html*/}
        {/*kg-card-begin: markdown*/}
        <h1 id="workingwithtables">Working with Tables</h1>
        <h2 id="initialize">Initialize</h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # import instructions will depend on where you have installed the
              client
            </span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> sys{'\n'}sys</span>
            <span className="pun">.</span>
            <span className="pln">path</span>
            <span className="pun">.</span>
            <span className="pln">append</span>
            <span className="pun">(</span>
            <span className="str">".."</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> dp_external_client </span>
            <span className="kwd">as</span>
            <span className="pln">
              {' '}
              dpec{'\n'}
              {'\n'}data_profiler_url{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              'https://{'{'}environment{'}'}-api.dataprofiler.com'
            </span>
            <span className="pln">{'\n'}api_key </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              '{'{'}your-api-key{'}'}'
            </span>
          </code>
        </pre>
        <h2 id="creatingatableobject">Creating a Table object</h2>
        <p>
          Creating a Table object requires you have a valid Dataset name and
          Table name for your chosen Data Profiler Environment.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Table</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> table_name</span>
            <span className="pun">=</span>
            <span className="str">"athlete_attributes"</span>
            <span className="pun">)</span>
          </code>
        </pre>
        <h2 id="ensuringyouhaveavaliddatasetnameandtablename">
          Ensuring you have a valid Dataset name and Table name
        </h2>
        <p>
          You can cross reference dataset and table names using{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getDatasetList</span>
          </code>{' '}
          in the Environment class and{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getTableList</span>
          </code>{' '}
          in the Dataset class.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">env</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetList</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="str">
              'Minority Health Indicators-Medical School Graduates By Race'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'PDAM'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-Mortality'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'CMS - Service Market Saturation and Utilization 2018-04-13'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CHEMBL-ebi-ac-uk'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'GeneTox'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'PREMIER'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'butterfly_modified'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Data Profiler'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CDC Vaccines'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'2018 CORE Challenge'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Public Data'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CMS'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'IPPS Provider Summary - Top 100 Diagnosis'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Women Health Disparities'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'synthea_10000'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Oecd'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Demographics'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-Births'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CMS - Medicare Disparities Study'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'ClinVar Genetic Variants'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MSD Spain HH'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'THIN'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Wh Mexico'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Survey on Drug Use and Health'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CPRD'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Employment Hours and Earnings'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Docgraph'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Oklahoma'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'TRUVEN_EARLYVIEW'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'OPENSNP'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Health Coverage'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'US Cancer Statistics'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CERNER DIM'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Austin Traffic Counts'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Dc'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'butterfly'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Bigfoot Sightings'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'National Cancer Institute'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'FLATIRON'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MEDICARE GOV DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Imss'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'National Mental Health Services Survey'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'icd9'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'MEDSTAT'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Healthcare.gov Community Health Status Indicators'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Shul_Tool_Output'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Rwe Mexico Inegi'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB1'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Data Healthcare Gov'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'OMDB FLATIRON Q4'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">
              'Minority Health Indicators-Health Status'
            </span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'TRUVEN MEDICAID'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB3'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Patient Claims DB2'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'synthea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'FDA Drug Database'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Baltimore'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'zach'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'BigFoot Sightings'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'CCRIS NCI'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Clinical Trials GOV Studies'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Minority Health Indicators-HIV AIDS'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'VEEVA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'Frequent Itemsets'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">dpec</span>
            <span className="pun">.</span>
            <span className="typ">Dataset</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">).</span>
            <span className="pln">getTableList</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <h2 id="tablemetadata">Table Metadata</h2>
        <p>
          The SDK gives access to all of the metadata available through the Data
          Profiler API.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getTableMetadata</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">{'{'}</span>
            <span className="str">'visibility'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'LIST.PUBLIC_DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'timestamp'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1592796975675</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'metadata_level'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'columnFamily'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'VERSIONED_METADATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'version_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'ff7c50a1-9946-45b4-909b-c731618cc6ea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">None</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'data_type'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'string'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'load_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'update_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_tables'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_columns'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">15630</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_unique_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'pat_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_num'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">-</span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'properties'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">{'{'}</span>
            <span className="str">'origin'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'upload'</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deleted'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deletedData'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">{'}'}</span>
          </code>
        </pre>
        <p>
          Additionally, there are helpers for parsing specific metadata fields.
        </p>
        <p>
          See{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getPullTimestamp</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getUpdateDate</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getUploaadDate</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getVisibility</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getValueCount</span>
          </code>
        </p>
        <h1 id="columninfo">Column info</h1>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="kwd">print</span>
            <span className="pun">(</span>
            <span className="str">"Num columns in table: "</span>
            <span className="pln"> </span>
            <span className="pun">+</span>
            <span className="pln"> str</span>
            <span className="pun">(</span>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getColumnCount</span>
            <span className="pun">()))</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">print</span>
            <span className="pun">(</span>
            <span className="str">"Column names: "</span>
            <span className="pln"> </span>
            <span className="pun">+</span>
            <span className="pln"> str</span>
            <span className="pun">(</span>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">getColumnList</span>
            <span className="pun">()))</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Num</span>
            <span className="pln"> columns </span>
            <span className="kwd">in</span>
            <span className="pln"> table</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Column</span>
            <span className="pln"> names</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'fid'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'country\r'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'birth_date'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'name'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'locality'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'weight'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'association'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'class'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'url'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'height'</span>
            <span className="pun">]</span>
          </code>
        </pre>
        <h2 id="loadingrowswithvaluefilters">
          Loading rows with value filters
        </h2>
        <h3 id="limitingrowdataresultsforlargertables">
          Limiting row data results for larger tables
        </h3>
        <p>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
          </code>{' '}
          is experimental and will default to fetch all rows for a table. This
          can cause long wait times and it is often more realistic to limit the
          results.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">().</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> ALL ROWS</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">,</span>
            <span className="pln"> athlete_attributes </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">02.104466</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>29688</td>
                <td>Ireland</td>
                <td>7/14/88</td>
                <td>Dublin</td>
                <td>Conor McGregor</td>
                <td>SBG Ireland</td>
                <td>145</td>
                <td>Featherweight</td>
                <td>/fighter/Conor-McGregor-29688</td>
                <td>68</td>
              </tr>
              <tr>
                <th>1</th>
                <td>27944</td>
                <td>United States</td>
                <td>7/19/87</td>
                <td>Rochester, New York</td>
                <td>Jon Jones</td>
                <td>Jackson-Wink MMA</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Jon-Jones-27944</td>
                <td>76</td>
              </tr>
              <tr>
                <th>2</th>
                <td>75125</td>
                <td>United States</td>
                <td>10/17/81</td>
                <td>Albuquerque, New Mexico</td>
                <td>Holly Holm</td>
                <td>Jackson-Wink MMA</td>
                <td>135</td>
                <td>Bantamweight</td>
                <td>/fighter/Holly-Holm-75125</td>
                <td>68</td>
              </tr>
              <tr>
                <th>3</th>
                <td>12107</td>
                <td>United States</td>
                <td>9/3/85</td>
                <td>San Diego, California</td>
                <td>Dominick Cruz</td>
                <td>Alliance MMA</td>
                <td>134</td>
                <td>Bantamweight</td>
                <td>/fighter/Dominick-Cruz-12107</td>
                <td>68</td>
              </tr>
              <tr>
                <th>4</th>
                <td>45452</td>
                <td>United States</td>
                <td>8/13/86</td>
                <td>Kirkland, Washington</td>
                <td>Demetrious Johnson</td>
                <td>AMC Pankration</td>
                <td>125</td>
                <td>Flyweight</td>
                <td>/fighter/Demetrious-Johnson-45452</td>
                <td>63</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <p>
          The SDK allows for result limiting by setting value filters. The
          following call to{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
          </code>{' '}
          will apply the filter before returning the results. A filter can be
          applied to multiple columns.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="com">
              # filter down to rows where "weight" column matches "170"
            </span>
            <span className="pln">{'\n'}athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">setFilters</span>
            <span className="pun">({'{'}</span>
            <span className="str">"weight"</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">"170"</span>
            <span className="pun">]{'}'})</span>
            <span className="pln">{'\n'}filtered_results </span>
            <span className="pun">=</span>
            <span className="pln"> athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">()</span>
            <span className="pln">{'\n'}filtered_results</span>
            <span className="pun">.</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> FILTERED TABLE</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">|</span>
            <span className="pln"> athlete_attributes {'\n'}</span>
            <span className="typ">Filter</span>
            <span className="pun">(</span>
            <span className="pln">s</span>
            <span className="pun">)</span>
            <span className="pln"> applied</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="pun">{'{'}</span>
            <span className="str">'weight'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'170'</span>
            <span className="pun">]{'}'}</span>
            <span className="pln"> </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.328070</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>2245</td>
                <td>United States</td>
                <td>3/20/82</td>
                <td>Granite City, Illinois</td>
                <td>Robbie Lawler</td>
                <td>American Top Team</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Robbie-Lawler-2245</td>
                <td>71</td>
              </tr>
              <tr>
                <th>1</th>
                <td>19726</td>
                <td>United States</td>
                <td>5/17/83</td>
                <td>San Diego, California</td>
                <td>Paul Bradley</td>
                <td>Alliance MMA</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Paul-Bradley-19726</td>
                <td>69</td>
              </tr>
              <tr>
                <th>2</th>
                <td>59608</td>
                <td>United States</td>
                <td>2/11/83</td>
                <td>Simpsonville, South Carolina</td>
                <td>Stephen Thompson</td>
                <td>Team Upstate Karate</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Stephen-Thompson-59608</td>
                <td>72</td>
              </tr>
              <tr>
                <th>3</th>
                <td>6765</td>
                <td>United States</td>
                <td>4/26/84</td>
                <td>Albuquerque, New Mexico</td>
                <td>Carlos Condit</td>
                <td>Jackson-Wink MMA</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Carlos-Condit-6765</td>
                <td>73</td>
              </tr>
              <tr>
                <th>4</th>
                <td>24539</td>
                <td>United States</td>
                <td>9/12/83</td>
                <td>Ada, Oklahoma</td>
                <td>Johny Hendricks</td>
                <td>Team Takedown</td>
                <td>170</td>
                <td>Welterweight</td>
                <td>/fighter/Johny-Hendricks-24539</td>
                <td>69</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <h3 id="unsetfilters">Unset filters</h3>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">setFilters</span>
            <span className="pun">
              ({'{'}
              {'}'})
            </span>
            <span className="pln">{'\n'}athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRows</span>
            <span className="pun">().</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> ALL ROWS</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">,</span>
            <span className="pln"> athlete_attributes </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">02.010845</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>29688</td>
                <td>Ireland</td>
                <td>7/14/88</td>
                <td>Dublin</td>
                <td>Conor McGregor</td>
                <td>SBG Ireland</td>
                <td>145</td>
                <td>Featherweight</td>
                <td>/fighter/Conor-McGregor-29688</td>
                <td>68</td>
              </tr>
              <tr>
                <th>1</th>
                <td>27944</td>
                <td>United States</td>
                <td>7/19/87</td>
                <td>Rochester, New York</td>
                <td>Jon Jones</td>
                <td>Jackson-Wink MMA</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Jon-Jones-27944</td>
                <td>76</td>
              </tr>
              <tr>
                <th>2</th>
                <td>75125</td>
                <td>United States</td>
                <td>10/17/81</td>
                <td>Albuquerque, New Mexico</td>
                <td>Holly Holm</td>
                <td>Jackson-Wink MMA</td>
                <td>135</td>
                <td>Bantamweight</td>
                <td>/fighter/Holly-Holm-75125</td>
                <td>68</td>
              </tr>
              <tr>
                <th>3</th>
                <td>12107</td>
                <td>United States</td>
                <td>9/3/85</td>
                <td>San Diego, California</td>
                <td>Dominick Cruz</td>
                <td>Alliance MMA</td>
                <td>134</td>
                <td>Bantamweight</td>
                <td>/fighter/Dominick-Cruz-12107</td>
                <td>68</td>
              </tr>
              <tr>
                <th>4</th>
                <td>45452</td>
                <td>United States</td>
                <td>8/13/86</td>
                <td>Kirkland, Washington</td>
                <td>Demetrious Johnson</td>
                <td>AMC Pankration</td>
                <td>125</td>
                <td>Flyweight</td>
                <td>/fighter/Demetrious-Johnson-45452</td>
                <td>63</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <h3 id="otherrowloadingoperations">Other row loading operations</h3>
        <p>
          As an alternative to setting filters on the Table class you can
          leverage{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">loadRowsInRange</span>
          </code>{' '}
          and{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">loadRowsWithSubstringMatches</span>
          </code>
          .
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRowsInRange</span>
            <span className="pun">(</span>
            <span className="lit">200</span>
            <span className="pun">,</span>
            <span className="lit">210</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">"weight"</span>
            <span className="pun">).</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> FILTERED TABLE</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">|</span>
            <span className="pln"> athlete_attributes {'\n'}</span>
            <span className="typ">Filter</span>
            <span className="pun">(</span>
            <span className="pln">s</span>
            <span className="pun">)</span>
            <span className="pln"> applied</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="pun">{'{'}</span>
            <span className="str">'weight'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'200'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'201'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'202'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'203'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'204'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'205'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'206'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'207'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'208'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'209'</span>
            <span className="pun">]{'}'}</span>
            <span className="pln"> </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.412331</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>27944</td>
                <td>United States</td>
                <td>7/19/87</td>
                <td>Rochester, New York</td>
                <td>Jon Jones</td>
                <td>Jackson-Wink MMA</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Jon-Jones-27944</td>
                <td>76</td>
              </tr>
              <tr>
                <th>1</th>
                <td>162</td>
                <td>United States</td>
                <td>8/10/71</td>
                <td>Columbus, Ohio</td>
                <td>Kevin Randleman</td>
                <td>Tapout Training Center</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Kevin-Randleman-162</td>
                <td>70</td>
              </tr>
              <tr>
                <th>2</th>
                <td>10200</td>
                <td>United States</td>
                <td>9/25/79</td>
                <td>Niagara Falls, New York</td>
                <td>Rashad Evans</td>
                <td>Blackzilians</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Rashad-Evans-10200</td>
                <td>71</td>
              </tr>
              <tr>
                <th>3</th>
                <td>22858</td>
                <td>United States</td>
                <td>6/7/83</td>
                <td>Tempe, Arizona</td>
                <td>Ryan Bader</td>
                <td>Power MMA Team</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Ryan-Bader-22858</td>
                <td>74</td>
              </tr>
              <tr>
                <th>4</th>
                <td>26162</td>
                <td>Sweden</td>
                <td>1/15/87</td>
                <td>Stockholm, Stockholm County</td>
                <td>Alexander Gustafsson</td>
                <td>Allstars Training Center</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Alexander-Gustafsson-26162</td>
                <td>76</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">athlete_attributes_table</span>
            <span className="pun">.</span>
            <span className="pln">loadRowsWithSubstringMatches</span>
            <span className="pun">({'{'}</span>
            <span className="str">"birth_date"</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">"7/"</span>
            <span className="pun">]{'}'}).</span>
            <span className="pln">head</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[</span>
            <span className="lit">36mNote</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="typ">This</span>
            <span className="pln"> endpoint </span>
            <span className="kwd">is</span>
            <span className="pln"> experimental </span>
            <span className="pun">-</span>
            <span className="pln"> expect longer load times </span>
            <span className="kwd">for</span>
            <span className="pln"> larger datasets</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
            <span className="pln">
              {'\n'}
              {'\n'}
            </span>
            <span className="typ">Downloading</span>
            <span className="pln"> FILTERED TABLE</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="typ">Athlete</span>
            <span className="pln"> </span>
            <span className="typ">Attributes</span>
            <span className="pln"> </span>
            <span className="pun">|</span>
            <span className="pln"> athlete_attributes {'\n'}</span>
            <span className="typ">Filter</span>
            <span className="pun">(</span>
            <span className="pln">s</span>
            <span className="pun">)</span>
            <span className="pln"> applied</span>
            <span className="pun">:</span>
            <span className="pln">{'  '}</span>
            <span className="pun">{'{'}</span>
            <span className="str">'birth_date'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">[</span>
            <span className="str">'1/17/66'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/17/94'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/27/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/7/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/7/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/7/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'1/7/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/17/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/17/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/17/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/17/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/27/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/7/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'10/7/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/17/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/17/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/27/68'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/27/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/27/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/27/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/27/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/7/62'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/7/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'11/7/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/69'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/17/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'12/7/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/27/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/27/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/7/65'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/7/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/7/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/7/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'2/7/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/17/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/17/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/17/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/27/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/7/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/7/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/7/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/7/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'3/7/92'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/17/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/17/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/17/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/27/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/27/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/27/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/7/70'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'4/7/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/70'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/17/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/27/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/27/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/27/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/27/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/7/72'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/7/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'5/7/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/17/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/17/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/17/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/27/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/7/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/7/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'6/7/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/1/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/10/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/10/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/10/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/11/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/11/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/11/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/11/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/12/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/12/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/12/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/13/69'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/13/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/13/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/71'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/72'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/14/92'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/15/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/15/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/15/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/58'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/60'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/62'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/63'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/64'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/66'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/68'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/69'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/16/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/17/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/17/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/17/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/17/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/18/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/18/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/19/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/19/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/19/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/19/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/2/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/2/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/2/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/20/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/20/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/20/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/21/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/21/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/21/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/21/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/68'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/71'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/22/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/23/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/23/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/23/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/24/69'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/24/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/24/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/24/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/24/85'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/25/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/25/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/25/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/25/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/26/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/26/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/27/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/27/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/68'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/70'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/78'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/28/88'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/29/73'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/29/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/29/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/29/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/29/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/3/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/30/66'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/30/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/30/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/30/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/30/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/31/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/31/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/31/89'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/31/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/4/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/5/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/5/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/6/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/6/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/7/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/7/79'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/7/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/7/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/7/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/8/90'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/9/75'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/9/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'7/9/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/17/76'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/27/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/27/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/27/80'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/27/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/7/72'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/7/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'8/7/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/69'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/74'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/77'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/82'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/84'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/17/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/27/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/27/91'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/7/81'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/7/83'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/7/86'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/7/87'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'9/7/94'</span>
            <span className="pun">]{'}'}</span>
            <span className="pln"> </span>
            <span className="pun">...</span>
            <span className="pln">{'\n'}</span>
            <span className="pun">[</span>
            <span className="lit">32m</span>
            <span className="pln">{'\n'}</span>
            <span className="typ">Download</span>
            <span className="pln"> successful</span>
            <span className="pun">!</span>
            <span className="pln"> </span>
            <span className="typ">Request</span>
            <span className="pln"> completed </span>
            <span className="kwd">in</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">:</span>
            <span className="lit">00</span>
            <span className="pun">:</span>
            <span className="lit">01.958521</span>
            <span className="pun">[</span>
            <span className="lit">0m</span>
          </code>
        </pre>
        <div>
          <table border={1} className="dataframe">
            <thead>
              <tr style={{ textAlign: 'right' }}>
                <th />
                <th>fid</th>
                <th>country</th>
                <th>birth_date</th>
                <th>locality</th>
                <th>name</th>
                <th>association</th>
                <th>weight</th>
                <th>class</th>
                <th>url</th>
                <th>height</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th>0</th>
                <td>29688</td>
                <td>Ireland</td>
                <td>7/14/88</td>
                <td>Dublin</td>
                <td>Conor McGregor</td>
                <td>SBG Ireland</td>
                <td>145</td>
                <td>Featherweight</td>
                <td>/fighter/Conor-McGregor-29688</td>
                <td>68</td>
              </tr>
              <tr>
                <th>1</th>
                <td>27944</td>
                <td>United States</td>
                <td>7/19/87</td>
                <td>Rochester, New York</td>
                <td>Jon Jones</td>
                <td>Jackson-Wink MMA</td>
                <td>205</td>
                <td>Light Heavyweight</td>
                <td>/fighter/Jon-Jones-27944</td>
                <td>76</td>
              </tr>
              <tr>
                <th>2</th>
                <td>75125</td>
                <td>United States</td>
                <td>10/17/81</td>
                <td>Albuquerque, New Mexico</td>
                <td>Holly Holm</td>
                <td>Jackson-Wink MMA</td>
                <td>135</td>
                <td>Bantamweight</td>
                <td>/fighter/Holly-Holm-75125</td>
                <td>68</td>
              </tr>
              <tr>
                <th>3</th>
                <td>8390</td>
                <td>Brazil</td>
                <td>7/30/77</td>
                <td>Porto Alegre, Rio Grande do Sul</td>
                <td>Fabricio Werdum</td>
                <td>Werdum Combat Team</td>
                <td>242</td>
                <td>Heavyweight</td>
                <td>/fighter/Fabricio-Werdum-8390</td>
                <td>76</td>
              </tr>
              <tr>
                <th>4</th>
                <td>25097</td>
                <td>Brazil</td>
                <td>5/27/87</td>
                <td>Fortaleza, Ceara</td>
                <td>Diego Brandao</td>
                <td>Jackson-Wink MMA</td>
                <td>145</td>
                <td>Featherweight</td>
                <td>/fighter/Diego-Brandao-25097</td>
                <td>67</td>
              </tr>
            </tbody>
          </table>
          <br />
        </div>
        <p>
          You can expose underlying substring matches using{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getSubstringValueMatches</span>
          </code>
        </p>
        {/*kg-card-end: markdown*/}
        {/*kg-card-begin: html*/}
        <a className="anchor" name="column-class" />
        {/*kg-card-end: html*/}
        {/*kg-card-begin: markdown*/}
        <h1 id="workingwithcolumns">Working with Columns</h1>
        <h2 id="initialize">Initialize</h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">i</span>
            <span className="com">
              # import instructions will depend on where you have installed the
              client
            </span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> sys{'\n'}sys</span>
            <span className="pun">.</span>
            <span className="pln">path</span>
            <span className="pun">.</span>
            <span className="pln">append</span>
            <span className="pun">(</span>
            <span className="str">".."</span>
            <span className="pun">)</span>
            <span className="pln">{'\n'}</span>
            <span className="kwd">import</span>
            <span className="pln"> dp_external_client </span>
            <span className="kwd">as</span>
            <span className="pln">
              {' '}
              dpec{'\n'}
              {'\n'}data_profiler_url{' '}
            </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              'https://{'{'}environment{'}'}-api.dataprofiler.com'
            </span>
            <span className="pln">{'\n'}api_key </span>
            <span className="pun">=</span>
            <span className="pln"> </span>
            <span className="str">
              '{'{'}your-api-key{'}'}'
            </span>
          </code>
        </pre>
        <h2 id="creatingacolumnobject">Creating a Column object</h2>
        <p>
          Creating a Column object requires that you have a valid dataset, table
          and column name for your chose Data Profiler Environment. See{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Working</span>
            <span className="pln"> </span>
            <span className="kwd">with</span>
            <span className="pln"> </span>
            <span className="typ">Tables</span>
          </code>{' '}
          to see strategies for validating dataset, table and column names using{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Environment</span>
            <span className="pun">.</span>
            <span className="pln">getDatasetList</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Dataset</span>
            <span className="pun">.</span>
            <span className="pln">getTableList</span>
          </code>{' '}
          and{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="typ">Table</span>
            <span className="pun">.</span>
            <span className="pln">getColumnList</span>
          </code>
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">weight_column </span>
            <span className="pun">=</span>
            <span className="pln"> dpec</span>
            <span className="pun">.</span>
            <span className="typ">Column</span>
            <span className="pun">(</span>
            <span className="pln">environment</span>
            <span className="pun">=</span>
            <span className="pln">env</span>
            <span className="pun">,</span>
            <span className="pln"> dataset_name</span>
            <span className="pun">=</span>
            <span className="str">"Athlete Attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> table_name</span>
            <span className="pun">=</span>
            <span className="str">"athlete_attributes"</span>
            <span className="pun">,</span>
            <span className="pln"> column_name</span>
            <span className="pun">=</span>
            <span className="str">"weight"</span>
            <span className="pun">)</span>
          </code>
        </pre>
        <h2 id="columnmetadata">Column Metadata</h2>
        <p>
          The SDK gives access to all of the metadata available through the Data
          Profiler API.
        </p>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">weight_column</span>
            <span className="pun">.</span>
            <span className="pln">getColumnMetadata</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">{'{'}</span>
            <span className="str">'visibility'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'LIST.PUBLIC_DATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'timestamp'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1592796975675</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'metadata_level'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'columnFamily'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'VERSIONED_METADATA'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'version_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'ff7c50a1-9946-45b4-909b-c731618cc6ea'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'dataset_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'Athlete Attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'table_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'athlete_attributes'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'weight'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_display_name'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'weight'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'data_type'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'string'</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'load_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'update_time'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1538082417280</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_tables'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">0</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_columns'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1563</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'num_unique_values'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">137</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'pat_id'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'column_num'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">-</span>
            <span className="lit">1</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'properties'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="pun">
              {'{'}
              {'}'},
            </span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deleted'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">,</span>
            <span className="pln">{'\n'} </span>
            <span className="str">'deletedData'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="kwd">False</span>
            <span className="pun">{'}'}</span>
          </code>
        </pre>
        <p>
          Additionally, there are helpers for parsing specific metadata fields.
        </p>
        <p>
          See{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getColumnDataType</span>
          </code>
          ,
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getNAscount</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getUniqueValueCount</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getValueCount</span>
          </code>
          ,{' '}
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pln">getVisibility</span>
          </code>
        </p>
        <h2 id="columnvaluefrequencycounts">Column value frequency counts</h2>
        <pre>
          <code
            className="language-python prettyprint prettyprinted"
            style={{}}>
            <span className="pln">weight_column</span>
            <span className="pun">.</span>
            <span className="pln">listColumnCounts</span>
            <span className="pun">()</span>
          </code>
        </pre>
        <pre>
          <code className="prettyprint prettyprinted" style={{}}>
            <span className="pun">[{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'170'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">229</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'155'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">209</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'185'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">166</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'145'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">133</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'205'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">123</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'135'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">114</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'125'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">56</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">''</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">30</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'115'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">28</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'169'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">25</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'156'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">14</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'136'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">14</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'154'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">13</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'171'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">12</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'240'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">11</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'235'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">11</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'203'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">11</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'250'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'245'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'230'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'186'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">10</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'265'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">9</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'225'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">9</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'160'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">9</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'210'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">8</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'204'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">8</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'200'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">8</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'190'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">8</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'146'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">8</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'264'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">7</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'184'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">7</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'168'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">7</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'116'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">7</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'260'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">6</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'253'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'241'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'220'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'199'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'198'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'183'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'180'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'134'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">5</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'300'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'255'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'237'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'206'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'181'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'179'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'174'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'159'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'143'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">4</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'259'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'258'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'257'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'251'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'249'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'248'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'242'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'238'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'233'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'227'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'222'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'195'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'182'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'176'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'175'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'153'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'137'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">3</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'290'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'263'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'262'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'261'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'256'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'244'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'243'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'236'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'234'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'228'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'219'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'216'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'215'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'214'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'212'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'207'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'202'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'196'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'192'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'187'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'178'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'167'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'165'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'158'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'144'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'126'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'110'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">2</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'600'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'415'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'410'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'400'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'390'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'345'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'332'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'323'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'313'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'312'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'295'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'275'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'270'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'254'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'247'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'246'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'239'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'231'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'229'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'226'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'223'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'218'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'213'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'209'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'208'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'201'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'191'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'189'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'177'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'173'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'164'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'152'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'151'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'149'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'148'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'141'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'140'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'139'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'123'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'114'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'113'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'},</span>
            <span className="pln">{'\n'} </span>
            <span className="pun">{'{'}</span>
            <span className="str">'value'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="str">'105'</span>
            <span className="pun">,</span>
            <span className="pln"> </span>
            <span className="str">'count'</span>
            <span className="pun">:</span>
            <span className="pln"> </span>
            <span className="lit">1</span>
            <span className="pun">{'}'}]</span>
          </code>
        </pre>
        {/*kg-card-end: markdown*/}
        {/*kg-card-begin: html*/}
      </div>
      {/*kg-card-end: html*/}
      <p />
    </div>
  )
}

export default DPContext(withStyles(styles)(Tutorial))
