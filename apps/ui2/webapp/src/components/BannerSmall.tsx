import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";

import ContentWrapper from "./ContentWrapper";
import { nomLogo } from "./Images";

const bannerStyles = css({
  background: "var(--navds-global-color-lightblue-900)",
  width: "100%",
  height: "100px",
  color: "var(--navds-global-color-white)",
  display: "flex",
  marginRight: "2.5rem",
});

const headerStyles = css({
  display: "flex",
  alignItems: "flex-end",
  marginLeft: "20px",
});

type BannerSmallProperties = {
  title: string;
};

const BannerSmall = (properties: BannerSmallProperties) => {
  return (
    <div className={bannerStyles}>
      <ContentWrapper>
        <div className={css({ display: "flex", marginBottom: "20px" })}>
          <img alt="nom logo" height="55px" src={nomLogo} />
          <Heading className={headerStyles} size="large">
            {properties.title}
          </Heading>
        </div>
      </ContentWrapper>
    </div>
  );
};

export default BannerSmall;
