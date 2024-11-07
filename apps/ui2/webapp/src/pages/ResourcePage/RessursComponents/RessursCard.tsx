import { css } from "@emotion/react";
import { PencilIcon } from "@navikt/aksel-icons";
import { Button, Heading, Tag } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import { format } from "date-fns";
import React from "react";

import { useHasAdminOrWriteAccess } from "~/api/useGetSession";
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
  const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();

  const navigate = useNavigate();

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

      <div
        css={css`
          display: flex;
          justify-content: end;
        `}
      >
        {hasQuit && hasAdminOrWriteAccess && (
          <Button
            icon={<PencilIcon fontSize="1.5rem" />}
            onClick={() =>
              navigate({ to: "/ressurs/create/$step", params: { step: "1" } })
            }
            size="small"
            variant="secondary"
          >
            Gjenopprett
          </Button>
        )}
      </div>
    </div>
  );
}
