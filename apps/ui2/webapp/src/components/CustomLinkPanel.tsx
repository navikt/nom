import { css } from "@emotion/css"
import { LinkPanel } from "@navikt/ds-react"
import { navLogo, ressursLogo } from "./Images"

const customLinkPanelStyles = css({
    display: "flex",
    alignItems: "center",
    padding: '10px'
})

type CustomLinkPanelProps = {
    title: string
    description: string
    linkTo: string
}

const CustomLinkPanel = (props: CustomLinkPanelProps) => {

    return (
        <>
            <LinkPanel href={props.linkTo}>
                <div className={customLinkPanelStyles}>
                <img src={ressursLogo} className={css({ marginRight: '50px'})} />
                <div>
                    <LinkPanel.Title>
                        {props.title}
                    </LinkPanel.Title>
                    <LinkPanel.Description>
                        {props.description}
                    </LinkPanel.Description>
                </div>
                </div>
            </LinkPanel>
        </>
    )
}

export default CustomLinkPanel