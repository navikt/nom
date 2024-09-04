import { UNSAFE_Combobox } from "@navikt/ds-react";
import type { ComboboxOption } from "@navikt/ds-react/esm/form/combobox/types";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Controller } from "react-hook-form";

import { CACHE_FOR } from "~/api/react-query-endpoints";
import { commonSearch } from "~/components/select/Searchbar";

const RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;

export function NavidentSelectFormPart({
  label,
  name,
}: {
  label: string;
  name: string;
}) {
  const queryClient = useQueryClient();

  const [searchTerm, setSearchTerm] = useState("");

  const optionsQuery = useQuery({
    queryKey: ["RESSURS_SØK", searchTerm],
    queryFn: () => searchNomRessurser(searchTerm),
    gcTime: CACHE_FOR.aMinute,
    staleTime: CACHE_FOR.aMinute,
    enabled: searchTerm.length >= 3,
  });

  const alleOptions = queryClient
    .getQueriesData<ComboboxOption[]>({
      queryKey: ["RESSURS_SØK"],
    })
    .flatMap((entry) => entry[1]);

  return (
    <Controller
      name={name}
      render={({ field, fieldState }) => {
        const selectedOption = alleOptions?.find(
          (option) => option?.value === field.value,
        );
        return (
          <UNSAFE_Combobox
            error={fieldState.error?.message?.toString()}
            filteredOptions={optionsQuery.data ?? []}
            label={label}
            onChange={(value) => setSearchTerm(value ?? "")}
            onToggleSelected={(value, isSelected) => {
              field.onChange(isSelected ? value : undefined);
            }}
            options={optionsQuery.data ?? []}
            selectedOptions={selectedOption ? [selectedOption] : []}
            size="small"
          />
        );
      }}
    />
  );
}

const searchNomRessurser = async (searchTerm: string) => {
  if (searchTerm.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  const response = await commonSearch(searchTerm);

  return response.flatMap((r) => {
    switch (r.__typename) {
      case "Ressurs": {
        return [
          {
            value: r.navident,
            label: r.visningsnavn,
          },
        ];
      }
      case "OrgEnhet": {
        return [];
      }
    }
  });
};
