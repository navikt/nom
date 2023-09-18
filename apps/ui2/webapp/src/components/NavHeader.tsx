import { css } from "@emotion/react";
import { PersonFillIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";

import { useGetSession, useHasWriteAccess } from "~/api/useGetSession";
import { ressursRoute } from "~/reactRouterRoutes";
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
  mediaQueryContentWidth,
);

export const NavHeader = () => {
  const session = useGetSession();
  const hasWrite = useHasWriteAccess();

  return (
    <div css={outerDiv}>
      <div css={innerDiv}>
        <ReactRouterLink title={"Hjem"} to="/">
          <img alt="Forside NOM" src={nomLogoCircle} />
        </ReactRouterLink>
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

            <ReactRouterLink
              params={{ id: session.data?.navident ?? "" }}
              to={ressursRoute.to}
            >
              <Link as="span">{session.data?.navident}</Link>
            </ReactRouterLink>
          </span>
          {hasWrite && <BurgerMenu />}
        </span>
      </div>
    </div>
  );
};
