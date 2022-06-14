import { css } from "@emotion/css"
import { BodyShort, Heading } from "@navikt/ds-react"
import { Ressurs } from "../../api/generated/graphql_generated"
import CustomLinkPanel2,{ LinkPanelType }  from "../CustomLinkPanel2"
import TextWithLabel2 from "../TextWithLabel2"
import UserImage from "../UserImage"

const ressursInfoStyles = css({
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    flexDirection: 'column',
})

const metadataStyles = css({
    width: '50%',
})

const metadataRow = css({
    display: 'flex',
    justifyContent: 'space-between',
    marginTop: '1rem'
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
                
                <div className={metadataStyles}>
                    <div className={metadataRow}>
                        <TextWithLabel2 label="Tlf: " text="Mangler tlf" />
                        <TextWithLabel2 label="E-post: " text={ressurs.epost || 'Mangler e-post'} />
                    </div>
                </div>
            </div>
        </CustomLinkPanel2>
    )
}

export default RessursInfo