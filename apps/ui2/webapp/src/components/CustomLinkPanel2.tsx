import { css } from "@emotion/css"
import { Next } from "@navikt/ds-icons"

export enum LinkPanelType {
    CURRENT='current',
    OVERENHET='overenhet',
    LARGE='large',
    SMALL='small'
}

const commonCss = css({
    height: "100px",
    width: "100%",
    display: "flex",
    alignItems: "center",
    marginBottom: '15px',
    padding: '30px 15px',
    border: '1px solid #B0B0B0',
    borderRadius: '5px'
  })
  
const linkPanelCurrentStyles = css(commonCss, {
    backgroundColor: "#CCE1FF",
    height: "160%",
    borderTop: '5px solid #134852'
  })
const linkPanelLargeStyles = css(commonCss, {
    backgroundColor: "#FFFFFF",
    width: "100%",
    borderLeft: '5px solid #134852'
})
const linkPanelOverenhetStyles = css(commonCss, {
    backgroundColor: "#FFFFFF",
    height: '90px',
    width: "100%",
    marginBottom: '0px'
})

type CustomLinkPanel2Props = {
    children: React.ReactNode
    type: LinkPanelType
}

const CustomLinkPanel2 = (props: CustomLinkPanel2Props) => {
    const { children, type} = props

    const setLinkPanelStyles = () => {
        if (type === LinkPanelType.CURRENT) return linkPanelCurrentStyles
        else if (type === LinkPanelType.OVERENHET) return linkPanelOverenhetStyles
        else if (type === LinkPanelType.LARGE) return linkPanelLargeStyles
    }

    return (
            <>
                <div className={setLinkPanelStyles()}>
                    {children}
                    {type !== LinkPanelType.CURRENT && <Next className="navds-link-panel__chevron" />}
                </div>
            </>
    )
}

export default CustomLinkPanel2