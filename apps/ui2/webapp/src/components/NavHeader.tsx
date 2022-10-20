import { css } from "@emotion/css";
import { PeopleFilled } from "@navikt/ds-icons";
import { Search } from "@navikt/ds-react";
import React from "react";
import { Link } from "react-router-dom";

import { UserRole, useSessionContext } from "../main";
import { mediaQueryContentWidth } from "../resources/NomStyles";
import BurgerMenu from "./BurgerMenu";
import { nomLogoCircle } from "./Images";

export const headerHeight = "6rem";

const outerDiv = css({
  display: "grid",
  gridTemplateColumns: "1fr",
  minHeight: headerHeight,
  width: "100%",
  justifyContent: "center",
  alignContent: "center",
});

const innerDiv = css(
  css`
    display: grid;
    justify-self: center;
    grid-template-columns: auto 1fr auto;
    column-gap: 2rem;
    @media (max-width: 60rem) {
      grid-template-columns: 1fr;
      justify-items: left;
      row-gap: 1rem;
      padding: 1rem 0;
    }
  `,
  mediaQueryContentWidth
);

// const titleStyle = css({
//   fontFamily: 'Source Sans Pro',
//   fontStyle: 'normal',
//   fontWeight: '700',
//   fontSize: '24px',
//   lineHeight: '30px',
//   color: NomColors.primaryColor,
//   marginLeft: '1rem',
//   marginRight: '4rem',
// })

const userText = css({
  fontFamily: "Source Sans Pro",
  fontStyle: "normal",
  fontWeight: "700",
  fontSize: "16px",
  lineHeight: "30px",
  color: "var(--nom-interaction-primary)",
  marginRight: "1rem",
});

const verticallyCenter = css({
  textAlign: "center",
});

const NavHeader = () => {
  const ses = useSessionContext();
  const hasWrite = ses.roles.includes(UserRole.ROLE_WRITE);

  return (
    <div className={outerDiv}>
      <div className={innerDiv}>
        <Link className={verticallyCenter} to="/">
          <img alt="logo" src={nomLogoCircle} />
        </Link>

        <Search
          disabled={true}
          label={"Søk etter ressurser"}
          placeholder={"Søkefunksjonalitet kommer"}
          size="medium"
          variant="secondary"
        ></Search>
        <span
          className={css`
            display: flex;
            align-items: center;
          `}
        >
          <PeopleFilled
            className={css({
              minWidth: "2rem",
            })}
            color="var(--nom-primary)"
            fontSize={"2rem"}
          />
          <span className={userText}>{ses.navident}</span>
          {hasWrite && <BurgerMenu />}
        </span>
      </div>
    </div>
  );
};

export default NavHeader;
