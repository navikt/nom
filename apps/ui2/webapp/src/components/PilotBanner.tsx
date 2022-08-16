import { css } from "@emotion/css"
import { Collapse, Expand } from "@navikt/ds-icons"
import { BodyShort, Button, Heading } from "@navikt/ds-react"
import { useState } from "react"
import { InfoIcon } from "./Images"

const outerDivStyles = css({
    width: '100%',
    minHeight: '50px',
    backgroundColor: '#DEECEF',
    borderBottom: '1px solid #236B7D',
    borderTop: '1px solid #236B7D',
})

const bannerStyles = css({
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: '0.5rem'
})

const iconStyles = css({
    height: '25px',
    width: '25px',
    marginRight: '0.5rem'
})

const textDivStyles = css({
    width: '30%',
    margin: '0 auto',
    paddingBottom: '0.7rem',
    paddingTop: '10px',
    marginBottom: '10px',
})

const buttonStyles = css({
    color: '#262626'
})

const buttonDivStyles = css({ display: 'flex', alignItems: 'center', marginLeft: '0.5rem' })

const PilotBanner = () => {
    const [isOpen, setIsOpen] = useState<boolean>(false)
    

    return (
        <div className={outerDivStyles}>
            <div className={bannerStyles} onClick={() => !isOpen ? setIsOpen(true) : setIsOpen(false)}>
                <img src={InfoIcon} alt="Pilot info" className={iconStyles}/>
                <Heading size="small">Løsningen er i pilot</Heading>
                <div className={buttonDivStyles}>
                    {isOpen && <Collapse fontSize='32px'/>}
                    {!isOpen && <Expand fontSize='32px'/> }
                </div>
            </div>

            {isOpen && (
                <div className={textDivStyles}>
                    <BodyShort size="medium" className={css({lineHeight: '28px'})}>
                        NOM presenterer det formelle organisasjonshierarkiet i NAV. 
                        Hierarkiet hentes per idag fra lønnssystemet, 
                        og oppsettet i lønnssystemet gjør at ansatte må knyttes til laveste nivå i organisasjonsstrukturen. 
                        Dette fører til at enkelte enheter gjentas flere ganger.
                    </BodyShort>
                </div>
            )}
        </div>
    )
}
export default PilotBanner