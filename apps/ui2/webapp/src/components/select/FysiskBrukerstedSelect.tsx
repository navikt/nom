import { useQuery } from "@tanstack/react-query";
import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";

import type { FysiskBrukersted } from "~/api/react-query-endpoints";
import { getFysiskeBrukersteder } from "~/api/react-query-endpoints";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";

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
          onChange={field.onChange}
          value={field.value}
        />
      )}
    />
  );
}

function FysiskBrukerstedSelect({
  fysiskeBrukersteder,
  description,
  onChange,
  error,
  value,
}: {
  fysiskeBrukersteder: FysiskBrukersted[];
  onChange: (s: string | null) => void;
  error?: FieldError;
  description?: string;
  value?: string;
}) {
  const options = fysiskeBrukersteder.map((fysiskBrukersted) =>
    fysiskBrukerstedOption(fysiskBrukersted),
  );

  const currentOption = options.find((option) => option.value === value);
  return (
    <SelectLayoutWrapper
      description={description}
      error={error}
      helpText="Med fysisk brukersted menes hvor (kontor/plassering) medarbeideren skal
          arbeide."
      htmlFor="Fysisk brukersted"
      label="Fysisk brukersted"
    >
      <BasicSelect
        inputId="Fysisk brukersted"
        onChange={(option) => onChange(option?.value ?? null)}
        options={options}
        value={currentOption ?? null}
      />
    </SelectLayoutWrapper>
  );
}

function fysiskBrukerstedOption(fysiskBrukersted: FysiskBrukersted) {
  return {
    label: `${fysiskBrukersted.brukerstedNr}: ${fysiskBrukersted.brukerstedNavn}`,
    value: fysiskBrukersted.brukerstedNr,
  };
}
