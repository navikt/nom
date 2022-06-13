import { css } from "@emotion/css"
import { BodyShort, Heading } from "@navikt/ds-react"
import { Ressurs } from "../../api/generated/graphql_generated"
import CustomLinkPanel2,{ LinkPanelType }  from "../CustomLinkPanel2"
import TextWithLabel2 from "../TextWithLabel2"
import UserImage from "../UserImage"

const ressursInfoStyles = css({
    minHeight: '350px',
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    flexDirection: 'column',
})
const ressursMetadataStyles = css({
    width: '100%',
    marginTop: '30px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
})

type RessursInfoProps = {
    ressurs: Ressurs
}

const RessursInfo = (props: RessursInfoProps) => {
    const { ressurs } = props
    console.log(ressurs, "RESSURS")

    return (
        <CustomLinkPanel2 type={LinkPanelType.RESSURS}>
            <div className={ressursInfoStyles}>
                <div className={css({ marginTop: '2rem', marginBottom: '0.5rem'})}>
                    <UserImage navIdent={ressurs.navIdent} size="130px" />
                </div>

                <Heading size="large" >{ressurs.fornavn} {ressurs.etternavn}</Heading>
                <BodyShort size="medium">{`(${ressurs.navIdent})`}</BodyShort>

                <div className={ressursMetadataStyles}>
                    <div className={css({ marginRight: '30px'})}>
                        <TextWithLabel2 label="NAV-ident: " text={ressurs.navIdent} />
                        <TextWithLabel2 label="Startdato: " text="Mangler startdato" />
                        <TextWithLabel2 label="Org-enhet: " text="Mangler enhet" />
                    </div>
                    
                    <div className={css({ marginLeft: '20px'})}>
                        <TextWithLabel2 label="Tlf: " text="Mangler tlf" />
                        <TextWithLabel2 label="E-post: " text={ressurs.epost || 'Mangler e-post'} />
                        <TextWithLabel2 label="Leder: " text={ressurs.ledere[0].ressurs.visningsNavn || 'Mangler leder'} />
                    </div>
                </div>
            </div>
        </CustomLinkPanel2>
    )
}

export default RessursInfo