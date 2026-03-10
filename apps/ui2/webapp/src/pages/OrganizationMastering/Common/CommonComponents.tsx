import { css } from "@emotion/react";
import { BodyLong } from "@navikt/ds-react";
import axios from "axios";
import React from "react";
import { useController } from "react-hook-form";

import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";

export function OrgEnhetIkkeFunnet() {
  return (
    <BodyLong
      css={css`
        font-size: var(--a-font-size-xlarge);
      `}
    >
      Organisasjonsenheten ble ikke funnet
    </BodyLong>
  );
}

export function IkkeTilgangRedigering() {
  return (
    <BodyLong
      css={css`
        font-size: var(--a-font-size-xlarge);
      `}
    >
      Du har ikke tilgang til å redigere denne organisasjonsenheten
    </BodyLong>
  );
}

export function FormRessursSearchWrapper({ name }: { name: string }) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <SelectLayoutWrapper
      error={fieldState.error}
      htmlFor="Leder"
      label="Ny leder på organisasjonsenhet"
    >
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < 3
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) => {
          field.onChange(selectedOption?.value ?? "");
        }}
        placeholder="Søk etter medarbeider"
      />
    </SelectLayoutWrapper>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  if (searchTerm.length < 3) {
    return [];
  }

  const searchQuery = `query ressurs($searchTerm: String!, $filter: RessursSearchFilter) {
        searchRessurs(term: $searchTerm, filter: $filter) {
          navident
          visningsnavn
        }
      }`;

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
