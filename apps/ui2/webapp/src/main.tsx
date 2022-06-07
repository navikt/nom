import React from 'react'
import ReactDOM from 'react-dom'
import '@navikt/ds-css'
import '@navikt/ds-css-internal'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { ApolloProvider } from '@apollo/client'
import { apolloClient } from './api/apolloclient'
import { css } from '@emotion/css'
import AppRoutes from './routes'
import NavHeader, { headerHeight } from './components/NavHeader'

const layoutStyle = css({
  backgroundColor: '#F1F1F1',
  minHeight: `calc(100vh - ${headerHeight})`,
  width: '100%',
  display: 'flex',
})

ReactDOM.render(
  <React.StrictMode>
    <BrowserRouter>
      <ApolloProvider client={apolloClient}>
        <NavHeader />
        <div className={layoutStyle}>
          <div className={css({ width: '100%' })}>
            <AppRoutes />
          </div>
        </div>
      </ApolloProvider>
    </BrowserRouter>
  </React.StrictMode>,
  document.getElementById('root')
)
