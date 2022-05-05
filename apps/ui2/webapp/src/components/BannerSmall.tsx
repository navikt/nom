import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";
import ContentWrapper from "./ContentWrapper";
import { nomLogo } from "./Images";

const bannerStyles = css({
  background: "#134852",
  width: "100%",
  height: "100px",
  color: "#FFFFFF",
  display: "flex",
  marginRight: '2.5rem'
});

const headerStyles = css({
    display: 'flex', 
    alignItems: 'flex-end',
    marginLeft: '20px',
})

type BannerSmallProps = {
    title: string
}

const BannerSmall = (props: BannerSmallProps) => {
  return (
    <div className={bannerStyles}>
      <ContentWrapper>
        <div className={css({ display: 'flex', marginBottom: '20px'})}> 
            <img src={nomLogo} alt="nom logo" height='55px'/>
            <Heading size="large" className={headerStyles}>{props.title}</Heading>
        </div>
      </ContentWrapper>
    </div>
  );
};

export default BannerSmall;