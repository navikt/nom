import { css } from "@emotion/react";
import { Alert, Heading, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import React from "react";
import { type FieldError, useController } from "react-hook-form";

import { getAlleKostnadssteder } from "~/api/react-query-endpoints";
import {
  AsyncSearch,
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { OrgEnhetInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type { KostnadsstedResponse, OrgEnhet } from "~/types/api-models";

export function OrgEnhetIkkeFunnet() {
  return <Alert variant="error">Organisasjonsenheten ble ikke funnet</Alert>;
}

export function IkkeTilgangRedigering() {
  return (
    <Alert variant="error">
      Du har ikke tilgang til å redigere denne organisasjonsenheten
    </Alert>
  );
}

export function OrgEnhetInfoWrapper({
  orgEnhet,
  heading,
}: {
  orgEnhet: OrgEnhet;
  heading: string;
}) {
  return (
    <VStack gap="2">
      {!!heading && (
        <Heading level="2" size="small">
          {heading}
        </Heading>
      )}

      <div
        css={css`
          background-color: #deecef;
          padding: var(--a-spacing-4);
          border-radius: var(--a-border-radius-xlarge);
          display: grid;
        `}
      >
        <OrgEnhetInfo orgEnhet={orgEnhet} />
      </div>
    </VStack>
  );
}

export function FormRessursSearchWrapper({
  name,
  label,
}: {
  name: string;
  label: string;
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <SelectLayoutWrapper error={fieldState.error} htmlFor="Leder" label={label}>
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < 3
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) => {
          field.onChange(selectedOption?.value ?? "");
        }}
        placeholder="Søk etter medarbeider"
      />
    </SelectLayoutWrapper>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  if (searchTerm.length < 3) {
    return [];
  }

  const searchQuery = `query ressurs($searchTerm: String!, $filter: RessursSearchFilter) {
        searchRessurs(term: $searchTerm, filter: $filter) {
          navident
          visningsnavn
        }
      }`;

  const filter = { statusSelection: "AKTIV" };

  const resultRaw = await axios.post("/frackend/nom-api/graphql", {
    operationName: "ressurs",
    query: searchQuery,
    variables: {
      searchTerm: searchTerm,
      filter: filter,
    },
  });

  const resultRessurser = resultRaw.data.data.searchRessurs;

  return resultRessurser.map((ressurs: any) => ({
    label: `${ressurs.visningsnavn} (${ressurs.navident})`,
    value: ressurs.navident,
  }));
}

export function KostnadsstedSelectFormPart({
  name,
  kostnadssteder,
  label = "Kostnadssted",
  description,
  helpText,
  isClearable = false,
}: {
  name: string;
  kostnadssteder?: KostnadsstedResponse[];
  label?: string;
  description?: string;
  helpText?: string;
  isClearable?: boolean;
}) {
  const { field, fieldState } = useController({ name });

  return (
    <KostnadsstedSelect
      description={description}
      error={fieldState.error}
      helpText={helpText}
      isClearable={isClearable}
      kostnadssteder={kostnadssteder}
      label={label}
      onChange={field.onChange}
      value={field.value}
    />
  );
}

export function KostnadsstedSelect({
  kostnadssteder,
  label,
  description,
  onChange,
  error,
  value,
  helpText,
  isClearable = false,
}: {
  kostnadssteder?: KostnadsstedResponse[];
  label: string;
  description?: string;
  onChange: (s: string | null) => void;
  error?: FieldError;
  value?: string;
  helpText?: string;
  isClearable?: boolean;
}) {
  const alleKostnadsstederQuery = useQuery({
    queryKey: getAlleKostnadssteder.queryKey(),
    queryFn: () => getAlleKostnadssteder.queryFn(),
    enabled: !kostnadssteder,
  });

  const alleKostnadssteder = alleKostnadsstederQuery.data ?? [];

  const optionKostnadssteder = kostnadssteder ?? alleKostnadssteder;

  const options = optionKostnadssteder
    .map((kostnadssted) => kostnadsstedOption(kostnadssted))
    .sort((a, b) => (a.value ?? "").localeCompare(b.value ?? "", "no"));

  const currentOption = options.find((option) => option.value === value);

  return (
    <SelectLayoutWrapper
      description={description}
      error={error}
      helpText={helpText}
      htmlFor="Kostnadssted"
      label={label}
    >
      <BasicSelect
        inputId="Kostnadssted"
        isClearable={isClearable}
        onChange={(option) => onChange(option?.value ?? null)}
        options={options}
        placeholder="Søk etter kostnadssted"
        value={currentOption}
      />
    </SelectLayoutWrapper>
  );
}

function kostnadsstedOption(kostnadssted: KostnadsstedResponse) {
  return {
    label: `${kostnadssted.kostnadsstedId}: ${kostnadssted.navn}`,
    value: kostnadssted.kostnadsstedId,
  };
}
