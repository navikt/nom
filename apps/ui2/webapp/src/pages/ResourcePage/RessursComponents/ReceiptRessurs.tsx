import { css } from "@emotion/react";
import { Heading, Link } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import capitalize from "lodash/capitalize";
import React from "react";

import { getOrgEnhet, getOrgTilknytninger } from "~/api/react-query-endpoints";
import { LabelWithValue } from "~/components/LabelWithValue";
import { UserImage } from "~/components/UserImage";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { blueTopBorder } from "~/resources/NomStyles";
import type { Ressurs } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function ReceiptRessurs({ ressurs }: { ressurs: Ressurs }) {
  // Presently there will only be one orgTilknytning after creation.
  const firstOrgtilknytningOrgEnhetId = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
    select: (orgtilknytninger) => orgtilknytninger[0]?.orgEnhetId,
  }).data;

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(firstOrgtilknytningOrgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(firstOrgtilknytningOrgEnhetId as string),
    enabled: !!firstOrgtilknytningOrgEnhetId,
  });

  const fysiskBrukersted = useFysiskBrukersted(ressurs.primaryFysiskBrukersted);

  const orgEnhet = orgEnhetQuery.data;
  const leder = orgEnhet?.ledere[0];

  return (
    <div
      css={[
        blueTopBorder,
        css`
          padding: var(--a-spacing-8);
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          flex-wrap: wrap-reverse;
          gap: var(--a-spacing-4);
        `,
      ]}
    >
      <div>
        <Heading level="1" size="large" spacing>
          {formatVisningsnavn(ressurs.visningsnavn)}
        </Heading>
        <div
          css={css`
            display: grid;
            gap: var(--a-spacing-4);
            grid-template-columns: 1fr 1fr;
          `}
        >
          <LabelWithValue
            label="Sektor"
            value={SektorNameMapping[ressurs.sektor]}
            wrapped
          />
          <LabelWithValue label="NAV-ident" value={ressurs.navident} wrapped />
          <LabelWithValue label="Org.enhet" value={orgEnhet?.navn} wrapped />
          <LabelWithValue
            label="Enhetsnummer"
            value={orgEnhet?.remedyEnhetId}
            wrapped
          />
          {leder && (
            <span>
              Leder:{" "}
              <ReactRouterLink
                params={{ id: leder.navident }}
                search={{}}
                to="/ressurs/$id"
              >
                <Link as="span">{formatVisningsnavn(leder.visningsnavn)}</Link>
              </ReactRouterLink>
            </span>
          )}
          <LabelWithValue
            label="Fysisk brukersted"
            value={
              fysiskBrukersted
                ? capitalize(fysiskBrukersted.brukerstedNavn)
                : "-"
            }
            wrapped
          />
        </div>
      </div>
      <UserImage
        includeInstructionsWhenNotFound
        navident={ressurs.navident}
        navn={formatVisningsnavn(ressurs.visningsnavn)}
        size="126px"
      />
    </div>
  );
}
