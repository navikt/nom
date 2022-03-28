import { css } from "@emotion/css";
import { navLogo } from "./Images";

export const headerHeight = '90px'

const headerStyle = css({
  height: headerHeight,
  width: '100%',
  backgroundColor: '"FFFFFFF"',
  display: 'flex',
  alignItems: 'center',
  paddingLeft: '40px'
})

const titleStyle = css({
  fontFamily: 'Source Sans Pro',
  fontStyle: 'normal',
  fontWeight: '700',
  fontSize: '24px',
  lineHeight: '30px',
  color: '#000000',
  marginLeft: '30px'
})

const logoStyle = css({
  height: '55px'
})


const Header = () => {
  return (
    <div className={headerStyle}>
      <img src={navLogo} alt="logo" className={logoStyle} />
      <span className={titleStyle}>NOM</span>
    </div>
  )
};

export default Header
