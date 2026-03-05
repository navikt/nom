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
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import axios, { type AxiosError } from "axios";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgEnhet, oppdaterOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type { OppdaterOrgEnhetRequest, OrgEnhet } from "~/types/api-models";
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
        heading="Valgt organisasjonsenhet"
        orgEnhetId={orgEnhetId}
      />

      <KorrigeringOrgEnhetForm orgEnhet={orgEnhet} />
    </>
  );
}

function KorrigeringOrgEnhetForm({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const navigate = useNavigate();

  const methods = useForm({
    defaultValues: {
      navn: orgEnhet.navn,
      gyldigFom: orgEnhet.gyldigFom,
      lederRessursNavident: orgEnhet.leder.navident,
      firesiffer: orgEnhet.remedyEnhetId ?? "",
      orgEnhetsType: orgEnhet.orgEnhetsType,
    },
    resolver: yupResolver(validationSchema),
  });

  const existingOrgEnhetLederOption = {
    label: `${orgEnhet.leder.visningsnavn} (${orgEnhet.leder.navident})`,
    value: orgEnhet.leder.navident,
  };

  const oppdaterOrgEnhetMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OppdaterOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return oppdaterOrgEnhet(request);
    },
    onSuccess: async () => {
      showToast({ text: "Oppdatert" });

      navigate({
        to: "/organisasjonsmastring",
      }).then();
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OppdaterOrgEnhetRequest = {
      ...values,
      id: orgEnhet.id,
    };

    oppdaterOrgEnhetMutation.mutate(request);
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <VStack gap="4">
            <TextField
              {...methods.register("navn")}
              description="Enheten kan ikke ha likt navn som en annen enhet"
              error={methods.formState.errors.navn?.message}
              label="Navn på organisasjonsenhet"
            />

            <FormRessursSearchWrapper
              name="lederRessursNavident"
              orgEnhetLederOption={existingOrgEnhetLederOption}
            />

            <FormDateWrapper name="gyldigFom" />

            <Select
              css={css`
                width: fit-content;
              `}
              {...methods.register("orgEnhetsType")}
              error={methods.formState.errors.orgEnhetsType?.message}
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
          </VStack>

          <div
            css={css`
              display: flex;
              justify-content: end;
              gap: var(--a-spacing-4);
            `}
          >
            <SmartLink href="/organisasjonsmastring">
              <Button size="small" variant="secondary">
                Avbryt
              </Button>
            </SmartLink>

            <Button
              icon={<FloppydiskIcon aria-hidden />}
              size="small"
              type="submit"
            >
              Lagre
            </Button>
          </div>
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
  navn: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  lederRessursNavident: yup.string().required("Obligatorisk"),
  firesiffer: yup.string().optional(),
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
});
