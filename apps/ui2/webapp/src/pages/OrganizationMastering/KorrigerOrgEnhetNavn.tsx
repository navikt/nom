import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  Loader,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React, { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  getOrgEnhet,
  korrigerOrgEnhetNavn,
  orgEnheterKeys,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import { LabelWithValue } from "~/components/LabelWithValue";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type { KorrigerOrgEnhetNavnRequest, OrgEnhet } from "~/types/api-models";

export const KorrigerOrgEnhetNavn = () => {
  const hasAdminAccess = useHasAdminAccess();

  const { id } = useSearch({ from: "/korriger-navn-organisasjonsenhet" });

  const [requestInfo, setRequestInfo] = useState<any>();

  return (
    <>
      <PageBanner text="Korrigér organisasjonsenhetsnavn" />

      <ContentWrapper>
        {!!requestInfo ? (
          <OrgEnhetKvittering
            orgEnhetId={id ?? ""}
            title="Navn på organisasjonsenhet oppdatert"
          >
            <KvitteringContent
              nyttNavn={requestInfo.nyttNavn}
              opprinneligNavn={requestInfo.opprinneligNavn}
            />
          </OrgEnhetKvittering>
        ) : (
          <ModalProvider>
            <ContainerWithHighlightedTop padding={6}>
              {hasAdminAccess ? (
                <KorrigeringOrgEnhetNavn
                  orgEnhetId={id}
                  setRequestInfo={setRequestInfo}
                />
              ) : (
                <IkkeTilgangRedigering />
              )}
            </ContainerWithHighlightedTop>

            <ModalComponent />
          </ModalProvider>
        )}
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhetNavn({
  orgEnhetId,
  setRequestInfo,
}: {
  orgEnhetId?: string;
  setRequestInfo: (response: any) => void;
}) {
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

      <KorrigeringOrgEnhetNavnForm
        orgEnhet={orgEnhet}
        setRequestInfo={setRequestInfo}
      />
    </>
  );
}

function KorrigeringOrgEnhetNavnForm({
  orgEnhet,
  setRequestInfo,
}: {
  orgEnhet: OrgEnhet;
  setRequestInfo: (response: any) => void;
}) {
  const queryClient = useQueryClient();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const opprinneligNavn = orgEnhet.navn;

  const korrigerOrgEnhetNavnMutation = useMutation<
    unknown,
    AxiosError<Error>,
    KorrigerOrgEnhetNavnRequest
  >({
    mutationFn: async (request) => {
      return korrigerOrgEnhetNavn(request);
    },
    onSuccess: async () => {
      const nyttNavn = methods.getValues("navn");

      setRequestInfo({ opprinneligNavn, nyttNavn });

      showToast({ text: "Navn oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });
    },
  });

  const { showModal } = useModal();

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: KorrigerOrgEnhetNavnRequest = {
      id: orgEnhet.id,
      navn: values.navn,
    };

    await showModal(
      <BekreftModal
        content={
          <ModalContent
            nyttNavn={values.navn}
            opprinneligNavn={opprinneligNavn}
          />
        }
        heading="Bekreft navneendring på organisasjonsenhet"
      />,
    ).then(() => {
      korrigerOrgEnhetNavnMutation.mutate(request);
    });
  };

  const mutateError =
    korrigerOrgEnhetNavnMutation?.error?.response?.data?.message;

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <TextField
            {...methods.register("navn")}
            description="Enheten kan ikke ha likt navn som en annen enhet"
            error={methods.formState.errors.navn?.message}
            label="Nytt navn på organisasjonsenhet"
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

function ModalContent({
  opprinneligNavn,
  nyttNavn,
}: {
  opprinneligNavn: string;
  nyttNavn: string;
}) {
  return (
    <VStack gap="4">
      <div>
        <LabelWithValue
          label={"Opprinnelig navn"}
          split
          value={opprinneligNavn}
        />
      </div>

      <div>
        <LabelWithValue label={"Nytt navn"} split value={nyttNavn} />
      </div>
    </VStack>
  );
}

function KvitteringContent({
  opprinneligNavn,
  nyttNavn,
}: {
  opprinneligNavn: string;
  nyttNavn: string;
}) {
  return (
    <span>
      Organisasjonsenheten har byttet navn fra <b>{opprinneligNavn}</b> til{" "}
      <b>{nyttNavn}</b>.
    </span>
  );
}

const validationSchema = yup.object({
  navn: yup.string().required("Obligatorisk"),
});
