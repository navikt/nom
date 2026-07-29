import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  Button,
  DatePicker,
  Heading,
  Select,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgEnhet, opprettOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { FormRessursSearchWrapper } from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import { DynamiskOrgKart } from "~/pages/OrganizationMastering/DynamiskOrgKart";
import { OrgEnhetInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type {
  OpprettOrgEnhetRequest,
  OpprettOrgEnhetResponse,
} from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

export const OpprettOrgEnhet = () => {
  const inDevelopment = isInDevelopment();

  const { id } = useSearch({
    from: "/opprett-organisasjonsenhet",
  });

  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  const [opprettResponse, setOpprettResponse] = useState<any>();

  return (
    <>
      <PageBanner text="Opprett ny organisasjonsenhet" />

      {!!opprettResponse ? (
        <ContentWrapper>
          <OrgEnhetKvittering
            orgEnhetId={opprettResponse.data.id}
            title="Organisasjonsenheten er nå registrert i NOM"
          >
            <OrgEnhetOpprettetKvittering opprettResponse={opprettResponse} />
          </OrgEnhetKvittering>
        </ContentWrapper>
      ) : (
        <ContentWrapper limitWidth={!!id}>
          {!id ? (
            <ContainerWithHighlightedTop padding={6}>
              <DynamiskOrgKart
                opprettKnapp
                title="Velg plassering"
                toppOrgEnhetId={inDevelopment ? "sa312u" : "py437s"}
              />

              {!id && (
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
                </div>
              )}
            </ContainerWithHighlightedTop>
          ) : (
            <ContainerWithHighlightedTop padding={6}>
              <OrgEnhetInfoWrapper
                heading="Enheten blir opprettet under"
                orgEnhetId={id}
              />

              <OrgEnhetInfoForm
                orgEnhetId={id}
                setOpprettResponse={setOpprettResponse}
              />
            </ContainerWithHighlightedTop>
          )}
        </ContentWrapper>
      )}
    </>
  );
};

function OrgEnhetInfoForm({
  orgEnhetId,
  setOpprettResponse,
}: {
  orgEnhetId: string | null;
  setOpprettResponse: (response: any) => void;
}) {
  if (!orgEnhetId) {
    return;
  }

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const opprettOrgEnhetMutation = useMutation<
    AxiosResponse<OpprettOrgEnhetResponse>,
    AxiosError<Error>,
    OpprettOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return opprettOrgEnhet(request);
    },
    onSuccess: async (response) => {
      setOpprettResponse(response);

      showToast({ text: "Opprettet" });
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OpprettOrgEnhetRequest = {
      ...values,
      overordnetOrgEnhetId: orgEnhetId,
    };

    opprettOrgEnhetMutation.mutate(request);
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <VStack gap="4">
            <Heading level="2" size="medium">
              Om enheten
            </Heading>

            <TextField
              {...methods.register("navn")}
              description="Enheten kan ikke ha likt navn som en annen enhet"
              error={methods.formState.errors.navn?.message}
              label="Navn på den nye organisasjonsenhet"
            />

            <FormRessursSearchWrapper
              label="Leder på organisasjonsenhet"
              name="lederRessursNavident"
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
              <option value="" />
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
              {...methods.register("tilgangsenhetId")}
              error={methods.formState.errors.tilgangsenhetId?.message}
              label="Tilgangsenhet"
            />
          </VStack>

          <div
            css={css`
              display: flex;
              justify-content: end;
              gap: var(--a-spacing-4);
            `}
          >
            <SmartLink href="/opprett-organisasjonsenhet">
              <Button size="small" variant="secondary">
                Tilbake
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
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label="Dato"
      />
    </DatePicker>
  );
}

function OrgEnhetOpprettetKvittering({
  opprettResponse,
}: {
  opprettResponse: any;
}) {
  const opprettetOrgEnhet = opprettResponse.data;

  return (
    <>
      <BodyLong>
        Organisasjonsenheten <b>{opprettetOrgEnhet.navn}</b> er nå registrert i
        NOM med ID <b>{opprettetOrgEnhet.id}</b>.
      </BodyLong>
    </>
  );
}
export function OrgEnhetInfoWrapper({
  orgEnhetId,
  heading,
}: {
  orgEnhetId?: string;
  heading: string;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    !!orgEnhet && (
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
    )
  );
}

const validationSchema = yup.object({
  navn: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  lederRessursNavident: yup.string().required("Obligatorisk"),
  tilgangsenhetId: yup.string().required("Obligatorisk"),
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk")
    .required("Obligatorisk"),
});
