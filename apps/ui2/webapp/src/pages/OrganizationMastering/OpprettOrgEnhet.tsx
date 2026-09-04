import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
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
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import {
  FormRessursSearchWrapper,
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfoWrapper,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import { DynamiskOrgKart } from "~/pages/OrganizationMastering/DynamiskOrgKart";
import type {
  OpprettOrgEnhetRequest,
  OpprettOrgEnhetResponse,
  OrgEnhet,
} from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

export const OpprettOrgEnhet = () => {
  const { id } = useSearch({
    from: "/opprett-organisasjonsenhet",
  });

  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return (
      <ContentWrapper>
        <IkkeTilgangRedigering />
      </ContentWrapper>
    );
  }

  const [opprettResponse, setOpprettResponse] = useState<any>();

  return (
    <>
      <PageBanner text="Opprett ny organisasjonsenhet" />

      <ContentWrapper limitWidth={!!id}>
        {!!opprettResponse ? (
          <KvitteringView opprettResponse={opprettResponse} />
        ) : (
          <ModalProvider>
            <ContainerWithHighlightedTop padding={6}>
              {!id ? (
                <OrgKartView />
              ) : (
                <FormView
                  overordnetOrgEnhetId={id}
                  setOpprettResponse={setOpprettResponse}
                />
              )}
            </ContainerWithHighlightedTop>

            <ModalComponent />
          </ModalProvider>
        )}
      </ContentWrapper>
    </>
  );
};

function OrgKartView() {
  const inDevelopment = isInDevelopment();

  return (
    <>
      <DynamiskOrgKart
        opprettKnapp
        title="Velg plassering"
        toppOrgEnhetId={inDevelopment ? "sa312u" : "py437s"}
      />

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
    </>
  );
}

function FormView({
  overordnetOrgEnhetId,
  setOpprettResponse,
}: {
  overordnetOrgEnhetId: string;
  setOpprettResponse: (response: any) => void;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(overordnetOrgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(overordnetOrgEnhetId ?? ""),
    enabled: !!overordnetOrgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (!orgEnhet) {
    return <OrgEnhetIkkeFunnet />;
  }

  return (
    <>
      <OrgEnhetInfoWrapper
        heading="Enheten blir opprettet under"
        orgEnhet={orgEnhet}
      />

      <OpprettOrgEnhetForm
        overordnetOrgEnhet={orgEnhet}
        setOpprettResponse={setOpprettResponse}
      />
    </>
  );
}

function KvitteringView({ opprettResponse }: { opprettResponse: any }) {
  return (
    <OrgEnhetKvittering
      orgEnhetId={opprettResponse.data.id}
      title="Organisasjonsenheten er nå registrert i NOM"
    >
      <OpprettOrgEnhetKvitteringContent opprettResponse={opprettResponse} />
    </OrgEnhetKvittering>
  );
}

function OpprettOrgEnhetForm({
  overordnetOrgEnhet,
  setOpprettResponse,
}: {
  overordnetOrgEnhet: OrgEnhet;
  setOpprettResponse: (response: any) => void;
}) {
  const methods = useForm({
    resolver: yupResolver(validationSchema),
    defaultValues: { tilgangsenhetId: overordnetOrgEnhet.tilgangsenhetId },
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

  const { showModal } = useModal();

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OpprettOrgEnhetRequest = {
      ...values,
      overordnetOrgEnhetId: overordnetOrgEnhet.id,
    };

    await showModal(
      <BekreftModal
        content={<OpprettOrgEnhetModalContent />}
        heading="Bekreft opprettelse av organisasjonsenhet"
      />,
    ).then(() => {
      opprettOrgEnhetMutation.mutate(request);
    });
  };

  const mutateError = opprettOrgEnhetMutation?.error?.response?.data?.message;

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
              label="Navn på ny organisasjonsenhet"
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

          {!!mutateError && <Alert variant="error">{mutateError}</Alert>}
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

function OpprettOrgEnhetModalContent() {
  return <BodyLong>Testmelding</BodyLong>;
}

function OpprettOrgEnhetKvitteringContent({
  opprettResponse,
}: {
  opprettResponse: any;
}) {
  const opprettetOrgEnhet = opprettResponse.data;

  return (
    <BodyLong>
      Organisasjonsenheten <b>{opprettetOrgEnhet.navn}</b> er nå registrert i
      NOM med ID <b>{opprettetOrgEnhet.id}</b>.
    </BodyLong>
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
