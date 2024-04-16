import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";
import type { SingleValueProps } from "react-select";
import { components } from "react-select";

import { searchRessurser } from "~/api/react-query-endpoints";

import { NavidentNameLookup } from "../NavidentNameLookup";
import { AsyncSearch, SelectLayoutWrapper } from "./CustomSelectComponents";
import type { Option } from "./OptionWithValue";
import { OptionWithValue } from "./OptionWithValue";
const RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;

export function NavidentSelectFormPart({
  label,
  name,
}: {
  label: string;
  name: string;
}) {
  const { control } = useFormContext();
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <NavidentSelect
          error={fieldState.error}
          label={label}
          onChange={field.onChange}
          value={field.value}
        />
      )}
    />
  );
}

export function NavidentSelect({
  onChange,
  label,
  value,
  error,
}: {
  value?: string;
  onChange: (s: string | undefined) => void;
  label: string;
  error?: FieldError;
}) {
  return (
    <SelectLayoutWrapper error={error} htmlFor={label} label={label}>
      <AsyncSearch
        components={{ Option: OptionWithValue, SingleValue: CustomSingleValue }}
        inputId={label}
        loadOptions={searchNomRessursWithQueryClient}
        noOptionsMessage={({ inputValue }) =>
          inputValue.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT
            ? "Skriv minst 3 bokstaver i søkefeltet"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(option) => onChange(option?.value)}
        placeholder="Søk etter ansatt"
        value={value ? { value, label: "" } : null}
      />
    </SelectLayoutWrapper>
  );
}

function CustomSingleValue(properties: SingleValueProps<Option, false>) {
  return (
    <components.SingleValue {...properties}>
      <NavidentNameLookup navident={properties.getValue()[0].value} />
    </components.SingleValue>
  );
}

const searchNomRessursWithQueryClient = async (searchTerm: string) => {
  if (searchTerm.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  const response = await searchRessurser.queryFn(searchTerm);

  return response.map(({ visningsnavn, navident }) => ({
    value: navident,
    label: visningsnavn,
  }));
};
