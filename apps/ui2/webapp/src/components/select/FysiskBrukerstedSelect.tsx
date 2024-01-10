import { HelpText } from "@navikt/ds-react";
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
  defaultValue,
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
          defaultValue={defaultValue}
          description={description}
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
  defaultValue,
}: {
  fysiskeBrukersteder: FysiskBrukersted[];
  onChange: (s: string | undefined) => void;
  description?: string;
  error?: FieldError;
  value?: string;
  defaultValue?: FysiskBrukersted;
}) {
  const options = fysiskeBrukersteder.map((fysiskBrukersted) =>
    fysiskBrukerstedOption(fysiskBrukersted),
  );

  const defaultOption = defaultValue
    ? fysiskBrukerstedOption(defaultValue)
    : null;

  const currentOption = options.find((option) => option.value === value);

  return (
    <SelectLayoutWrapper
      description={description}
      error={error}
      htmlFor="Fysisk brukersted"
      label={
        <>
          Fysisk brukersted
          <HelpText>
            Med fysisk brukersted menes hvor(kontor/lokasjon) medarbeideren skal
            arbeide.
          </HelpText>
        </>
      }
    >
      <BasicSelect
        inputId="Fysisk brukersted"
        onChange={(option) => onChange(option?.value)}
        options={options}
        value={currentOption ?? defaultOption}
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
