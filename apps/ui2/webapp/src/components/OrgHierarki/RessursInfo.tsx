import { css } from "@emotion/css"
import { Heading } from "@navikt/ds-react"
import React from "react"
import { Ressurs } from "../../api/generated/graphql_generated"
import CustomLinkPanel2,{ LinkPanelType }  from "../CustomLinkPanel2"
import TextWithLabel2 from "../TextWithLabel2"
import UserImage from "../UserImage"

const ressursInfoStyles = css({
    height: '350px',
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    flexDirection: 'column',
    paddingTop: '20px'
})

const ressursMetadataStyles = css({
    width: '100%',
    marginTop: '10px',
    display: 'flex',
    justifyContent: 'center'
})


type RessursInfoProps = {
    ressurs: Ressurs
}

const RessursInfo = (props: RessursInfoProps) => {
    const { ressurs } = props

    return (
        <CustomLinkPanel2 type={LinkPanelType.RESSURS}>
            <div className={ressursInfoStyles}>
                <UserImage navIdent={ressurs.navIdent} size="130px" />

                <Heading size="large">{ressurs.visningsNavn}</Heading>

                <div className={ressursMetadataStyles}>
                    <div className={css({ marginRight: '30px'})}>
                        <TextWithLabel2 label="NAV-ident: " text={ressurs.navIdent} />
                        <TextWithLabel2 label="Startdato: " text="Mangler startdato" />
                        <TextWithLabel2 label="Org-enhet: " text="Mangler enhet" />
                    </div>
                    
                    <div>
                        <TextWithLabel2 label="Tlf: " text="Mangler tlf" />
                        <TextWithLabel2 label="E-post: " text={ressurs.epost || 'Mangler e-post'} />
                        <TextWithLabel2 label="Leder:  " text={ressurs.ledere[0].ressurs.visningsNavn || 'Mangler leder'} />
                    </div>
                    
                </div>
            </div>
        </CustomLinkPanel2>
    )
}

export default RessursInfo