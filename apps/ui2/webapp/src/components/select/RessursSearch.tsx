import axios from "axios";
import React from "react";
import type { FieldError } from "react-hook-form";

import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";

export function RessursSearch({
  label = "Medarbeider",
  onChange,
  error,
}: {
  label?: string;
  onChange: (value: string) => void;
  error?: FieldError | string;
}) {
  return (
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
        onChange={(selectedOption: { label: string; value: string } | null) => {
          onChange(selectedOption?.value ?? "");
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
