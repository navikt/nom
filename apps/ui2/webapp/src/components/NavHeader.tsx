import { useNavigate } from 'react-router'
import { getSession, SessionInterface } from '../api/getSession'
import React, { useEffect } from 'react'
import { css } from '@emotion/css'
import { Link } from 'react-router-dom'
import { nomLogoCircle } from './Images'
import { Search } from '@navikt/ds-react'
import { PeopleFilled } from '@navikt/ds-icons'
import {contentWidthByBreakpoint, mediaQueryContentWidth, NomColors, widthBreakpoints} from '../resources/NomStyles'

export const headerHeight = '6rem'

const outerDiv = css({
  display: "grid",
  gridTemplateColumns: "1fr",
  minHeight: headerHeight,
  width: '100%',
  justifyContent: "center",
  alignContent: "center"
})

const innerDiv = css(css`
  display: grid;
  justify-self: center;
  grid-template-columns: auto 1fr auto;
  column-gap: 2rem;
  @media (max-width: 60rem){
    grid-template-columns: 1fr;
    justify-items: left;
    row-gap: 1rem;
    padding: 1rem 0;
  }
`,mediaQueryContentWidth)

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

const searchBarStyle = css({
  minWidth: '18rem',
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
  const [session, setSession] = React.useState<any>()

  useEffect(() => {
    (async () => {
      const res = await getSession()
      setSession(res)
    })()
  }, [])

  return (
    <div className={outerDiv}>
      <div className={innerDiv}>
        <Link to='/' className={verticallyCenter}>
          <img src={nomLogoCircle} alt='logo'/>
        </Link>

        <Search
          disabled={true}
          placeholder={"Søkefunksjonalitet kommer"}
          className={searchBarStyle}
          label={'Søk etter ressurser'}
          variant='secondary'
          size='medium'
        ></Search>
        <span className={css`
          display: flex;
          align-items: center;
        `}>
        <PeopleFilled
          className={css({
            minWidth: "2rem"
          })}
          color='#236B7D'
          fontSize={'2rem'}
        />
        <span className={userText}>{session}</span>
        </span>
        {/* <BurgerMenu /> */}
      </div>
    </div>
  )
}

export default NavHeader

