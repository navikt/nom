import { css } from "@emotion/react";
import { Heading, Label, Tag } from "@navikt/ds-react";
import { format } from "date-fns";
import React from "react";

import type { HentRessursMedNavidentQuery } from "~/api/generated/graphql_generated";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { DATE_FORMATS } from "~/utils";

export function RessursHasQuit({
  ressurs,
}: {
  ressurs: NonNullable<HentRessursMedNavidentQuery["ressurs"]>;
}) {
  return (
    <div
      css={css`
        border-radius: 5px;
        background-color: var(--nom-secondary-hover);
        border: 1px solid var(--a-gray-400);
      `}
    >
      <HorizontalEdge />
      <div
        css={css`
          padding: 2rem;
        `}
      >
        <div
          css={css`
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 1rem;
          `}
        >
          <Heading level="2" size="large">
            {ressurs.visningsnavn}
          </Heading>
          <Tag variant="warning">Sluttet</Tag>
        </div>
        <div
          css={css`
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: var(--a-spacing-3);
          `}
        >
          <span>NAV-ident:</span>
          <Label as="span">{ressurs.navident}</Label>
          <span>Sektor:</span>
          <span>
            {ressurs.sektor
              .map((sektor) => SektorNameMapping[sektor])
              .join(", ")}
          </span>
          <span>Startdato:</span>
          <span>{format(new Date(ressurs.startdato), DATE_FORMATS.no)}</span>
          <span>Sluttdato:</span>
          <span>{format(new Date(ressurs.sluttdato), DATE_FORMATS.no)}</span>
        </div>
      </div>
    </div>
  );
}
