import { css } from '@emotion/css'
import { Next } from '@navikt/ds-icons'

export enum LinkPanelType {
  CURRENT = 'current',
  OVERENHET = 'overenhet',
  KOBLING = 'kobling',
  RESSURS = 'ressurs',
  LARGE = 'large',
  SMALL = 'small',
}

const commonCss = css({
  height: '100px',
  width: '100%',
  display: 'flex',
  alignItems: 'center',
  marginBottom: '15px',
  padding: '30px 15px',
  border: '1px solid #B0B0B0',
  borderRadius: '5px',
})

const linkPanelCurrentStyles = css(commonCss, {
  backgroundColor: '#DEECEF',
  height: '160px',
  borderTop: '5px solid #236B7D',
})
const linkPanelLargeStyles = css(commonCss, {
  backgroundColor: '#FFFFFF',
  height: '130px',
  width: '100%',
  borderLeft: '5px solid #236B7D',
})
const linkPanelOverenhetStyles = css(commonCss, {
  backgroundColor: '#FFFFFF',
  height: '90px',
  width: '100%',
  marginBottom: '0px',
})
const linkPanelKoblingStyles = css(commonCss, {
  backgroundColor: '#FFFFFF',
  height: '70px',
  width: '100%',
  borderLeft: '5px solid #236B7D',
})
const linkPanelRessursStyles = css({
  backgroundColor: '#DEECEF',
  width: '100%',
  borderTop: '5px solid #236B7D',
  borderRadius: '5px',
  borderLeft: '1px solid #B0B0B0',
  borderRight: '1px solid #B0B0B0',
  borderBottom: '1px solid #B0B0B0',
})

type CustomLinkPanel2Props = {
  children: React.ReactNode
  type: LinkPanelType
}

const CustomLinkPanel2 = (props: CustomLinkPanel2Props) => {
  const { children, type } = props

  const setLinkPanelStyles = () => {
    if (type === LinkPanelType.CURRENT) return linkPanelCurrentStyles
    else if (type === LinkPanelType.OVERENHET) return linkPanelOverenhetStyles
    else if (type === LinkPanelType.LARGE) return linkPanelLargeStyles
    else if (type === LinkPanelType.KOBLING) return linkPanelKoblingStyles
    else if (type === LinkPanelType.RESSURS) return linkPanelRessursStyles
  }

  return (
    <>
      <div className={setLinkPanelStyles()}>
        {children}
        {type !== LinkPanelType.CURRENT && type !== LinkPanelType.RESSURS && (
          <Next className='navds-link-panel__chevron' />
        )}
      </div>
    </>
  )
}

export default CustomLinkPanel2
