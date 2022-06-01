import { css } from "@emotion/css"
import { LinkPanel } from "@navikt/ds-react"
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
}

const EnhetInfo = (props: EnhetInfoProps) => {
    const {title, lederNavn, ident, overenhet} = props


    return (
        <div className={enhetInfoStyles}>
            <div>
                <LinkPanel.Title>{title}</LinkPanel.Title>
                <LinkPanel.Description>Leder: <span> {lederNavn}</span></LinkPanel.Description>
            </div>
            {!overenhet && <UserImage navIdent={ident} />}
        </div>
    )
}

export default EnhetInfo