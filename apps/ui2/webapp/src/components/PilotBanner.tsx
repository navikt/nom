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
    borderTop: '1px solid #236B7D'
})

const bannerStyles = css({
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
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
})

const buttonStyles = css({
    color: '#262626'
})

const buttonDivStyles = css({ display: 'flex', alignItems: 'baseline', marginLeft: '1rem' })

const PilotBanner = () => {
    const [isOpen, setIsOpen] = useState<boolean>(false)
    

    return (
        <div className={outerDivStyles}>
            <div className={bannerStyles}>
                <img src={InfoIcon} alt="Pilot info" className={iconStyles}/>
                <Heading size="small">Løsningen er i pilot</Heading>
                <div className={buttonDivStyles}>
                    <Button variant="tertiary" onClick={() => !isOpen ? setIsOpen(true) : setIsOpen(false)} className={buttonStyles}>
                        {isOpen && <Collapse fontSize='32px'/>}
                        {!isOpen && <Expand fontSize='32px'/> }
                    </Button>
                </div>
            </div>

            {isOpen && (
                <div className={textDivStyles}>
                    <BodyShort size="medium" className={css({lineHeight: '28px'})}>
                        NOM presenterer det formelle organisasjonshierarkiet i NAV. 
                        Hierarkiet hentes rett fra lønnssystemet. 
                        Oppsett i lønnssystemet gjør at ansatte må knyttes til laveste nivå i organisasjonsstrukturen. 
                        Dette fører eksempelvis til at “HR-avdelingen” gjentas flere ganger.
                    </BodyShort>
                </div>
            )}
        </div>
    )
}
export default PilotBanner