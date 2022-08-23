import { css } from "@emotion/css";

const contentStyle = css`
  margin: 0 auto 2.5rem;
  margin-top: 2rem;
  margin-bottom: 5rem;
  width: 95%;
  @media (min-width: 31.5rem) {
    width: 60%;
  }
  @media (min-width: 100rem) {
    width: 50%;
  }
  @media (min-width: 120rem) {
    width: 60rem;
  }
`

type ContentWrapperProps = {
    children: React.ReactElement
}

const ContentWrapper = (props: ContentWrapperProps) => (
    <div className={contentStyle}>
        {props.children}
    </div>
)

export default ContentWrapper