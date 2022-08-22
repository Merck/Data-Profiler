import React, { useEffect } from 'react'
import CircularProgress from '@material-ui/core/CircularProgress'
import { parseQueryString } from '@dp-ui/lib/dist/helpers/uriHelpers'
import { DPContext } from '@dp-ui/lib'

const styles = { padding: 30 }

const RouOAuthCallback = (props) => {
    useEffect(() => {
        const payload = parseQueryString(window.location.search)
        props.api
            .post({
                resource: 'auth',
                postObject: {
                    ...payload,
                    uiPath: process.env.USER_FACING_UI_HTTP_PATH,
                    alt: process.env.USE_ALTERNATE_AUTH,
                },
                withoutAuth: true,
            })
            .then((res) => {
                props.dataprofiler.bulkSetDPState('session', {
                    username: res.body.username,
                    authToken: res.body.access_token,
                })
            })
            .then(() => {
                props.router.navigate('/')
            })
            .catch((e) => {
                console.error(e)
                props.router.navigateHard('/logout')
            })
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <div styles={styles}>
            <CircularProgress />
        </div>
    )
}

export default DPContext(RouOAuthCallback)