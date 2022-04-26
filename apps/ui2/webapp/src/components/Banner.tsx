import { css } from "@emotion/css"
import { Heading } from "@navikt/ds-react"

const bannerStyles = css({
    background: '#CCF1D6',
    width: '100%',
    height: '230px',
    paddingTop: '60px',
    paddingLeft: '110px'
})

const subHeaderStyles = css({
    font: 'Source Sans Pro',
    fontSize: '18px',
    fontStyle: 'normal',
    fontWeight: '400',
    lineHeight: '22px',
    letterSpacing: '0px',
})


const Banner = () => {

    return (
        <div className={bannerStyles}>
            <Heading size="xlarge">NAV Organisasjonsmaster</Heading>
            <p className={subHeaderStyles}>Kort tekst om hva Org.master er</p>
        </div>
    )
}

export default Banner