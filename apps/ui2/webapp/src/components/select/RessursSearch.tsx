import axios from "axios";
import React, { useState } from "react";
import type { FieldError } from "react-hook-form";

import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { useQuery } from "@tanstack/react-query";
import { getOrgEnhet, getOrgTilknytninger } from "~/api/react-query-endpoints";
import {
  isPeriodActive,
  nomDateIsAfterToday,
  nomDateIsBefore,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { BodyShort, VStack } from "@navikt/ds-react";
import { css } from "@emotion/react";
import { formatDisplayDate } from "~/utils";

export function RessursSearch({
  label = "Medarbeider",
  onChange,
  error,
  visValgtRessursInfo = false,
}: {
  label?: string;
  onChange: (value: string) => void;
  error?: FieldError | string;
  visValgtRessursInfo?: boolean;
}) {
  const [valgtRessursOption, setValgtRessursOption] = useState<
    { label: string; value: string } | undefined
  >(undefined);

  return (
    <VStack gap="4">
      <SelectLayoutWrapper
        error={
          error
            ? typeof error === "string"
              ? ({ message: error } as FieldError)
              : error
            : undefined
        }
        htmlFor="Medarbeider"
        label={label}
      >
        <AsyncSearch
          loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
          noOptionsMessage={({ inputValue }: { inputValue: string }) =>
            inputValue.length < 3
              ? "Du må skrive lenger for å søke"
              : `Fant ingen resultater for "${inputValue}"`
          }
          onChange={(
            selectedOption: { label: string; value: string } | null,
          ) => {
            onChange(selectedOption?.value ?? "");

            setValgtRessursOption(selectedOption ?? undefined);
          }}
          placeholder="Søk etter medarbeider"
        />
      </SelectLayoutWrapper>

      {visValgtRessursInfo && valgtRessursOption && (
        <div
          css={css`
            background-color: #deecef;
            padding: var(--a-spacing-4);
            border-radius: var(--a-border-radius-xlarge);
            display: grid;
          `}
        >
          <RessursInfo ressursOption={valgtRessursOption} />
        </div>
      )}
    </VStack>
  );
}

export function RessursInfo({
  ressursOption,
}: {
  ressursOption: { label: string; value: string };
}) {
  const navident = ressursOption?.value ?? undefined;

  const ressursOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(navident),
    queryFn: () => getOrgTilknytninger.queryFn(navident),
    enabled: !!navident,
  });

  const ressursOrgtilknytninger = ressursOrgtilknytningerQuery.data;

  const aktiveDolOTer = ressursOrgtilknytninger?.filter(
    (tilknytning) =>
      isPeriodActive(tilknytning.gyldigFom, tilknytning.gyldigTom) &&
      tilknytning.erDagligOppfolging,
  );

  const aktivEllerFremtidigDolOT =
    aktiveDolOTer?.[0] ??
    ressursOrgtilknytninger
      ?.filter(
        (tilknytning) =>
          tilknytning.erDagligOppfolging &&
          nomDateIsAfterToday(tilknytning.gyldigFom),
      )
      .sort((a, b) => (nomDateIsBefore(a.gyldigFom, b.gyldigFom) ? -1 : 1))[0];

  const OTErFremtidig =
    !!aktivEllerFremtidigDolOT &&
    nomDateIsAfterToday(aktivEllerFremtidigDolOT.gyldigFom);

  const relevantOrgEnhetId = aktivEllerFremtidigDolOT?.orgEnhetId ?? "";

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(relevantOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(relevantOrgEnhetId),
    enabled: !!relevantOrgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  return (
    <VStack gap="2">
      <BodyShort
        css={css`
          font-size: var(--a-font-size-xlarge);
        `}
      >
        <b>{ressursOption.label}</b>
      </BodyShort>

      <div>
        Ansatt i: <b>{orgEnhet?.navn ?? "Ingen enhet funnet"}</b>
        {OTErFremtidig && (
          <> (f.o.m. {formatDisplayDate(aktivEllerFremtidigDolOT.gyldigFom)})</>
        )}
      </div>
    </VStack>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  if (searchTerm.length < 3) {
    return [];
  }

  const searchQuery = `
    query ressurs($searchTerm: String!, $filter: RessursSearchFilter) {
          searchRessurs(term: $searchTerm, filter: $filter) {
            navident
            visningsnavn
          }
        }
  `;

  const filter = { statusSelection: "AKTIV" };

  const resultRaw = await axios.post("/frackend/nom-api/graphql", {
    operationName: "ressurs",
    query: searchQuery,
    variables: {
      searchTerm: searchTerm,
      filter: filter,
    },
  });

  const resultRessurser = resultRaw.data.data.searchRessurs;

  return resultRessurser.map((ressurs: any) => ({
    label: `${ressurs.visningsnavn} (${ressurs.navident})`,
    value: ressurs.navident,
  }));
}
