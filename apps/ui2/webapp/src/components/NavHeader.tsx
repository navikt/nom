import { useNavigate } from 'react-router'
import { getSession, SessionInterface } from '../api/getSession'
import React, { ReactNode, useEffect } from 'react'
import { css } from '@emotion/css'
import { Link, NavLink } from 'react-router-dom'
import { nomLogoCircle2 } from './Images'
import { Search } from '@navikt/ds-react'
import { PeopleFilled, System } from '@navikt/ds-icons'
import { Dropdown, Header } from '@navikt/ds-react-internal'
import BurgerMenu from './BurgerMenu'

export const headerHeight = '6rem'

const outerDiv = css({
  height: '6rem',
  width: '100%',
})

const innerDiv = css({
  // border: 'solid',
  color: 'black',
  height: '100%',
  width: '60%',
  margin: 'auto',
  display: 'flex',
  alignItems: 'center',
})

const titleStyle = css({
  fontFamily: 'Source Sans Pro',
  fontStyle: 'normal',
  fontWeight: '700',
  fontSize: '24px',
  lineHeight: '30px',
  color: '#236B7D',
  marginLeft: '1rem',
  marginRight: '4rem',
})

const userText = css({
  fontFamily: 'Source Sans Pro',
  fontStyle: 'normal',
  fontWeight: '700',
  fontSize: '16px',
  lineHeight: '30px',
  color: '#236B7D',
  marginLeft: '0.5rem',
  marginRight: '1rem',
})

const logoStyle = css({
  height: '55px',
  marginRight: '4rem',
})

const verticallyCenter = css({
  textAlign: 'center',
})

const searchBarStyle = css({
  marginRight: '4rem',
})

const placeholderMeny = css({
  marginLeft: '4rem',
  color: '#236B7D',
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
    <div className={outerDiv}>
      <div className={innerDiv}>
        <Link to='/' className={verticallyCenter}>
          <img src={nomLogoCircle2} alt='logo' className={logoStyle} />
        </Link>

        <Search
          className={searchBarStyle}
          label={'Søk etter ressurser'}
          size='medium'
        ></Search>
        <PeopleFilled color='#236B7D' fontSize={'2rem'} />
        <span className={userText}>H158887</span>
        <BurgerMenu />
        {/* <span className={placeholderMeny}>Meny</span> */}
      </div>
    </div>
  )
}

export default NavHeader
