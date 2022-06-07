import { Header } from '@navikt/ds-react-internal'
import { useNavigate } from 'react-router'
import { getSession, SessionInterface } from '../api/getSession'
import React, { useEffect } from 'react'
import { css } from '@emotion/css'

export const headerHeight = '3rem'

const userText = css({
  color: '#ffffff',
  marginLeft: 'Auto',
})

const NavHeader = () => {
  const [session, setSession] = React.useState<SessionInterface>()

  const navigate = useNavigate()

  useEffect(() => {
    ;(async () => {
      const res = await getSession()
      setSession(res)
    })()
  }, [])

  return (
    <Header
      onClick={() => {
        navigate('/')
      }}
      className='w-full'
      style={{ height: headerHeight }}
    >
      <Header.Title as='button'>NOM</Header.Title>
      <Header.User name={String(session) || 'Logg inn'} className={userText} />
    </Header>
  )
}

export default NavHeader
