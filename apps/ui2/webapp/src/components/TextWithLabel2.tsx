import { css } from "@emotion/css"
import { BodyShort, Detail, Label } from "@navikt/ds-react"


const textStyles = css({ 
    display: 'flex',
    margin: '0px',
    marginBottom: '10px'
})

type TextWithLabel2Props = {
    label: string
    text: string
}

const TextWithLabel2 = (props: TextWithLabel2Props) => {
    
    return (
        <div className={textStyles}>
            <BodyShort size="medium">{props.label} &nbsp;</BodyShort>
            <Label size="medium">{props.text}</Label>
        </div>
)}

export default TextWithLabel2