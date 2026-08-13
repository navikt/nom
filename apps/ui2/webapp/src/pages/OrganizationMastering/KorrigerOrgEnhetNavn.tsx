import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, BodyLong, Button, TextField, VStack } from "@navikt/ds-react";
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
import { LoaderWithText } from "~/components/LoaderWithText";
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
  OrgEnhetInfoWrapper,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import type { KorrigerOrgEnhetNavnRequest, OrgEnhet } from "~/types/api-models";

export const KorrigerOrgEnhetNavn = () => {
  const { id } = useSearch({ from: "/korriger-navn-organisasjonsenhet" });

  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return (
      <ContentWrapper>
        <IkkeTilgangRedigering />
      </ContentWrapper>
    );
  }

  if (!id) {
    return (
      <ContentWrapper>
        <OrgEnhetIkkeFunnet />
      </ContentWrapper>
    );
  }

  const [changeInfo, setChangeInfo] = useState<any>();

  return (
    <>
      <PageBanner text="Korrigér organisasjonsenhetsnavn" />

      <ContentWrapper>
        {!!changeInfo ? (
          <KvitteringView changeInfo={changeInfo} orgEnhetId={id} />
        ) : (
          <FormView orgEnhetId={id} setChangeInfo={setChangeInfo} />
        )}
      </ContentWrapper>
    </>
  );
};

function FormView({
  orgEnhetId,
  setChangeInfo,
}: {
  orgEnhetId: string;
  setChangeInfo: (response: any) => void;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  if (!orgEnhet) {
    return <OrgEnhetIkkeFunnet />;
  }

  return (
    <ModalProvider>
      <ContainerWithHighlightedTop padding={6}>
        <OrgEnhetInfoWrapper
          heading="Valgt organisasjonsenhet"
          orgEnhet={orgEnhet}
        />

        <KorrigerOrgEnhetNavnForm
          orgEnhet={orgEnhet}
          setChangeInfo={setChangeInfo}
        />
      </ContainerWithHighlightedTop>

      <ModalComponent />
    </ModalProvider>
  );
}

function KvitteringView({
  orgEnhetId,
  changeInfo,
}: {
  orgEnhetId: string;
  changeInfo: any;
}) {
  return (
    <OrgEnhetKvittering
      orgEnhetId={orgEnhetId}
      title="Navn på organisasjonsenhet oppdatert"
    >
      <KorrigerOrgEnhetNavnKvitteringContent
        nyttNavn={changeInfo.nyttNavn}
        opprinneligNavn={changeInfo.opprinneligNavn}
      />
    </OrgEnhetKvittering>
  );
}

function KorrigerOrgEnhetNavnForm({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
}) {
  const opprinneligNavn = orgEnhet.navn;

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const queryClient = useQueryClient();

  const { showModal } = useModal();

  const korrigerOrgEnhetNavnMutation = useMutation<
    unknown,
    AxiosError<Error>,
    KorrigerOrgEnhetNavnRequest
  >({
    mutationFn: async (request) => {
      return korrigerOrgEnhetNavn(request);
    },
    onSuccess: async () => {
      setChangeInfo({ opprinneligNavn, nyttNavn: methods.getValues("navn") });

      showToast({ text: "Navn oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: KorrigerOrgEnhetNavnRequest = {
      id: orgEnhet.id,
      navn: values.navn,
    };

    await showModal(
      <BekreftModal
        content={
          <KorrigerOrgEnhetNavnModalContent
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

function KorrigerOrgEnhetNavnModalContent({
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

function KorrigerOrgEnhetNavnKvitteringContent({
  opprinneligNavn,
  nyttNavn,
}: {
  opprinneligNavn: string;
  nyttNavn: string;
}) {
  return (
    <BodyLong>
      Organisasjonsenheten har byttet navn fra <b>{opprinneligNavn}</b> til{" "}
      <b>{nyttNavn}</b>.
    </BodyLong>
  );
}

const validationSchema = yup.object({
  navn: yup.string().required("Obligatorisk"),
});
