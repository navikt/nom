import { css } from "@emotion/css"
import { boldText, regularText } from "./Styles"

const labelStyles = css({
    regularText,
    marginBottom: '5px'
})

type TextWithLabelProps = {
    label: string,
    text: string,
}

const TextWithLabel = (props: TextWithLabelProps) => (
    <div>
        <div className={labelStyles}>{props.label}</div>
        <div className={boldText}>{props.text}</div>
    </div>
)

export default TextWithLabel