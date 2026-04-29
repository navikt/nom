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
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { type AxiosError } from "axios";
import React from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  endreOrgEnhetInfo,
  getOrgEnhet,
  opprettOrgEnhetLeder,
  orgEnheterKeys,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import {
  formatDateWithoutTimezone,
  todayWithoutTimezone,
} from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  NomNivaaNameMapping,
  OrgEnhetsTypeNameMapping,
} from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import {
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type {
  OrgEnhet,
  OvrigeEndringerOrgEnhetRequest,
} from "~/types/api-models";
import { OpprettOrgEnhetLederRequest } from "~/types/api-models";
import { NomNivaa } from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";

export const KorrigerOrgEnhet = () => {
  const hasAdminAccess = useHasAdminAccess();

  const { id } = useSearch({ from: "/korriger-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Korrigér organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          {hasAdminAccess ? (
            <KorrigeringOrgEnhet orgEnhetId={id} />
          ) : (
            <IkkeTilgangRedigering />
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhet({ orgEnhetId }: { orgEnhetId?: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
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
    return <OrgEnhetIkkeFunnet />;
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

  const queryClient = useQueryClient();

  const methods = useForm({
    defaultValues: {
      gyldigFom: orgEnhet.gyldigFom,
      firesiffer: orgEnhet.remedyEnhetId ?? "",
      orgEnhetsType: orgEnhet.orgEnhetsType,
      nomNivaa: orgEnhet.nomNivaa ?? undefined,
    },
    resolver: yupResolver(validationSchema),
  });

  const endreOrgEnhetInformasjonMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OvrigeEndringerOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return endreOrgEnhetInfo(request);
    },
    onSuccess: async () => {
      showToast({ text: "Oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });

      navigate({
        to: "/organisasjonsmastring",
      }).then();
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OvrigeEndringerOrgEnhetRequest = {
      id: orgEnhet.id,
      firesiffer: values.firesiffer,
      gyldigFom: values.gyldigFom,
      nomNivaa: values.nomNivaa ?? undefined,
      orgEnhetsType: values.orgEnhetsType,
    };

    endreOrgEnhetInformasjonMutation.mutate(request);
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

            <Select
              css={css`
                width: fit-content;
              `}
              {...methods.register("nomNivaa")}
              error={methods.formState.errors.nomNivaa?.message}
              label="Organisasjonsenhetstype"
            >
              {!orgEnhet.nomNivaa && <option value={undefined} />}
              {Object.values(NomNivaa).map((nomNivaa) => (
                <option key={nomNivaa} value={nomNivaa}>
                  {NomNivaaNameMapping[nomNivaa]}
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
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
  firesiffer: yup.string().optional(),
  nomNivaa: yup
    .mixed<NomNivaa>()
    .nullable()
    .notRequired()
    .test(
      "valid-if-present",
      "Ugyldig",
      (value) => !value || Object.values(NomNivaa).includes(value),
    ),
});
