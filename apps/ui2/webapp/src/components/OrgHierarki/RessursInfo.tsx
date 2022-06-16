import { css } from "@emotion/css"
import { BodyShort, Heading } from "@navikt/ds-react"
import { Ressurs, RessursKobling } from "../../api/generated/graphql_generated"
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2"
import TextWithLabel2 from "../TextWithLabel2"
import UserImage from "../UserImage"

const ressursInfoStyles = css({
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    flexDirection: 'column',
})

const metadataStyles = css({
    display: 'flex',
    width: '100%',
    justifyContent: 'center',
    marginLeft: '2rem'
})

const metadataColumnStyles = css({
    display: 'flex',
    flexDirection: 'column',
    marginTop: '1rem',
    wordBreak: 'break-all',
    maxWidth: '48%',
    minWidth: '30%'
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
                <div className={css({ marginTop: '2rem', marginBottom: '0.5rem' })}>
                    <UserImage navIdent={ressurs.navIdent} size="130px" />
                </div>

                <Heading size="large" >{ressurs.fornavn} {ressurs.etternavn}</Heading>
                <BodyShort size="medium">{`(${ressurs.navIdent})`}</BodyShort>
            </div>

            <div className={metadataStyles}>
                <div className={css(metadataColumnStyles, {marginRight: '1.5rem', textAlign: 'right'})}>
                    <TextWithLabel2 label="Tlf: " text="Mangler tlf" />
                    {ressurs.koblinger &&  ressurs.koblinger.map((kobling: RessursKobling) => (
                        <TextWithLabel2 label="Org-enhet" text={kobling.organisasjonsenhet.navn} />
                    ))}
                </div>

                <div className={metadataColumnStyles}>
                    <TextWithLabel2 label="E-post: " text={ressurs.epost || "Mangler tlf"} />
                    {ressurs.koblinger && ressurs.koblinger.map((kobling: RessursKobling) => (
                        <>
                            {kobling.organisasjonsenhet.leder && (
                                <TextWithLabel2 
                                    label="Leder: " 
                                    text={kobling.organisasjonsenhet.leder.length > 0 ? (kobling.organisasjonsenhet.leder[0].ressurs.fornavn as string ) as string : 'Mangler navn'} 
                                />
                            )}
                        </>
                    ))}
                </div>



                {/* {ressurs.koblinger && ressurs.koblinger.map((kobling: RessursKobling) => (
                    <div className={metadataRow}>
                        <TextWithLabel2 label="Org-enhet: " text={kobling.organisasjonsenhet.navn} />
                        <TextWithLabel2 label="Leder: " text={kobling.organisasjonsenhet.leder[0].ressurs.fornavn + ' ' + kobling.organisasjonsenhet.leder[0].ressurs.etternavn} />
                    </div>
                ))} */}
            </div>
        </CustomLinkPanel2>
    )
}

export default RessursInfo