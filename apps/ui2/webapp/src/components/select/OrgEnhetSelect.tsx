import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";
import { createFilter } from "react-select";

import type { OrgEnhet } from "~/api/generated/graphql_generated";

import { AsyncSearch, SelectLayoutWrapper } from "./CustomSelectComponents";
import { OptionWithValue } from "./OptionWithValue";

export function OrgEnhetSelectFormPart({
  orgEnheter,
}: {
  orgEnheter: OrgEnhet[];
}) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name="orgEnhetId"
      render={({ field, fieldState }) => (
        <OrgEnhetSelect
          error={fieldState.error}
          label="Org.enhet"
          onChange={field.onChange}
          orgEnheter={orgEnheter}
          value={field.value}
        />
      )}
    />
  );
}

function OrgEnhetSelect({
  onChange,
  orgEnheter,
  label = "Status",
  value,
  error,
}: {
  orgEnheter: OrgEnhet[];
  onChange: (s: string | undefined) => void;
  value?: string;
  label?: string;
  error?: FieldError;
}) {
  const options = orgEnheter.map((orgEnhet) => ({
    value: orgEnhet.id,
    label: orgEnhet.navn,
  }));
  const currentValue = options.find((option) => option.value === value);

  return (
    <SelectLayoutWrapper error={error} label={label}>
      {/*
        Even though this is not an Async search, there is a good reason why this AsyncSearch> is used instead of BaseSelect.
        Presently there are over 1000 orgenheter. These are all fetched at once and input to this component simply filters that list.
        The default filter function from react-select is pretty neat, however, the base implementation of Select calls that filter function waaay too many times!
        It is called for every mouse and keyboard event.
        That means that every mouseEnter, mouseExit, mouseMove and so on, will trigger the filter function for 1000+ elements, every, single, time... which is quite excessive.
        By instead using the AsyncSearch we can get better control over when the filter function is called to dramatically improve performance.
      */}
      <AsyncSearch
        components={{ Option: OptionWithValue }}
        loadOptions={search(options)}
        noOptionsMessage={({ inputValue }) =>
          inputValue.length === 0
            ? "Skriv minst 1 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(option) => onChange(option?.value)}
        placeholder="Søk etter enhetsnavn eller ID"
        value={currentValue ?? null}
      />
    </SelectLayoutWrapper>
  );
}

const search =
  (options: { value: string; label: string }[]) =>
  async (searchTerm: string) => {
    const filterFunction = createFilter();

    // For some reason the filterFunction requires a data property on every option.
    return options.filter((option) =>
      filterFunction({ ...option, data: {} }, searchTerm)
    );
  };
