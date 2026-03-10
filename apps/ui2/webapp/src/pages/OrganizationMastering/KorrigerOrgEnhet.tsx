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
import { type AxiosError } from "axios";
import React from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgEnhet, oppdaterOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
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
      gyldigFom: orgEnhet.gyldigFom,

      firesiffer: orgEnhet.remedyEnhetId ?? "",
      orgEnhetsType: orgEnhet.orgEnhetsType,
    },
    resolver: yupResolver(validationSchema),
  });

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

const validationSchema = yup.object({
  gyldigFom: yup.string().required("Obligatorisk"),
  firesiffer: yup.string().optional(),
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
});
