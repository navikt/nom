import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  DatePicker,
  Heading,
  Loader,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { subDays } from "date-fns";
import React from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  getOrgEnhet,
  getPeriodiserteLedereOrgEnhet,
  getRessurs,
  opprettOrgEnhetLeder,
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
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  FormRessursSearchWrapper,
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import {
  nomDateIsAfterToday,
  nomDateIsTodayOrAfter,
  nomDateIsTodayOrBefore,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type {
  OpprettOrgEnhetLederRequest,
  OrgEnhet,
  OrgEnhetLederResponse,
} from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

export const KorrigerOrgEnhetLeder = () => {
  const hasAdminAccess = useHasAdminAccess();

  const { id } = useSearch({ from: "/korriger-leder-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Endre leder på organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          {hasAdminAccess ? (
            <ModalProvider>
              <KorrigeringOrgEnhetLeder orgEnhetId={id} />

              <ModalComponent />
            </ModalProvider>
          ) : (
            <IkkeTilgangRedigering />
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhetLeder({ orgEnhetId }: { orgEnhetId?: string }) {
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
        orgEnhet={orgEnhet}
      />

      <KorrigeringOrgEnhetLederForm orgEnhet={orgEnhet} />
    </>
  );
}

function KorrigeringOrgEnhetLederForm({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const periodiserteLedereQuery = useQuery({
    queryKey: getPeriodiserteLedereOrgEnhet.queryKey(orgEnhet.id),
    queryFn: () => getPeriodiserteLedereOrgEnhet.queryFn(orgEnhet.id),
  });

  const periodiserteLedere = periodiserteLedereQuery.data;

  const aktivtLederskap = periodiserteLedere?.find(
    (lederskap) =>
      nomDateIsTodayOrBefore(lederskap.startdato) &&
      nomDateIsTodayOrAfter(lederskap.sluttdato),
  );

  const planlagtLederskap = periodiserteLedere?.find((lederskap) =>
    nomDateIsAfterToday(lederskap.startdato),
  );

  const navigate = useNavigate();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const queryClient = useQueryClient();

  const { showModal } = useModal();

  const opprettOrgEnhetLederMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OpprettOrgEnhetLederRequest
  >({
    mutationFn: async (request) => {
      return opprettOrgEnhetLeder(request);
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
    const request: OpprettOrgEnhetLederRequest = {
      orgEnhetId: orgEnhet.id,
      navident: values.lederRessursNavident,
      gyldigFom: values.gyldigFom,
    };

    await showModal(
      <BekreftModal
        content={
          <LeggTilLederModalContent
            aktivtLederskap={aktivtLederskap}
            planlagtLederskap={planlagtLederskap}
            request={request}
          />
        }
        heading="Bekreft lederendring"
      />,
    ).then(() => {
      opprettOrgEnhetLederMutation.mutate(request);
    });
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="8">
          <VStack gap="6">
            <FormRessursSearchWrapper
              label="Ny leder på organisasjonsenhet"
              name="lederRessursNavident"
            />

            <FormDateWrapper label="Leder fra og med" name="gyldigFom" />

            <FremtidigLeder planlagtLederskap={planlagtLederskap} />
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

            {!!opprettOrgEnhetLederMutation?.error?.response?.data?.message && (
              <Alert variant="error">
                {opprettOrgEnhetLederMutation?.error?.response?.data?.message}
              </Alert>
            )}
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}

function FremtidigLeder({
  planlagtLederskap,
}: {
  planlagtLederskap: OrgEnhetLederResponse | undefined;
}) {
  if (!planlagtLederskap) {
    return;
  }

  return (
    <VStack gap="2">
      <Heading level="2" size="small">
        Fremtidig leder:
      </Heading>

      <span>
        Det er registrert at{" "}
        <b>{formatVisningsnavn(planlagtLederskap.visningsnavn)}</b> blir leder
        for organisasjonsenheten fra og med{" "}
        <b>{formatDisplayDate(planlagtLederskap.startdato)}</b>.
      </span>

      <span>
        En registrering av ny leder overskriver den registrerte fremtidige
        lederen.
      </span>
    </VStack>
  );
}

function LeggTilLederModalContent({
  request,
  aktivtLederskap,
  planlagtLederskap,
}: {
  request: OpprettOrgEnhetLederRequest;
  aktivtLederskap: OrgEnhetLederResponse | undefined;
  planlagtLederskap: OrgEnhetLederResponse | undefined;
}) {
  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(request?.navident ?? ""),
    queryFn: () => getRessurs.queryFn(request?.navident ?? ""),
    enabled: !!request.navident,
  });

  const ressurs = ressursQuery.data;

  if (ressursQuery.isLoading) {
    return <LoaderWithText />;
  }

  return (
    <VStack gap="4">
      <span>
        Endringen innebærer at{" "}
        <b>{formatVisningsnavn(ressurs?.visningsnavn)}</b> blir leder for
        enheten fra og med <b>{formatDisplayDate(request.gyldigFom)}</b>.
      </span>
      <span>
        Dette betyr at{" "}
        <b>{formatVisningsnavn(aktivtLederskap?.visningsnavn)}</b> er leder for
        enheten til og med{" "}
        <b>
          {!!request.gyldigFom
            ? formatDisplayDate(subDays(new Date(request.gyldigFom), 1))
            : "dagen før dette"}
        </b>
        .
      </span>
      {!!planlagtLederskap && (
        <span>
          Det planlagte lederskapet til{" "}
          <b>{formatVisningsnavn(planlagtLederskap.visningsnavn)}</b> fra og med{" "}
          <b>{formatDisplayDate(planlagtLederskap.startdato)}</b> blir
          overskrevet.
        </span>
      )}
    </VStack>
  );
}

function FormDateWrapper({ name, label }: { name: string; label: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
    },
    defaultSelected: field.value ? new Date(field.value) : undefined,
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label={label}
        size="medium"
      />
    </DatePicker>
  );
}

const validationSchema = yup.object({
  lederRessursNavident: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
});
