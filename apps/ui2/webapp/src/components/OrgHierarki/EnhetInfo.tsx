import { css } from "@emotion/css"
import { LinkPanel } from "@navikt/ds-react"
import { LinkPanelType } from "../CustomLinkPanel2"
import TextWithLabel2 from "../TextWithLabel2"
import UserImage from "../UserImage"

const enhetInfoStyles = css({
    height: '100%',
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between'
})

type EnhetInfoProps = {
    title: string
    lederNavn: string
    ident?: string
    icon?: string | undefined
    overenhet?: boolean
    type: LinkPanelType
}

const EnhetInfo = (props: EnhetInfoProps) => {
    const {title, lederNavn, ident, overenhet, type} = props

    const getSize = () => {
        if (type === LinkPanelType.CURRENT) return '130px'
        else if (type === LinkPanelType.LARGE) return '90px'
        else return undefined
    }

    return (
        <div className={enhetInfoStyles}>
            <div>
                <LinkPanel.Title>{title}</LinkPanel.Title>
                <LinkPanel.Description>Leder: <span className={css({ fontWeight: '600'})}> {lederNavn}</span></LinkPanel.Description>
            </div>
            {!overenhet && <UserImage navIdent={ident} size={getSize()} />}
        </div>
    )
}

export default EnhetInfo