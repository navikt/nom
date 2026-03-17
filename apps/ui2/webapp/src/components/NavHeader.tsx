import { css } from "@emotion/react";

import { SmartLink } from "~/components/navigation/SmartLink";
import nomLogoCircle from "~/resources/nomLogoCircle.svg";
import nomLogoCircleChristmas from "~/resources/nomLogoCircleChristmas.svg";
import { defaultWidth } from "~/resources/NomStyles";
import { isDecember } from "~/utils";

import { BurgerMenu } from "./BurgerMenu";
import { SearchBar } from "./select/Searchbar";

export const headerHeight = "6rem";

const displayChristmasLogo = isDecember();

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
    align-items: center;
  `,
  defaultWidth,
);

export const NavHeader = () => {
  return (
    <div css={outerDiv}>
      <div css={innerDiv}>
        <SmartLink
          css={css`
            ${`margin-right: var(--a-spacing-${displayChristmasLogo ? "3" : "8"})`};
          `}
          href="/"
          title="Hjem"
        >
          <img
            alt="Forside NOM"
            src={displayChristmasLogo ? nomLogoCircleChristmas : nomLogoCircle}
          />
        </SmartLink>

        <div
          css={css`
            margin-right: var(--a-spacing-8);
          `}
        >
          <SearchBar />
        </div>

        <BurgerMenu />
      </div>
    </div>
  );
};
