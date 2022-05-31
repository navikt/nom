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
    ident?: string
    icon?: string | undefined
}

const getImageUrl = (ident: string) => {
    console.log(ident, "IDENT")
    return `https://nom-ui2.intern.nav.no/teamcat/${ident}/photo`

}
const EnhetInfo = (props: EnhetInfoProps) => {
    const {title, lederNavn, ident, icon} = props


    return (
        <div className={enhetInfoStyles}>
            <div>
                <LinkPanel.Title>{title}</LinkPanel.Title>
                <LinkPanel.Description>Leder: <span> {lederNavn}</span></LinkPanel.Description>
            </div>
            <img 
                src={ident ? getImageUrl(ident) : mannMedFjes}
                className={css({ height: '100%'})}
                onError={() => {
                    console.log('Feil i henting av bilde')
                }}

            />
        </div>
    )
}

export default EnhetInfo