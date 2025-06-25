import { css } from "@emotion/react";
import { Link as ReactRouterLink } from "@tanstack/react-router";

import { SmartLink } from "~/components/navigation/SmartLink";
import nomLogoCircle from "~/resources/nomLogoCircle.svg";
import { defaultWidth } from "~/resources/NomStyles";

import { BurgerMenu } from "./BurgerMenu";
import { SearchBar } from "./select/Searchbar";

export const headerHeight = "6rem";

const outerDiv = css`
  display: grid;
  grid-template-columns: 1fr;
  min-height: ${headerHeight};
  width: 100%;
  place-content: center center;
  border-bottom: 1px solid var(--a-lightblue-800);
  padding-top: 1px;
`;

const innerDiv = css(
  css`
    display: grid;
    justify-self: center;
    grid-template-columns: auto 1fr auto;
    column-gap: var(--a-spacing-8);
    align-items: center;
  `,
  defaultWidth,
);

export const NavHeader = () => {
  return (
    <div css={outerDiv}>
      <div css={innerDiv}>
        <SmartLink href="/" title="Hjem">
          <img alt="Forside NOM" src={nomLogoCircle} />
        </SmartLink>
        <SearchBar />
        <BurgerMenu />
      </div>
    </div>
  );
};
