import { useNavigate } from 'react-router'
import { getSession, SessionInterface } from '../api/getSession'
import React, { useEffect } from 'react'
import { css } from '@emotion/css'
import { Link } from 'react-router-dom'
import { nomLogoCircle2 } from './Images'
import { Search } from '@navikt/ds-react'
import { PeopleFilled } from '@navikt/ds-icons'
import BurgerMenu from './BurgerMenu'
import { NomColors } from '../resources/NomStyles'

export const headerHeight = '6rem'

const outerDiv = css({
  height: headerHeight,
  width: '100%',
})

const innerDiv = css({
  // border: 'solid',
  color: 'black',
  height: '100%',
  width: '50%',
  margin: 'auto',
  display: 'flex',
  alignItems: 'center',
})

// const titleStyle = css({
//   fontFamily: 'Source Sans Pro',
//   fontStyle: 'normal',
//   fontWeight: '700',
//   fontSize: '24px',
//   lineHeight: '30px',
//   color: NomColors.primaryColor,
//   marginLeft: '1rem',
//   marginRight: '4rem',
// })

const userText = css({
  fontFamily: 'Source Sans Pro',
  fontStyle: 'normal',
  fontWeight: '700',
  fontSize: '16px',
  lineHeight: '30px',
  color: NomColors.primaryColor,
  marginRight: '1rem',
})

const logoStyle = css({
  height: '55px',
  marginRight: '4rem',
})

const verticallyCenter = css({
  textAlign: 'center',
})

export const searchBarStyle = css({
  marginRight: '10rem',
  width: '20em',
  color: NomColors.primaryColor,
  ':hover': {
    boxShadow: `inset 0 0 0 2px ${NomColors.primaryColor}`,
    border: NomColors.primaryColor,
  },
  ':focus': {
    boxShadow: `inset 0 0 0 2px ${NomColors.primaryColor}`,
  },

  button: {
    color: NomColors.primaryColor,

    ':hover': {
      backgroundColor: NomColors.primaryAccent,
      color: NomColors.primaryColor,
    },
    ':focus': {
      boxShadow: `inset 0 0 0 2px ${NomColors.primaryColor}`,
    },
  },
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
          variant='secondary'
          size='medium'
        ></Search>
        <PeopleFilled
          className={css({ marginRigh: '1em' })}
          color='#236B7D'
          fontSize={'2rem'}
        />
        <span className={userText}>{session}</span>
        {/* <BurgerMenu /> */}
      </div>
    </div>
  )
}

export default NavHeader
