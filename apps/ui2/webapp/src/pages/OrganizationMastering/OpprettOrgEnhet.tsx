import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  Heading,
  HGrid,
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
  getAlleKostnadssteder,
  getAlleTilgangsenheter,
  getKostnadssted,
  getOrgEnhet,
  getRessurs,
  opprettOrgEnhet,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import {
  FormRessursSearchWrapper,
  FormTilgangsenhetSelectWrapper,
} from "~/components/forms/FormComponents";
import { LabelWithValue } from "~/components/LabelWithValue";
import { LoaderWithText } from "~/components/LoaderWithText";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/customUtils/dateUtils";
import { useNavToppnodeOrgEnhetId } from "~/customUtils/environmentUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  NomNivaaNameMapping,
  OrgEnhetsTypeNameMapping,
} from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import {
  IkkeTilgangRedigering,
  KostnadsstedSelectFormPart,
  NomNivaaSelect,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfo,
  OrgEnhetTypeSelect,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import { DynamiskOrgKart } from "~/pages/OrganizationMastering/DynamiskOrgKart";
import type {
  KostnadsstedResponse,
  NomNivaa,
  OpprettOrgEnhetRequest,
  OpprettOrgEnhetResponse,
  OrgEnhet,
  Tilgangsenhet,
} from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

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
            <ContainerWithHighlightedTop padding={24}>
              {!id ? (
                <OrgKartView />
              ) : (
                <FormView
                  overliggendeOrgEnhetId={id}
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
  return (
    <>
      <DynamiskOrgKart
        opprettKnapp
        title="Velg plassering"
        toppnodeOrgEnhetId={useNavToppnodeOrgEnhetId()}
      />

      <div
        css={css`
          display: flex;
          justify-content: end;
          gap: var(--ax-space-16);
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
  overliggendeOrgEnhetId,
  setOpprettResponse,
}: {
  overliggendeOrgEnhetId: string;
  setOpprettResponse: (response: any) => void;
}) {
  const overliggendeOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(overliggendeOrgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(overliggendeOrgEnhetId),
    enabled: !!overliggendeOrgEnhetId,
  });

  const overliggendeOrgEnhet = overliggendeOrgEnhetQuery.data;

  const kostnadsstedQuery = useQuery({
    queryKey: getKostnadssted.queryKey(overliggendeOrgEnhetId),
    queryFn: () => getKostnadssted.queryFn(overliggendeOrgEnhetId),
    enabled: !!overliggendeOrgEnhetId,
    retry: false,
    refetchOnWindowFocus: false,
  });

  const kostnadssted = kostnadsstedQuery.data;

  if (overliggendeOrgEnhetQuery.isLoading || kostnadsstedQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (!overliggendeOrgEnhet) {
    return <OrgEnhetIkkeFunnet />;
  }

  return (
    <>
      <OrgEnhetInfo
        heading="Enheten blir opprettet under"
        orgEnhet={overliggendeOrgEnhet}
        displayKostnadssted
      />

      <OpprettOrgEnhetForm
        overliggendeOrgEnhet={overliggendeOrgEnhet}
        overliggendeOrgEnhetKostnadsstedId={kostnadssted?.kostnadsstedId ?? ""}
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
  overliggendeOrgEnhet,
  overliggendeOrgEnhetKostnadsstedId,
  setOpprettResponse,
}: {
  overliggendeOrgEnhet: OrgEnhet;
  overliggendeOrgEnhetKostnadsstedId: string;
  setOpprettResponse: (response: any) => void;
}) {
  const alleTilgangsenheterQuery = useQuery({
    queryKey: getAlleTilgangsenheter.queryKey(),
    queryFn: () => getAlleTilgangsenheter.queryFn(),
  });

  const alleTilgangsenheter = alleTilgangsenheterQuery.data ?? [];

  const alleKostnadsstederQuery = useQuery({
    queryKey: getAlleKostnadssteder.queryKey(),
    queryFn: () => getAlleKostnadssteder.queryFn(),
  });

  const alleKostnadssteder = alleKostnadsstederQuery.data ?? [];

  const methods = useForm({
    resolver: yupResolver(validationSchema),
    defaultValues: {
      nomNivaa: undefined,
      tilgangsenhetId: overliggendeOrgEnhet.tilgangsenhetId,
      kostnadsstedId: overliggendeOrgEnhetKostnadsstedId,
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
      overordnetOrgEnhetId: overliggendeOrgEnhet.id,
      nomNivaa: values.nomNivaa || undefined,
    };

    await showModal(
      <BekreftModal
        content={
          <OpprettOrgEnhetModalContent
            request={request}
            tilgangsenheter={alleTilgangsenheter}
            kostnadssteder={alleKostnadssteder}
            overliggendeOrgEnhet={overliggendeOrgEnhet}
          />
        }
        heading="Bekreft opprettelse"
      />,
    ).then(() => {
      opprettOrgEnhetMutation.mutate(request);
    });
  };

  const mutateError = opprettOrgEnhetMutation?.error?.response?.data?.message;

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="space-36">
          <VStack gap="space-16">
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
              visValgtRessursInfo
            />

            <FormDateWrapper name="gyldigFom" />

            <VStack
              gap="space-16"
              css={css`
                width: 50%;
              `}
            >
              <OrgEnhetTypeSelect name="orgEnhetsType" fullWidth />

              <NomNivaaSelect name="nomNivaa" fullWidth />

              <FormTilgangsenhetSelectWrapper
                name="tilgangsenhetId"
                tilgangsenheter={alleTilgangsenheter}
              />

              <KostnadsstedSelectFormPart
                label="Kostnadssted"
                name="kostnadsstedId"
                kostnadssteder={alleKostnadssteder}
              />
            </VStack>
          </VStack>

          <div
            css={css`
              display: flex;
              justify-content: end;
              gap: var(--ax-space-16);
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
              loading={opprettOrgEnhetMutation.isPending}
            >
              Lagre
            </Button>
          </div>

          {!!mutateError &&
            opprettOrgEnhetMutation.error?.response?.status !== 409 &&
            mutateError && <Alert variant="error">{mutateError}</Alert>}

          {opprettOrgEnhetMutation.error?.response?.status === 409 && (
            <Alert variant="error">
              Det valgte navnet er allerede i bruk. Vennligst velg et annet
              navn.
            </Alert>
          )}
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
        label="Enheten er gyldig fra og med"
      />
    </DatePicker>
  );
}

function OpprettOrgEnhetModalContent({
  request,
  tilgangsenheter,
  kostnadssteder,
  overliggendeOrgEnhet,
}: {
  request: OpprettOrgEnhetRequest;
  tilgangsenheter: Tilgangsenhet[];
  kostnadssteder: KostnadsstedResponse[];
  overliggendeOrgEnhet: OrgEnhet;
}) {
  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(request.lederRessursNavident),
    queryFn: () => getRessurs.queryFn(request.lederRessursNavident),
  });

  const ressurs = ressursQuery.data;

  const valgtTilgangsenhet = tilgangsenheter.find(
    (enhet) => enhet.enhetId === request.tilgangsenhetId,
  );

  const valgtKostnadssted = kostnadssteder.find(
    (kostnadssted) => kostnadssted.kostnadsstedId === request.kostnadsstedId,
  );

  return (
    <HGrid columns="max-content max-content" gap="space-8 space-8">
      <LabelWithValue split label="Navn" value={request.navn} />
      <LabelWithValue
        split
        label="Opprett under"
        value={`${overliggendeOrgEnhet.navn} (${overliggendeOrgEnhet.id})`}
      />
      <LabelWithValue
        split
        label="Leder"
        value={`${formatVisningsnavn(ressurs?.visningsnavn)} (${ressurs?.navident})`}
      />
      <LabelWithValue
        split
        label="Gyldig f.o.m."
        value={`${formatDisplayDate(request.gyldigFom)}`}
      />
      <LabelWithValue
        split
        label="Enhetstype"
        value={OrgEnhetsTypeNameMapping[request.orgEnhetsType]}
      />
      <LabelWithValue
        split
        label="NOM-nivå"
        value={
          !!request.nomNivaa
            ? `${NomNivaaNameMapping[request.nomNivaa]}`
            : undefined
        }
      />
      <LabelWithValue
        split
        label="Tilgangsenhet"
        value={`${valgtTilgangsenhet?.navn ?? "Ukjent"} (${request.tilgangsenhetId})`}
      />
      <LabelWithValue
        split
        label="Kostnadssted"
        value={`${valgtKostnadssted?.navn ?? "Ukjent"} (${request.kostnadsstedId})`}
      />
    </HGrid>
  );
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
  nomNivaa: yup.mixed<NomNivaa>(),
  kostnadsstedId: yup.string().required("Obligatorisk"),
});
