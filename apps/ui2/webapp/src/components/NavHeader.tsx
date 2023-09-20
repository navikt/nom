import { css } from "@emotion/react";
import { Link as ReactRouterLink } from "@tanstack/react-router";

import nomLogoCircle from "~/resources/nomLogoCircle.svg";
import { mediaQueryContentWidth } from "~/resources/NomStyles";

import { BurgerMenu } from "./BurgerMenu";
import { SearchBar } from "./select/Searchbar";

export const headerHeight = "6rem";

const outerDiv = css`
  display: grid;
  grid-template-columns: 1fr;
  min-height: ${headerHeight};
  width: 100%;
  justify-content: center;
  align-content: center;
`;

const innerDiv = css(
  css`
    display: grid;
    justify-self: center;
    grid-template-columns: auto 1fr auto;
    column-gap: 2rem;
    align-items: center;

    @media (width <= 60rem) {
      grid-template-columns: 1fr;
      justify-items: left;
      row-gap: 1rem;
      padding: 1rem 0;
    }
  `,
  mediaQueryContentWidth,
);

export const NavHeader = () => {
  return (
    <div css={outerDiv}>
      <div css={innerDiv}>
        <ReactRouterLink title="Hjem" to="/">
          <img alt="Forside NOM" src={nomLogoCircle} />
        </ReactRouterLink>
        <SearchBar />
        <BurgerMenu />
      </div>
    </div>
  );
};
