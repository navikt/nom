import { css } from "@emotion/css"
import { LinkPanel } from "@navikt/ds-react"
import { mannMedFjes } from "../Images"

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
    icon?: string | undefined
}

const EnhetInfo = (props: EnhetInfoProps) => {
    const {title, lederNavn, icon} = props

    return (
        <div className={enhetInfoStyles}>
            <div>
                <LinkPanel.Title>{title}</LinkPanel.Title>
                <LinkPanel.Description>Leder: <span> {lederNavn}</span></LinkPanel.Description>
            </div>
            <img src={mannMedFjes} className={css({ height: '100%'})}/>
        </div>
    )
}

export default EnhetInfo