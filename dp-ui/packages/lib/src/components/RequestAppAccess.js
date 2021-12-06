import React from 'react'
import { Button } from '@material-ui/core'
import { Alert } from '@material-ui/lab'

const RequestAppAccess = (props) => {
  const visibilityError =
    props?.error?.includes('You do not have the visibilities') ||
    props?.showVisibilityError
  return (
    <Alert
      severity="error"
      variant="filled"
      style={{
        marginTop: '50px',
        display: 'inline-flex',
      }}
      action={
        visibilityError ? (
          <Button
            variant="outlined"
            color="inherit"
            size="small"
            href="https://forms.office.com/Pages/ResponsePage.aspx?id=7OQNoKhIpkO-dOMSdOIGDc_XWJ9dzUxMlr7IpT2nsK5UOTEyQUExSjBTN1g5NklNU1FHTldEUEIzUy4u"
            target="_blank"
            style={{ marginRight: 8 }}>
            REQUEST ACCESS
          </Button>
        ) : null
      }>
      {visibilityError
        ? 'You do not have the required access level'
        : 'Oops! Something went wrong, please try again later!'}
    </Alert>
  )
}

export default RequestAppAccess
