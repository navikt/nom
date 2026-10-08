import { useSearch } from "@tanstack/react-router";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import {
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfoWrapper,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import React from "react";
import { PageBanner } from "~/components/PageBanner";
import {
  EndreOpphengOrgEnhetRequest,
  EndreOpphengOrgEnhetResponse,
  OrgEnhet,
} from "~/types/api-models";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  endreOrgEnhetOverliggende,
  getOrgEnhet,
  orgEnheterKeys,
} from "~/api/react-query-endpoints";
import { LoaderWithText } from "~/components/LoaderWithText";
import { FormProvider, useForm } from "react-hook-form";
import { yupResolver } from "@hookform/resolvers/yup";
import * as yup from "yup";
import { BekreftModal } from "~/components/BekreftModal";
import type { AxiosError, AxiosResponse } from "axios";
import { showToast } from "~/hooks/showToast";
import { Alert, Button, VStack } from "@navikt/ds-react";
import { css } from "@emotion/react";
import { SmartLink } from "~/components/navigation/SmartLink";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { FormDateWrapper } from "~/components/forms/FormComponents";
import { OrgEnhetSearch } from "~/components/select/OrgEnhetSearch";

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
        <FormView orgEnhet={orgEnhet} setChangeInfo={() => {}} />
      </ContentWrapper>
    </>
  );
};

function FormView({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
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
          setChangeInfo={setChangeInfo}
        />
      </ContainerWithHighlightedTop>

      <ModalComponent />
    </ModalProvider>
  );
}

function EndreOverliggendeEnhetForm({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
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
    onSuccess: async () => {
      showToast({ text: "Oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });

      setChangeInfo({ testtekst: "testtekst" });
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: EndreOpphengOrgEnhetRequest = {
      currentOverenhetId: orgEnhet.overordnetEnhetId ?? "",
      nyOverenhetId: values.nyOverenhetId,
      orgenhetId: orgEnhet.id,
    };

    await showModal(
      <BekreftModal
        content={<div>Bekreft endring</div>}
        heading="Bekreft endring på organisasjonsenhet"
      />,
    ).then(() => {
      //endreOverordnetOrgEnhetMutation.mutate(request);

      console.log("Would save", request);
    });
  };

  const mutateError =
    endreOverordnetOrgEnhetMutation?.error?.response?.data?.message;

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <VStack gap="4">
            <OrgEnhetSearch
              name="nyOverenhetId"
              label="Ny overliggende enhet"
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

            {!!mutateError && <Alert variant="error">{mutateError}</Alert>}
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}

const validationSchema = yup.object({
  nyOverenhetId: yup.string().required("Obligatorisk"),
});
