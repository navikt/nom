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
  sluttetOverEtArSiden = false,
}: {
  ressurs: Ressurs;
  sluttetOverEtArSiden?: boolean;
}) {
  const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();

  const navigate = useNavigate();

  return (
    <div
      css={[
        css`
          padding: var(--ax-space-32);
        `,
        blueTopBorder,
      ]}
    >
      <div
        css={css`
          display: flex;
          align-items: center;
          gap: var(--ax-space-16);
          margin-bottom: var(--ax-space-16);
        `}
      >
        <Heading size="large">
          {formatVisningsnavn(ressurs.visningsnavn)}
        </Heading>
        {sluttetOverEtArSiden && <Tag variant="warning">Sluttet</Tag>}
      </div>

      <div
        css={css`
          display: grid;
          grid-template-columns: max-content 1fr;
          gap: var(--ax-space-12);
        `}
      >
        <LabelWithValue label="Nav-ident" split value={ressurs.navident} />
        <LabelWithValue
          label="Sektor"
          split
          value={SektorNameMapping[ressurs.sektor]}
        />
        <LabelWithValue
          label="Startdato"
          split
          value={
            ressurs.startdato &&
            format(new Date(ressurs.startdato), DATE_FORMATS.no)
          }
        />
        <LabelWithValue
          label="Sluttdato"
          split
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
        {sluttetOverEtArSiden && hasAdminOrWriteAccess && (
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
