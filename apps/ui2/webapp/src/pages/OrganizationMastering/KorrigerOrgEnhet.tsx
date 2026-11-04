import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import { type AxiosError } from "axios";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  endreOrgEnhetInfo,
  getAlleTilgangsenheter,
  getOrgEnhet,
  orgEnheterKeys,
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
import { formatDateWithoutTimezone } from "~/customUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  IkkeTilgangRedigering,
  NomNivaaSelect,
  OrgEnhetIkkeFunnet,
  OrgEnhetInfo,
  OrgEnhetTypeSelect,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import {
  NomNivaa,
  OrgEnhet,
  OrgEnhetsType,
  OvrigeEndringerOrgEnhetRequest,
} from "~/types/api-models";
import { FormTilgangsenhetSelectWrapper } from "~/components/forms/FormComponents";

export const KorrigerOrgEnhet = () => {
  const { id } = useSearch({ from: "/korriger-organisasjonsenhet" });

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

  const [changeInfo, setChangeInfo] = useState<any>();

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
      <PageBanner text="Korrigér organisasjonsenhet" />

      <ContentWrapper>
        {!!changeInfo ? (
          <KvitteringView changeInfo={changeInfo} orgEnhet={orgEnhet} />
        ) : (
          <FormView orgEnhet={orgEnhet} setChangeInfo={setChangeInfo} />
        )}
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
        <OrgEnhetInfo heading="Valgt organisasjonsenhet" orgEnhet={orgEnhet} />

        <KorrigerOrgEnhetForm
          orgEnhet={orgEnhet}
          setChangeInfo={setChangeInfo}
        />
      </ContainerWithHighlightedTop>

      <ModalComponent />
    </ModalProvider>
  );
}

function KvitteringView({
  orgEnhet,
  changeInfo,
}: {
  orgEnhet: OrgEnhet;
  changeInfo: any;
}) {
  return (
    <OrgEnhetKvittering
      orgEnhetId={orgEnhet.id}
      title="Leder på organisasjonsenhet oppdatert"
    >
      <KorrigerOrgEnhetKvitteringContent orgEnhet={orgEnhet} />
    </OrgEnhetKvittering>
  );
}

function KorrigerOrgEnhetForm({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
}) {
  const hasAdminAccess = useHasAdminAccess();

  const queryClient = useQueryClient();

  const alleTilgangsenheterQuery = useQuery({
    queryKey: getAlleTilgangsenheter.queryKey(),
    queryFn: () => getAlleTilgangsenheter.queryFn(),
  });

  const alleTilgangsenheter = alleTilgangsenheterQuery.data ?? [];

  const methods = useForm({
    defaultValues: {
      gyldigFom: orgEnhet.gyldigFom,
      orgEnhetsType: orgEnhet.orgEnhetsType,
      nomNivaa: orgEnhet.nomNivaa,
      tilgangsenhetId: orgEnhet.tilgangsenhetId,
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

      setChangeInfo({ testtekst: "testtekst" });
    },
  });

  const { showModal } = useModal();

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OvrigeEndringerOrgEnhetRequest = {
      id: orgEnhet.id,
      gyldigFom: values.gyldigFom,
      orgEnhetsType: values.orgEnhetsType ?? undefined,
      nomNivaa: values.nomNivaa,
      tilgangsenhetId: values.tilgangsenhetId,
    };

    await showModal(
      <BekreftModal
        content={<KorrigerOrgEnhetModalContent orgEnhet={orgEnhet} />}
        heading="Bekreft endring på organisasjonsenhet"
      />,
    ).then(() => {
      endreOrgEnhetInformasjonMutation.mutate(request);
    });
  };

  const mutateError =
    endreOrgEnhetInformasjonMutation?.error?.response?.data?.message;

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <VStack
            gap="4"
            css={css`
              width: 50%;
            `}
          >
            <FormDateWrapper name="gyldigFom" />

            <OrgEnhetTypeSelect
              emptyOption={!orgEnhet.orgEnhetsType}
              name="orgEnhetsType"
            />

            {hasAdminAccess && (
              <>
                <NomNivaaSelect name="nomNivaa" fullWidth />

                <FormTilgangsenhetSelectWrapper
                  name="tilgangsenhetId"
                  tilgangsenheter={alleTilgangsenheter}
                />
              </>
            )}
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
              loading={endreOrgEnhetInformasjonMutation.isPending}
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

function KorrigerOrgEnhetModalContent({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  return (
    <BodyLong>
      Informasjon blir oppdatert for <b>{orgEnhet.navn}</b>.
    </BodyLong>
  );
}

function KorrigerOrgEnhetKvitteringContent({
  orgEnhet,
}: {
  orgEnhet: OrgEnhet;
}) {
  return (
    <BodyLong>
      Informasjon oppdatert for <b>{orgEnhet.navn}</b>.
    </BodyLong>
  );
}

const validationSchema = yup.object({
  gyldigFom: yup.string().required("Obligatorisk"),
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
  nomNivaa: yup
    .mixed<NomNivaa>()
    .transform((value) => (value === "" ? undefined : value))
    .oneOf(Object.values(NomNivaa), "Ugyldig verdi")
    .optional(),
  tilgangsenhetId: yup.string().required("Obligatorisk"),
});
