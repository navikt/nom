import { css } from "@emotion/react";
import { PeopleFilled } from "@navikt/ds-icons";
import { Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router-dom";

import { useGetSession, useHasWriteAccess } from "~/api/useGetSession";
import { mediaQueryContentWidth } from "~/resources/NomStyles";

import { BurgerMenu } from "./BurgerMenu";
import { nomLogoCircle } from "./Images";
import { SearchBar } from "./select/Searchbar";

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

const userText = css({
  fontFamily: "Source Sans Pro",
  fontStyle: "normal",
  fontWeight: "700",
  fontSize: "16px",
  lineHeight: "30px",
  color: "var(--nom-interaction-primary)",
  marginRight: "1rem",
});

const NavHeader = () => {
  const session = useGetSession();
  const hasWrite = useHasWriteAccess();

  return (
    <div css={outerDiv}>
      <div css={innerDiv}>
        <Link
          as={ReactRouterLink}
          css={css`
            background-color: transparent !important;
            text-align: center;
          `}
          title={"Hjem"}
          to="/"
        >
          <img alt="Forside NOM" src={nomLogoCircle} />
        </Link>
        <SearchBar />
        <span
          css={css`
            display: flex;
            align-items: center;
          `}
        >
          <PeopleFilled
            css={css`
              color: var(--a-lightblue-800);
              width: 32px;
              height: 32px;
            `}
          />
          <span css={userText}>{session.data?.navident}</span>
          {hasWrite && <BurgerMenu />}
        </span>
      </div>
    </div>
  );
};

export default NavHeader;
