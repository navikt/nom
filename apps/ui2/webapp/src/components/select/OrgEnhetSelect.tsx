import { css } from "@emotion/react";
import { Label } from "@navikt/ds-react";
import { forwardRef } from "react";
import type { FieldError } from "react-hook-form";
import { Controller, useFormContext } from "react-hook-form";
import type { OptionProps } from "react-select";
import { components, createFilter } from "react-select";

import { useInDevelopment } from "~/customUtils/environmentUtils";
import { gamleOrgEnheter } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { nomDateIsAfterToday } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgEnhetMedLedernavn } from "~/types/api-models";

import { AsyncSearch, SelectLayoutWrapper } from "./CustomSelectComponents";

export function OrgEnhetSelectFormPart({
  name,
  orgEnheter,
}: {
  name: string;
  orgEnheter: OrgEnhetMedLedernavn[];
}) {
  const { control } = useFormContext();

  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <OrgEnhetSelect
          error={fieldState.error}
          helpText="Med organisasjonsenhet menes hvilken enhet medarbeideren skal
              tilhøre og dermed hvilken leder som er ansvarlig for vedkommende."
          label="Organisasjonsenhet"
          onChange={(oe) => field.onChange(oe?.id)}
          orgEnheter={orgEnheter}
          ref={field.ref}
          value={field.value}
        />
      )}
    />
  );
}

export const OrgEnhetSelect = forwardRef(
  (
    {
      onChange,
      orgEnheter,
      label,
      description,
      value,
      error,
      helpText,
    }: {
      orgEnheter: OrgEnhetMedLedernavn[];
      onChange: (orgEnhet: OrgEnhetMedLedernavn | undefined) => void;
      value?: string;
      description?: string;
      label: string;
      error?: FieldError;
      helpText?: string;
    },
    reference,
  ) => {
    const inDevelopment = useInDevelopment();

    const options = orgEnheter.map((orgEnhet) => {
      const ikkeAktiv =
        nomDateIsAfterToday(orgEnhet.gyldigFom) ||
        (!inDevelopment && gamleOrgEnheter.includes(orgEnhet.id));

      return {
        value: orgEnhet.id,
        label: `${orgEnhet.navn}${ikkeAktiv ? " (ikke aktiv)" : ""}`,
        id: orgEnhet.id,
      };
    });

    const currentValue = options.find((option) => option.value === value);

    return (
      <SelectLayoutWrapper
        description={description}
        error={error}
        helpText={helpText}
        htmlFor={label}
        label={label}
      >
        {/*
        Even though this is not an Async search, there is a good reason why this AsyncSearch> is used instead of BaseSelect.
        Presently there are over 1000 orgenheter. These are all fetched at once and input to this component simply filters that list.
        The default filter function from react-select is pretty neat, however, the base implementation of Select calls that filter function waaay too many times!
        It is called for every mouse and keyboard event.
        That means that every mouseEnter, mouseExit, mouseMove and so on, will trigger the filter function for 1000+ elements, every, single, time... which is quite excessive.
        By instead using the AsyncSearch we can get better control over when the filter function is called to dramatically improve performance.
      */}
        <AsyncSearch
          components={{ Option: OptionWithIds }}
          css={[
            error &&
              css`
                border-color: var(--a-border-danger);
                box-shadow: 0 0 0 1px var(--a-border-danger);
              `,
          ]}
          inputId={label}
          loadOptions={search(options)}
          noOptionsMessage={({ inputValue }) =>
            inputValue.length === 0
              ? "Skriv minst ett tegn for å søke"
              : `Fant ingen resultater for "${inputValue}"`
          }
          onChange={(option) =>
            onChange(orgEnheter.find((it) => it.id === option?.value))
          }
          placeholder="Søk etter navn på organisasjonsenhet"
          ref={reference}
          value={currentValue ?? null}
        />
      </SelectLayoutWrapper>
    );
  },
);

const search =
  (options: { value: string; label: string; id: string }[]) =>
  async (searchTerm: string) => {
    const newSearchTerm = searchTerm.toLowerCase().replace("nav", "");
    const filterFunction = createFilter();

    // For some reason the filterFunction requires a data property on every option.
    return options.filter((option) =>
      filterFunction({ ...option, data: {} }, newSearchTerm),
    );
  };

type Option = { value: string; label: string; id: string };

function OptionWithIds(properties: OptionProps<Option, false>) {
  return (
    <components.Option {...properties}>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
          gap: var(--a-spacing-4);
        `}
      >
        <span>{properties.data.label}</span>
        <Label size="small">{properties.data.value}</Label>
      </div>
    </components.Option>
  );
}
