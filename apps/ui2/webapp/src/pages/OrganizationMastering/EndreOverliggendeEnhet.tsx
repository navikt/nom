import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Alert, BodyLong, Button, VStack } from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import React, { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  endreOrgEnhetOverliggende,
  getOrgEnhet,
  orgEnheterKeys,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import {
  FormDateWrapper,
  FormOrgEnhetSearchWrapper,
} from "~/components/forms/FormComponents";
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
import type {
  EndreOpphengOrgEnhetRequest,
  EndreOpphengOrgEnhetResponse,
  OrgEnhet,
} from "~/types/api-models";
import { formatDisplayDate } from "~/utils";

export const EndreOverliggendeEnhet = () => {
  const { id } = useSearch({ from: "/endre-overliggende-enhet" });

  const hasAdminAccess = useHasAdminAccess();

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id ?? ""),
    queryFn: () => getOrgEnhet.queryFn(id ?? ""),
    enabled: !!id,
  });

  const orgEnhet = orgEnhetQuery.data;

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

  const [responseData, setResponseData] =
    useState<EndreOpphengOrgEnhetResponse>();

  if (orgEnhetQuery.isLoading) {
    return (
      <ContentWrapper>
        <LoaderWithText>Laster...</LoaderWithText>
      </ContentWrapper>
    );
  }

  if (!orgEnhet) {
    return (
      <ContentWrapper>
        <OrgEnhetIkkeFunnet />
      </ContentWrapper>
    );
  }

  return (
    <>
      <PageBanner text="Endre overliggende enhet" />

      <ContentWrapper>
        {!!responseData ? (
          <KvitteringView responseData={responseData} />
        ) : (
          <FormView orgEnhet={orgEnhet} setResponseData={setResponseData} />
        )}
      </ContentWrapper>
    </>
  );
};

function FormView({
  orgEnhet,
  setResponseData,
}: {
  orgEnhet: OrgEnhet;
  setResponseData: (response: EndreOpphengOrgEnhetResponse) => void;
}) {
  return (
    <ModalProvider>
      <ContainerWithHighlightedTop padding={6}>
        <OrgEnhetInfoWrapper
          heading="Valgt organisasjonsenhet"
          orgEnhet={orgEnhet}
        />

        <EndreOverliggendeEnhetForm
          orgEnhet={orgEnhet}
          setResponseData={setResponseData}
        />
      </ContainerWithHighlightedTop>

      <ModalComponent />
    </ModalProvider>
  );
}

function KvitteringView({
  responseData,
}: {
  responseData: EndreOpphengOrgEnhetResponse;
}) {
  return (
    <OrgEnhetKvittering
      title="Overliggende enhet endret"
      orgEnhetId={responseData.orgenhetId}
    >
      <EndreOverliggendeKvitteringContent responseData={responseData} />
    </OrgEnhetKvittering>
  );
}

function EndreOverliggendeEnhetForm({
  orgEnhet,
  setResponseData,
}: {
  orgEnhet: OrgEnhet;
  setResponseData: (response: EndreOpphengOrgEnhetResponse) => void;
}) {
  const queryClient = useQueryClient();

  const { showModal } = useModal();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const endreOverordnetOrgEnhetMutation = useMutation<
    AxiosResponse<EndreOpphengOrgEnhetResponse>,
    AxiosError<Error>,
    EndreOpphengOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return endreOrgEnhetOverliggende(request);
    },
    onSuccess: async (response) => {
      showToast({ text: "Oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });

      setResponseData(response.data);
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: EndreOpphengOrgEnhetRequest = {
      currentOverenhetId: orgEnhet.overordnetEnhetId ?? "",
      nyOverenhetId: values.nyOverenhetId,
      orgenhetId: orgEnhet.id,
      gyldigFom: values.gyldigFom,
    };

    await showModal(
      <BekreftModal
        content={<EndreOverliggendeModalContent request={request} />}
        heading="Bekreft endring på organisasjonsenhet"
      />,
    ).then(() => {
      endreOverordnetOrgEnhetMutation.mutate(request);
    });
  };

  const mutateError =
    endreOverordnetOrgEnhetMutation?.error?.response?.data?.message;

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <VStack gap="4">
            <FormOrgEnhetSearchWrapper
              label="Ny overliggende enhet"
              name="nyOverenhetId"
            />

            <FormDateWrapper name="gyldigFom" label="Gjeldende f.o.m." />
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

          {!!mutateError && <Alert variant="error">{mutateError}</Alert>}
        </VStack>
      </form>
    </FormProvider>
  );
}

function EndreOverliggendeModalContent({
  request,
}: {
  request: EndreOpphengOrgEnhetRequest;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(request.nyOverenhetId),
    queryFn: () => getOrgEnhet.queryFn(request.nyOverenhetId),
  });

  const nyOverliggendeEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    <BodyLong>
      Enheten blir liggende under{" "}
      <b>
        {nyOverliggendeEnhet?.navn} ({nyOverliggendeEnhet?.id})
      </b>{" "}
      fra og med <b>{formatDisplayDate(request.gyldigFom)}</b>.
    </BodyLong>
  );
}

function EndreOverliggendeKvitteringContent({
  responseData,
}: {
  responseData: EndreOpphengOrgEnhetResponse;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(responseData.nyOverenhetId),
    queryFn: () => getOrgEnhet.queryFn(responseData.nyOverenhetId),
  });

  const overliggendeOrgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return <LoaderWithText>Laster...</LoaderWithText>;
  }

  return (
    <BodyLong>
      Enheten blir liggende under{" "}
      <b>
        {overliggendeOrgEnhet?.navn} ({overliggendeOrgEnhet?.id})
      </b>{" "}
      fra og med <b>{formatDisplayDate(responseData.gyldigFom)}</b>.
    </BodyLong>
  );
}

const validationSchema = yup.object({
  nyOverenhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
});
