import { css } from "@emotion/react";
import { Heading, Label } from "@navikt/ds-react";
import React from "react";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/components/CreateResourceForm";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";

export function ReceiptRessurs({ ressurs }: { ressurs: Ressurs }) {
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
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          flex-wrap: wrap-reverse;
          gap: var(--a-spacing-4);
        `}
      >
        <div>
          <Heading level="1" size="large" spacing>
            {ressurs.visningsnavn} ({ressurs.navident})
          </Heading>
          <div>
            <span>Sektor: </span>
            <Label as="span">
              {ressurs.sektor
                .map((sektor) => SektorNameMapping[sektor])
                .join(", ")}
            </Label>
          </div>
        </div>
        <UserImage
          navident={ressurs.navident}
          navn={ressurs.visningsnavn ?? ""}
          size="126px"
        />
      </div>
    </div>
  );
}
