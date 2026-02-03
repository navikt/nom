import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  DatePicker,
  Loader,
  Select,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import axios from "axios";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type { OrgEnhet } from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";

export const KorrigerOrgEnhet = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  const { id } = useParams({ from: "/korriger-organisasjonsenhet/$id" });

  return (
    <>
      <PageBanner text="Korrigér organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          <KorrigeringOrgEnhet orgEnhetId={id} />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhet({ orgEnhetId }: { orgEnhetId: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  if (!orgEnhet) {
    return;
  }

  return (
    <>
      <OrgEnhetInfoWrapper
        heading="Informasjon om organisasjonsenhet"
        orgEnhetId={orgEnhetId}
      />

      <KorrigeringOrgEnhetForm orgEnhet={orgEnhet} />
    </>
  );
}

function KorrigeringOrgEnhetForm({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const methods = useForm({
    defaultValues: {
      orgEnhetNavn: orgEnhet.navn,
      gyldigFom: orgEnhet.gyldigFom,
      lederRessursId: orgEnhet.leder.navident,
      firesiffer: orgEnhet.remedyEnhetId ?? "",
      orgEnhetType: orgEnhet.orgEnhetsType,
    },
    resolver: yupResolver(validationSchema),
  });

  const existingOrgEnhetLederOption = {
    label: `${orgEnhet.leder.visningsnavn} (${orgEnhet.leder.navident})`,
    value: orgEnhet.leder.navident,
  };

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {};

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="4">
          <TextField
            {...methods.register("orgEnhetNavn")}
            description="Enheten kan ikke ha likt navn som en annen enhet"
            error={methods.formState.errors.orgEnhetNavn?.message}
            label="Navn på organisasjonsenhet"
          />

          <FormRessursSearchWrapper
            name="lederRessursId"
            orgEnhetLederOption={existingOrgEnhetLederOption}
          />

          <FormDateWrapper name="gyldigFom" />

          <Select
            css={css`
              width: fit-content;
            `}
            {...methods.register("orgEnhetType")}
            error={methods.formState.errors.orgEnhetType?.message}
            label="Organisasjonsenhetstype"
          >
            {!orgEnhet.orgEnhetsType && <option value="" />}
            {Object.values(OrgEnhetsType)
              .filter((orgEnhetsType) => orgEnhetsType !== OrgEnhetsType.DIR)
              .map((orgEnhetsType) => (
                <option key={orgEnhetsType} value={orgEnhetsType}>
                  {OrgEnhetsTypeNameMapping[orgEnhetsType]}
                </option>
              ))}
          </Select>

          <TextField
            css={css`
              width: 20%;
            `}
            {...methods.register("firesiffer")}
            error={methods.formState.errors.firesiffer?.message}
            label="Firesiffer"
          />

          <Button
            css={css`
              align-self: end;
              width: fit-content;
            `}
            icon={<FloppydiskIcon aria-hidden />}
            size="small"
            type="submit"
            variant="primary"
          >
            Lagre
          </Button>
        </VStack>
      </form>
    </FormProvider>
  );
}

function FormDateWrapper({ name }: { name: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
    },
    defaultSelected: field.value,
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label="Gyldig fra og med"
      />
    </DatePicker>
  );
}

function FormRessursSearchWrapper({
  name,
  orgEnhetLederOption,
}: {
  name: string;
  orgEnhetLederOption: any;
}) {
  const { field, fieldState } = useController({
    name,
  });

  const [lederChanged, setLederChanged] = useState<boolean>(false);

  return (
    <SelectLayoutWrapper
      error={fieldState.error}
      htmlFor="Leder"
      label="Leder for ny organisasjonsenhet"
    >
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < 3
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) => {
          field.onChange(selectedOption?.value ?? "");

          setLederChanged(true);
        }}
        placeholder="Søk etter medarbeider"
        value={lederChanged ? undefined : orgEnhetLederOption}
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

const validationSchema = yup.object({
  orgEnhetNavn: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  lederRessursId: yup.string().required("Obligatorisk"),
  firesiffer: yup.string().required("Obligatorisk"),
  orgEnhetType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
});
