import { css } from "@emotion/css"
import { Detail, Label } from "@navikt/ds-react"


const textStyles = css({ display: 'flex', margin: '0px', marginBottom: '10px'})

type TextWithLabel2Props = {
    label: string
    text: string
}

const TextWithLabel2 = (props: TextWithLabel2Props) => {
    
    return (
        <div className={textStyles}>
            <Label size="small">{props.label} &nbsp;</Label>
            <Detail size="small">{props.text}</Detail>
        </div>
)}

export default TextWithLabel2