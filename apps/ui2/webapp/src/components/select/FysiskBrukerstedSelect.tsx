import { useQuery } from "@tanstack/react-query";
import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";

import type { FysiskBrukersted } from "~/api/react-query-endpoints";
import { getFysiskeBrukersteder } from "~/api/react-query-endpoints";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";

export function FysiskBrukerstedSelectFormPart() {
  const { control } = useFormContext();
  const fysiskeBrukerstederQuery = useQuery(getFysiskeBrukersteder);
  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data || [];

  return (
    <Controller
      control={control}
      name="fysiskBrukerstedNr"
      render={({ field, fieldState }) => (
        <FysiskBrukerstedSelect
          description="Med fysisk brukersted menes hvor(kontor/lokasjon) medarbeideren skal arbeide. "
          error={fieldState.error}
          fysiskeBrukersteder={fysiskeBrukersteder}
          onChange={field.onChange}
          value={field.value}
        />
      )}
    ></Controller>
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
  onChange: (s: string | undefined) => void;
  description: string;

  error?: FieldError;
  value?: string;
}) {
  const options = fysiskeBrukersteder.map((fysiskBrukersted) => ({
    label: `${fysiskBrukersted.brukerstedNr}: ${fysiskBrukersted.brukerstedNavn}`,
    value: fysiskBrukersted.brukerstedNr,
  }));

  const currentValue = options.find((option) => option.value === value);

  return (
    <SelectLayoutWrapper
      description={description}
      error={error}
      htmlFor="Fysisk brukersted"
      label="Fysisk brukersted"
    >
      <BasicSelect
        inputId="Fysisk brukersted"
        onChange={(option) => onChange(option?.value)}
        options={options}
        value={currentValue ?? null}
      />
    </SelectLayoutWrapper>
  );
}
