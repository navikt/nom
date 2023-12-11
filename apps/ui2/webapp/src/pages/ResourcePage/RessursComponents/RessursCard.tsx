import { css } from "@emotion/react";
import { Heading, Tag } from "@navikt/ds-react";
import { format } from "date-fns";
import React from "react";

import { LabelWithValue } from "~/components/LabelWithValue";
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
        <LabelWithValue label="NAV-ident" value={ressurs.navident} />
        <LabelWithValue
          label="Sektor"
          value={SektorNameMapping[ressurs.sektor]}
        />
        <LabelWithValue
          label="Startdato"
          value={
            ressurs.startdato &&
            format(new Date(ressurs.startdato), DATE_FORMATS.no)
          }
        />
        <LabelWithValue
          label="Sluttdato"
          value={
            ressurs.sluttdato &&
            format(new Date(ressurs.sluttdato), DATE_FORMATS.no)
          }
        />
      </div>
    </div>
  );
}
