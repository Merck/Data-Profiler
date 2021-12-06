import React, { useEffect, useState } from 'react'
import { CircularProgress } from '@material-ui/core'
import { get } from 'lodash'
import DPContext from './DPContext'

function LoginPrep(props) {
  const [initialized, setInitalized] = useState(
    get(props, 'dataprofiler.state.app.initialized', false)
  )
  const { dataprofiler, api } = props
  const { bulkSetDPState } = dataprofiler

  useEffect(() => {
    async function initialize() {
      await api
        .get({ resource: 'info', withoutAuth: true })
        .query({
          alt: process.env.USE_ALTERNATE_AUTH,
          uiPath: process.env.USER_FACING_UI_HTTP_PATH,
        })
        .then((res) => bulkSetDPState('session', res.body))
      setInitalized(true)
    }
    if (!initialized) {
      initialize()
    }
  }, [initialized])

  if (!initialized) return <CircularProgress />

  return props.children
}

const DPLoginPrep = DPContext(LoginPrep)

export default (InputComponent) => (props) =>
  (
    <DPLoginPrep {...props}>
      <InputComponent {...props} />
    </DPLoginPrep>
  )
