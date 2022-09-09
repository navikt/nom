import { css } from '@emotion/css'
import { Link } from 'react-router-dom'
import { navLogo } from './Images'

export const headerHeight2 = '90px'

const headerStyle = css({
  height: headerHeight2,
  width: '100%',
  backgroundColor: '"FFFFFFF"',
  display: 'flex',
  alignItems: 'center',
  paddingLeft: '40px',
})

const titleStyle = css({
  fontFamily: 'Source Sans Pro',
  fontStyle: 'normal',
  fontWeight: '700',
  fontSize: '24px',
  lineHeight: '30px',
  color: '#000000',
  marginLeft: '30px',
})

const logoStyle = css({
  height: '55px',
})

const Header = () => {
  return (
    <div className={headerStyle}>
      <Link to='/'>
        <img src={navLogo} alt='logo' className={logoStyle} />
      </Link>
      <span className={titleStyle}>NOM</span>
    </div>
  )
}

export default Header
