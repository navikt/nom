import { css } from "@emotion/css";

const contentStyle = css({
    margin: "0 auto 2.5rem",
    marginTop: '2rem',
    marginBottom: '5rem',
    width: '50%'
})

type ContentWrapperProps = {
    children: React.ReactElement
}

const ContentWrapper = (props: ContentWrapperProps) => (
    <div className={contentStyle}>
        {props.children}
    </div>
)

export default ContentWrapper