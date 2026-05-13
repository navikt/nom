import { useQuery } from "@tanstack/react-query";
import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";

import type { FysiskBrukersted } from "~/api/react-query-endpoints";
import { getFysiskeBrukersteder } from "~/api/react-query-endpoints";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { capitalizeNameFysiskBrukersted } from "~/utils";

export function FysiskBrukerstedSelectFormPart({
  name = "fysiskBrukerstedNr",
  description,
}: {
  name?: string;
  description?: string;
  defaultValue?: FysiskBrukersted;
}) {
  const { control } = useFormContext();
  const fysiskeBrukerstederQuery = useQuery(getFysiskeBrukersteder);
  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data || [];

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <FysiskBrukerstedSelect
          description={description}
          error={fieldState.error}
          fysiskeBrukersteder={fysiskeBrukersteder}
          helpText="Med fysisk brukersted menes hvor (kontor/plassering) medarbeideren skal
          arbeide."
          onChange={field.onChange}
          value={field.value}
        />
      )}
    />
  );
}

export function FysiskBrukerstedSelect({
  fysiskeBrukersteder,
  description,
  onChange,
  error,
  value,
  helpText,
  isClearable = false,
}: {
  fysiskeBrukersteder: FysiskBrukersted[];
  onChange: (s: string | null) => void;
  error?: FieldError;
  description?: string;
  value?: string;
  helpText?: string;
  isClearable?: boolean;
}) {
  const options = fysiskeBrukersteder.map((fysiskBrukersted) =>
    fysiskBrukerstedOption(fysiskBrukersted),
  );

  const currentOption = options.find((option) => option.value === value);
  return (
    <SelectLayoutWrapper
      description={description}
      error={error}
      helpText={helpText}
      htmlFor="Fysisk brukersted"
      label="Fysisk brukersted"
    >
      <BasicSelect
        inputId="Fysisk brukersted"
        isClearable={isClearable}
        onChange={(option) => onChange(option?.value ?? null)}
        options={options}
        placeholder="Søk etter fysisk brukersted"
        value={currentOption ?? null}
      />
    </SelectLayoutWrapper>
  );
}

function fysiskBrukerstedOption(fysiskBrukersted: FysiskBrukersted) {
  return {
    label: `${fysiskBrukersted.brukerstedNr}: ${capitalizeNameFysiskBrukersted(fysiskBrukersted.brukerstedNavn)}`,
    value: fysiskBrukersted.brukerstedNr,
  };
}
