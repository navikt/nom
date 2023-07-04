import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router-dom";

import { useGetSession, useHasWriteAccess } from "~/api/useGetSession";
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

    @media (width <= 60rem) {
      grid-template-columns: 1fr;
      justify-items: left;
      row-gap: 1rem;
      padding: 1rem 0;
    }
  `,
  mediaQueryContentWidth
);

export const NavHeader = () => {
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
            gap: 1rem;
          `}
        >
          <span
            css={css`
              display: flex;
              align-items: center;
            `}
          >
            <PersonFillIcon
              css={css`
                color: var(--a-lightblue-800);
                width: 32px;
                height: 32px;
              `}
            />
            <Link
              as={ReactRouterLink}
              to={`/ressurs/${session.data?.navident}`}
            >
              {session.data?.navident}
            </Link>
          </span>
          {hasWrite && <BurgerMenu />}
        </span>
      </div>
    </div>
  );
};
