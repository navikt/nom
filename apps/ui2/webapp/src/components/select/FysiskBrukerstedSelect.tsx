import { css } from "@emotion/react";
import { HelpText, HStack, Select } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { FieldError, RefCallBack } from "react-hook-form";
import { useController } from "react-hook-form";
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
  helpText,
}: {
  name?: string;
  description?: string;
  helpText?: string;
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
          helpText={helpText}
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

export function FysiskBrukerstedSelectFormPartNew({
  name,
  label,
  description,
  helpText,
  size,
  fysiskeBrukersteder,
}: {
  name: string;
  label?: string;
  description?: string;
  helpText?: string;
  size?: "small" | "medium";
  fysiskeBrukersteder?: FysiskBrukersted[];
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <FysiskBrukerstedSelectNew
      description={description}
      error={fieldState.error?.message}
      fysiskeBrukersteder={fysiskeBrukersteder}
      helpText={helpText}
      label={label}
      onChange={field.onChange}
      ref={field.ref}
      size={size}
      value={field.value}
    />
  );
}

export function FysiskBrukerstedSelectNew({
  label = "Fysisk brukersted",
  description,
  helpText,
  size = "medium",
  fysiskeBrukersteder,
  onChange,
  error,
  value,
  ref,
}: {
  label?: string;
  description?: string;
  helpText?: string;
  fysiskeBrukersteder?: FysiskBrukersted[];
  onChange: (s: string) => void;
  error?: string;
  value?: string;
  size?: "small" | "medium";
  ref?: RefCallBack;
}) {
  return (
    <Select
      description={description}
      error={error}
      label={
        <SelectLabelWithHelpText
          helpText={helpText}
          label={label}
          size={size}
        />
      }
      onChange={(option) => onChange(option.target.value)}
      ref={ref}
      size={size}
      value={value}
    >
      <option value="" />
      {fysiskeBrukersteder?.map((fysiskBrukersted) => (
        <option
          key={fysiskBrukersted.brukerstedNr}
          value={fysiskBrukersted.brukerstedNr}
        >{`${fysiskBrukersted.brukerstedNr}: ${capitalizeNameFysiskBrukersted(fysiskBrukersted.brukerstedNavn)}`}</option>
      ))}
    </Select>
  );
}

function SelectLabelWithHelpText({
  label,
  helpText,
  size,
}: {
  label: string;
  helpText?: string;
  size: string;
}) {
  return (
    <HStack gap={size === "small" ? "1" : "2"}>
      <div>{label}</div>
      {helpText && (
        <HelpText
          css={css`
            .navds-help-text__icon {
              height: ${size === "small" ? "16px" : undefined};
            }
          `}
        >
          {helpText}
        </HelpText>
      )}
    </HStack>
  );
}

function fysiskBrukerstedOption(fysiskBrukersted: FysiskBrukersted) {
  return {
    label: `${fysiskBrukersted.brukerstedNr}: ${capitalizeNameFysiskBrukersted(fysiskBrukersted.brukerstedNavn)}`,
    value: fysiskBrukersted.brukerstedNr,
  };
}
