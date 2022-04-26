import { css } from "@emotion/css"
import { Radio } from "@navikt/ds-react"

const radioStyles = css({
    padding: '16px',
    width: '198px',
    height: '56px',
    border: '1px solid #6A6A6A',
    boxSizing: 'border-box',
    borderRadius: '4px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
})

const CustomRadio = () => {

    return (
        <Radio value="10" className={radioStyles}>Tekst</Radio>
    )
}

export default CustomRadio