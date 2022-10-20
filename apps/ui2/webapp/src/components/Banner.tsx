import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";

import ContentWrapper from "./ContentWrapper";
import { nomLogo } from "./Images";

const bannerStyles = css({
  background: "var(--navds-global-color-blue-900)",
  width: "100%",
  height: "230px",
  color: "var(--navds-global-color-white)",
  display: "flex",
  marginRight: "2.5rem",
});

const headerStyles = css({
  display: "flex",
  alignItems: "flex-end",
  marginLeft: "20px",
});

const Banner = () => {
  return (
    <div className={bannerStyles}>
      <ContentWrapper>
        <div className={css({ display: "flex", marginTop: "25px" })}>
          <img alt="nom logo" src={nomLogo} />
          <Heading className={headerStyles} size="xlarge">
            NAV Organisasjonsmaster
          </Heading>
        </div>
      </ContentWrapper>
    </div>
  );
};

export default Banner;
