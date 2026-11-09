import { css } from "@emotion/react";
import { Alert, BodyShort, Heading, Select, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React from "react";
import {
  type FieldError,
  useController,
  useFormContext,
} from "react-hook-form";

import { getAlleKostnadssteder } from "~/api/react-query-endpoints";
import { EnhetKontekstInfo } from "~/components/EnhetKontekstInfo";
import {
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import {
  NomNivaaNameMapping,
  OrgEnhetsTypeNameMapping,
} from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import {
  type KostnadsstedResponse,
  NomNivaa,
  type OrgEnhet,
  OrgEnhetsType,
} from "~/types/api-models";

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

export function OrgEnhetInfo({
  orgEnhet,
  heading,
  internalHeading,
  displayKostnadssted,
}: {
  orgEnhet: OrgEnhet;
  heading?: string;
  internalHeading?: string;
  displayKostnadssted?: boolean;
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
          padding: var(--a-spacing-4) var(--a-spacing-6);
          border-radius: var(--a-border-radius-xlarge);
          display: grid;
        `}
      >
        <VStack gap="2">
          {!!internalHeading && (
            <Heading level="1" size="xsmall">
              {internalHeading}
            </Heading>
          )}

          <BodyShort
            css={css`
              font-size: var(--a-font-size-xlarge);
            `}
          >
            <b>{orgEnhet.navn}</b>
          </BodyShort>

          <EnhetKontekstInfo
            orgEnhet={orgEnhet}
            displayKostnadssted={displayKostnadssted}
          />
        </VStack>
      </div>
    </VStack>
  );
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

export function OrgEnhetTypeSelect({
  name,
  emptyOption = true,
  fullWidth = false,
}: {
  name: string;
  emptyOption?: boolean;
  fullWidth?: boolean;
}) {
  const methods = useFormContext();

  const { fieldState } = useController({ name });

  return (
    <Select
      css={css`
        width: ${fullWidth ? "100%" : "fit-content"};
      `}
      {...methods.register(name)}
      error={fieldState.error?.message}
      label="Organisasjonsenhetstype"
    >
      {emptyOption && <option value="" />}

      {Object.values(OrgEnhetsType)
        .filter((orgEnhetsType) => orgEnhetsType !== OrgEnhetsType.DIR)
        .map((orgEnhetsType) => (
          <option key={orgEnhetsType} value={orgEnhetsType}>
            {OrgEnhetsTypeNameMapping[orgEnhetsType]}
          </option>
        ))}
    </Select>
  );
}

export function NomNivaaSelect({
  name,
  emptyOption = true,
  fullWidth = false,
}: {
  name: string;
  emptyOption?: boolean;
  fullWidth?: boolean;
}) {
  const methods = useFormContext();

  const { fieldState } = useController({ name });

  return (
    <Select
      css={css`
        width: ${fullWidth ? "100%" : "fit-content"};
      `}
      {...methods.register(name)}
      error={fieldState.error?.message}
      label="NOM-nivå"
    >
      {emptyOption && <option />}

      {Object.values(NomNivaa).map((nomNivaa) => (
        <option key={nomNivaa} value={nomNivaa}>
          {NomNivaaNameMapping[nomNivaa]}
        </option>
      ))}
    </Select>
  );
}
