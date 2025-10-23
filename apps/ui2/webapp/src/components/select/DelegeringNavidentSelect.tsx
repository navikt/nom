import { UNSAFE_Combobox } from "@navikt/ds-react";
import { useState } from "react";
import { Controller } from "react-hook-form";

import type { MinimalRessurs } from "~/types/api-models";

export function DelegeringNavidentSelectFormPart({
  name,
  delegerbareRessurser,
}: {
  name: string;
  delegerbareRessurser: MinimalRessurs[];
}) {
  const [searchTerm, setSearchTerm] = useState("");

  const alleOptions = delegerbareRessurser.map((option) => {
    return {
      value: option.navident,
      label: `${option.visningsnavn} (${option.navident})`,
    };
  });

  const options = alleOptions.filter((option) =>
    option.label.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  return (
    <Controller
      name={name}
      render={({ field, fieldState }) => {
        const selectedOption = alleOptions.find(
          (option) => option?.value === field.value,
        );

        return (
          <UNSAFE_Combobox
            error={fieldState.error?.message?.toString()}
            filteredOptions={options ?? []}
            label="Søk opp medarbeider"
            onChange={(value) => setSearchTerm(value ?? "")}
            onToggleSelected={(value, isSelected) => {
              field.onChange(isSelected ? value : undefined);
            }}
            options={options ?? []}
            selectedOptions={selectedOption ? [selectedOption] : []}
            size="small"
          />
        );
      }}
    />
  );
}
