import { css } from "@emotion/react";
import { Alert, BodyShort, Heading, Select, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import React, { useState } from "react";
import {
  type FieldError,
  useController,
  useFormContext,
} from "react-hook-form";

import {
  getAlleKostnadssteder,
  getOrgEnhet,
  getOrgTilknytninger,
} from "~/api/react-query-endpoints";
import {
  AsyncSearch,
  BasicSelect,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { OrgEnhetInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import {
  type KostnadsstedResponse,
  type OrgEnhet,
  OrgEnhetsType,
} from "~/types/api-models";
import {
  isPeriodActive,
  nomDateIsAfterToday,
  nomDateIsBefore,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { formatDisplayDate } from "~/utils";

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
  valgtRessursInfo = false,
}: {
  name: string;
  label: string;
  valgtRessursInfo?: boolean;
}) {
  const { field, fieldState } = useController({
    name,
  });

  const [valgtRessursOption, setValgtRessursOption] = useState<
    { label: string; value: string } | undefined
  >(undefined);

  return (
    <VStack gap="4">
      <SelectLayoutWrapper
        error={fieldState.error}
        htmlFor="Leder"
        label={label}
      >
        <AsyncSearch
          loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
          noOptionsMessage={({ inputValue }: { inputValue: string }) =>
            inputValue.length < 3
              ? "Du må skrive minst 3 tegn for å søke"
              : `Fant ingen resultater for "${inputValue}"`
          }
          onChange={(
            selectedOption: { label: string; value: string } | null,
          ) => {
            field.onChange(selectedOption?.value ?? "");

            setValgtRessursOption(selectedOption ?? undefined);
          }}
          placeholder="Søk etter medarbeider"
        />
      </SelectLayoutWrapper>

      {valgtRessursInfo && valgtRessursOption && (
        <div
          css={css`
            background-color: #deecef;
            padding: var(--a-spacing-4);
            border-radius: var(--a-border-radius-xlarge);
            display: grid;
          `}
        >
          <RessursInfo ressursOption={valgtRessursOption} />
        </div>
      )}
    </VStack>
  );
}

export function RessursInfo({
  ressursOption,
}: {
  ressursOption: { label: string; value: string };
}) {
  const navident = ressursOption?.value ?? undefined;

  const ressursOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(navident),
    queryFn: () => getOrgTilknytninger.queryFn(navident),
    enabled: !!navident,
  });

  const ressursOrgtilknytninger = ressursOrgtilknytningerQuery.data;

  const aktiveDolOTer = ressursOrgtilknytninger?.filter(
    (tilknytning) =>
      isPeriodActive(tilknytning.gyldigFom, tilknytning.gyldigTom) &&
      tilknytning.erDagligOppfolging,
  );

  const aktivEllerFremtidigDolOT =
    aktiveDolOTer?.[0] ??
    ressursOrgtilknytninger
      ?.filter(
        (tilknytning) =>
          tilknytning.erDagligOppfolging &&
          nomDateIsAfterToday(tilknytning.gyldigFom),
      )
      .sort((a, b) => (nomDateIsBefore(a.gyldigFom, b.gyldigFom) ? -1 : 1))[0];

  const OTErFremtidig =
    !!aktivEllerFremtidigDolOT &&
    nomDateIsAfterToday(aktivEllerFremtidigDolOT.gyldigFom);

  const relevantOrgEnhetId = aktivEllerFremtidigDolOT?.orgEnhetId ?? "";

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(relevantOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(relevantOrgEnhetId),
    enabled: !!relevantOrgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  return (
    <VStack gap="2">
      <BodyShort
        css={css`
          font-size: var(--a-font-size-xlarge);
        `}
      >
        <b>{ressursOption.label}</b>
      </BodyShort>

      <div>
        Ansatt i: <b>{orgEnhet?.navn ?? "Ingen enhet funnet"}</b>
        {OTErFremtidig && (
          <> (f.o.m. {formatDisplayDate(aktivEllerFremtidigDolOT.gyldigFom)})</>
        )}
      </div>
    </VStack>
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
