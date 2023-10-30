import { css } from "@emotion/react";
import { Heading, Label, Tag } from "@navikt/ds-react";
import { format } from "date-fns";
import React from "react";

import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import type { Ressurs } from "~/types/api-models";
import { DATE_FORMATS, formatVisningsnavn } from "~/utils";

export function RessursHasQuit({ ressurs }: { ressurs: Ressurs }) {
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
            {formatVisningsnavn(ressurs.visningsnavn)}
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
          <span>{SektorNameMapping[ressurs.sektor]}</span>
          <span>Startdato:</span>
          <span>
            {ressurs.startdato
              ? format(new Date(ressurs.startdato), DATE_FORMATS.no)
              : ""}
          </span>
          <span>Sluttdato:</span>
          <span>
            {ressurs.sluttdato
              ? format(new Date(ressurs.sluttdato), DATE_FORMATS.no)
              : ""}
          </span>
        </div>
      </div>
    </div>
  );
}
