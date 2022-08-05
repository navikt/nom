import { css } from '@emotion/css'
import { BodyLong } from '@navikt/ds-react/esm/typography'
import { Link } from 'react-router-dom'
import { slackNom } from './Images'

export const footerHeigth = '9.5rem'

const outerDiv = css({
  width: '100%',
  bottom: '0',
  height: footerHeigth,
  background: '#134852',
})

const innerDiv = css({
  height: '100%',
  width: '50%',
  margin: 'auto',
  display: 'flex',
  alignItems: 'center',
  color: '#ffffff',
})

const slackLink = css({
  marginLeft: '3rem',
})

const FooterStyle = css({
  borderLeft: 'solid 1px #ffffff',
  borderRight: 'solid 1px #ffffff',
  paddingLeft: '1rem',
  paddingright: '1rem',
})

const footerText =
  'Organisasjonsstrukturen hentes rett fra lønnssystemet. Oppsett i lønnssystemet gjør at ansatte må knyttes til laveste nivå i organisasjonsstrukturen. Dette fører eksempelvis til at “HR-avdelingen” gjentas flere ganger.'

const Footer = () => {
  return (
    <div className={outerDiv}>
      <div className={innerDiv}>
        <div className={FooterStyle}>
          <BodyLong> {footerText}</BodyLong>
        </div>
        <a
          rel='noopener noreferrer'
          href='https://nav-it.slack.com/archives/C0362LA7ZLN'
          target='_blank'
          className={slackLink}
        >
          <img src={slackNom}></img>
        </a>
      </div>
    </div>
  )
}

export default Footer
