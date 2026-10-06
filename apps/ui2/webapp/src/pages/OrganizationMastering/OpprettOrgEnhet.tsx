import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  Heading,
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

import {
  getAlleTilgangsenheter,
  getKostnadssted,
  getOrgEnhet,
  opprettOrgEnhet,
} from "~/api/react-query-endpoints";
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
import { TilgangsenhetSelect } from "~/components/select/FysiskBrukerstedSelect";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  FormRessursSearchWrapper,
  IkkeTilgangRedigering,
  KostnadsstedSelectFormPart,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfoWrapper,
  OrgEnhetTypeSelect,
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
    queryKey: getOrgEnhet.queryKey(overordnetOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(overordnetOrgEnhetId),
    enabled: !!overordnetOrgEnhetId,
  });

  const kostnadsstedQuery = useQuery({
    queryKey: getKostnadssted.queryKey(overordnetOrgEnhetId),
    queryFn: () => getKostnadssted.queryFn(overordnetOrgEnhetId),
    enabled: !!overordnetOrgEnhetId,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const kostnadssted = kostnadsstedQuery.data;

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading || kostnadsstedQuery.isLoading) {
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
        overordnetOrgEnhetKostnadsstedId={kostnadssted?.kostnadsstedId ?? ""}
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
  overordnetOrgEnhetKostnadsstedId,
  setOpprettResponse,
}: {
  overordnetOrgEnhet: OrgEnhet;
  overordnetOrgEnhetKostnadsstedId: string;
  setOpprettResponse: (response: any) => void;
}) {
  const methods = useForm({
    resolver: yupResolver(validationSchema),
    defaultValues: {
      tilgangsenhetId: overordnetOrgEnhet.tilgangsenhetId,
      kostnadsstedId: overordnetOrgEnhetKostnadsstedId,
    },
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
              autoComplete="off"
            />

            <FormRessursSearchWrapper
              label="Leder på organisasjonsenhet"
              name="lederRessursNavident"
              valgtRessursInfo
            />

            <FormDateWrapper name="gyldigFom" />

            <div
              css={css`
                width: 50%;
              `}
            >
              <OrgEnhetTypeSelect name="orgEnhetsType" fullWidth />
            </div>

            <FormTilgangsenhetSelectWrapper name="tilgangsenhetId" />

            <div
              css={css`
                width: 50%;
              `}
            >
              <KostnadsstedSelectFormPart
                label="Kostnadssted"
                name="kostnadsstedId"
              />
            </div>
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

function FormTilgangsenhetSelectWrapper({ name }: { name: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const alleTilgangsenheterQuery = useQuery({
    queryKey: getAlleTilgangsenheter.queryKey(),
    queryFn: () => getAlleTilgangsenheter.queryFn(),
  });

  const alleTilgangsenheter = alleTilgangsenheterQuery.data ?? [];

  return (
    <div
      css={css`
        width: 50%;
      `}
    >
      <TilgangsenhetSelect
        error={fieldState.error}
        label="Tilgangsenhet"
        tilgangsenheter={alleTilgangsenheter}
        value={field.value}
        onChange={field.onChange}
      />
    </div>
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
  kostnadsstedId: yup.string().required("Obligatorisk"),
});
