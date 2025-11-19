import { css } from "@emotion/react";
import { HStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import { getKostnadssted, getOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { LabelWithValue } from "~/components/LabelWithValue";
import type { OrgEnhet } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function EnhetKontekstInfo({
  orgEnhet,
  fallbackMessage,
  displayLeder = true,
  displayUni4Niv = false,
  nomIdLink = false,
  displayKostnadssted = false,
}: {
  orgEnhet: OrgEnhet | undefined;
  fallbackMessage?: string;
  displayLeder?: boolean;
  displayUni4Niv?: boolean;
  nomIdLink?: boolean;
  displayKostnadssted?: boolean;
}) {
  const hasAdminAccess = useHasAdminAccess();

  const ledere =
    Array.isArray(orgEnhet?.ledere) && orgEnhet.ledere.length > 0
      ? orgEnhet.ledere.map((leder) => formatVisningsnavn(leder.visningsnavn))
      : [];

  const extraAgressoInfo = useExtraAgressoInfo(orgEnhet?.id);

  const kostnadssted =
    displayKostnadssted && hasAdminAccess
      ? useKostnadsstedInfo(orgEnhet?.id)
      : undefined;

  if (!orgEnhet) {
    return <>{fallbackMessage}</>;
  }

  return (
    <HStack
      css={css`
        font-size: var(--a-font-size-large);
        row-gap: var(--a-spacing-1);
      `}
      gap="4"
    >
      <LabelWithValue
        label="NOM-ID"
        value={orgEnhet.id}
        valueHref={nomIdLink ? `/org/${orgEnhet.id}` : undefined}
      />
      {displayLeder && <LabelWithValue label="Leder" value={ledere[0]} />}
      <LabelWithValue label="Enhet i Remedy" value={orgEnhet.remedyEnhetId} />
      <LabelWithValue label="Unit4-ID" value={extraAgressoInfo?.agressoId} />
      {displayUni4Niv && (
        <LabelWithValue label="Unit4-nivå" value={orgEnhet.orgNiv} />
      )}
      <LabelWithValue
        label="Kostnadssted"
        value={kostnadssted?.kostnadsstedId}
      />
    </HStack>
  );
}

function useExtraAgressoInfo(orgEnhetId: string | undefined) {
  const agressoInfoQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const agressoInfo = agressoInfoQuery.data;

  return agressoInfo
    ? {
        agressoId: agressoInfo.agressoId,
        agressoOrgenhetType: agressoInfo.agressoOrgenhetType,
      }
    : undefined;
}

export function useKostnadsstedInfo(orgEnhetId: string | undefined) {
  const kostnadsstedQuery = useQuery({
    queryKey: getKostnadssted.queryKey(orgEnhetId ?? ""),
    queryFn: () => getKostnadssted.queryFn(orgEnhetId ?? ""),
    enabled: /[a-zA-Z]{2}\d{3}[a-zA-Z]/.test(orgEnhetId ?? ""),
    retry: 1,
  });

  const kostnadssted = kostnadsstedQuery.data;

  return !!kostnadssted
    ? {
        orgenhetId: kostnadssted.orgenhetId,
        kostnadsstedId: kostnadssted.kostnadsstedId,
      }
    : undefined;
}
