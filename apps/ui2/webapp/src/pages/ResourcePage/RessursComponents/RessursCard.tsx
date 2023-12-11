import { css } from "@emotion/react";
import { Heading, Label, Tag } from "@navikt/ds-react";
import { format } from "date-fns";
import React from "react";

import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { blueTopBorder } from "~/resources/NomStyles";
import type { Ressurs } from "~/types/api-models";
import { DATE_FORMATS, formatVisningsnavn } from "~/utils";

export function RessursCard({
  ressurs,
  hasQuit = false,
}: {
  ressurs: Ressurs;
  hasQuit?: boolean;
}) {
  return (
    <div
      css={[
        css`
          padding: var(--a-spacing-8);
        `,
        blueTopBorder,
      ]}
    >
      <div
        css={css`
          display: flex;
          align-items: center;
          gap: var(--a-spacing-4);
          margin-bottom: var(--a-spacing-4);
        `}
      >
        <Heading size="large">
          {formatVisningsnavn(ressurs.visningsnavn)}
        </Heading>
        {hasQuit && <Tag variant="warning">Sluttet</Tag>}
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
        <Label as="span">{SektorNameMapping[ressurs.sektor]}</Label>
        <span>Startdato:</span>
        <Label as="span">
          {ressurs.startdato
            ? format(new Date(ressurs.startdato), DATE_FORMATS.no)
            : ""}
        </Label>
        <span>Sluttdato:</span>
        <Label as="span">
          {ressurs.sluttdato
            ? format(new Date(ressurs.sluttdato), DATE_FORMATS.no)
            : "Ikke satt"}
        </Label>
      </div>
    </div>
  );
}
