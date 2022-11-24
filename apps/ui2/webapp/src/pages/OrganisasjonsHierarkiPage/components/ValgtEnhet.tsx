import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";
import type { RefObject } from "react";
import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";

import type { OrgEnhet } from "../../../api/generated/graphql_generated";
import UserImage from "../../../components/UserImage";
import { Building } from "../../../resources/CustomIcons/Building";
import { HorizontalEdge } from "./CardViews";

const OrgenhetLederLink = (properties: { enhet: OrgEnhet }) => {
  const reference = useRef<HTMLAnchorElement>(null);

  const { agressoId, orgNiv, leder } = properties.enhet;
  const firstLeder = leder[0]?.ressurs;
  const { navident, visningsnavn } = firstLeder ?? {};

  // Give autofocus on initial load.
  // This way the user can continue navigation from where they were before page change.
  useEffect(() => {
    reference.current?.focus();
  }, []);

  if (!navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <Link
      className={css`
        color: var(--navds-global-color-gray-900);
      `}
      ref={reference}
      to={`/org/${agressoId}/${orgNiv}/${navident}`}
    >
      {visningsnavn}
    </Link>
  );
};

export const ValgtEnhet = ({
  valgtEnhet,
  scrollReference,
}: {
  valgtEnhet: OrgEnhet;
  scrollReference: RefObject<HTMLDivElement>;
}) => (
  <div
    className={css`
      display: grid;
      border-radius: 5px;
      margin-bottom: 1rem;
      background-color: var(--nom-secondary-hover);
      border: 1px solid var(--navds-global-color-gray-400);
    `}
    ref={scrollReference}
  >
    <HorizontalEdge />
    <div
      className={css`
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 2rem;
      `}
    >
      <div>
        <Heading size="large">
          <Building
            className={css`
              color: var(--navds-global-color-lightblue-800);
              margin-right: 1rem;
            `}
          />
          {valgtEnhet.navn}
        </Heading>
        <span>
          Leder: <OrgenhetLederLink enhet={valgtEnhet} />
        </span>
      </div>
      <UserImage
        navident={valgtEnhet.leder[0]?.ressurs.navident}
        navn={valgtEnhet.leder[0]?.ressurs.visningsnavn ?? ""}
        size="130px"
      />
    </div>
  </div>
);
