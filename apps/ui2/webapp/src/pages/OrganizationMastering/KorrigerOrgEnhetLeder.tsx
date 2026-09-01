import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  Heading,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { addDays, subDays } from "date-fns";
import React, { useState } from "react";
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
  OrgEnhetInfoWrapper,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetKvittering } from "~/pages/OrganizationMastering/Common/OrgEnhetKvittering";
import {
  nomDateIsAfter,
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
  const { id } = useSearch({ from: "/korriger-leder-organisasjonsenhet" });

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
      <PageBanner text="Endre leder på organisasjonsenhet" />

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
        <OrgEnhetInfoWrapper
          heading="Valgt organisasjonsenhet"
          orgEnhet={orgEnhet}
        />

        <KorrigerOrgEnhetLederForm
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
      <KorrigerOrgEnhetLederKvitteringContent
        changeInfo={changeInfo}
        orgEnhet={orgEnhet}
      />
    </OrgEnhetKvittering>
  );
}

function KorrigerOrgEnhetLederForm({
  orgEnhet,
  setChangeInfo,
}: {
  orgEnhet: OrgEnhet;
  setChangeInfo: (response: any) => void;
}) {
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

  const methods = useForm({
    resolver: yupResolver(validationSchema),
    context: { aktivtLederskapStart: aktivtLederskap?.startdato },
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
    onSuccess: async (_, request) => {
      setChangeInfo({ request, aktivtLederskap });

      showToast({ text: "Leder oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });
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
          <KorrigerOrgEnhetLederModalContent
            aktivtLederskap={aktivtLederskap}
            request={request}
          />
        }
        heading="Bekreft lederendring"
      />,
    ).then(() => {
      opprettOrgEnhetLederMutation.mutate(request);
    });
  };

  const mutateError =
    opprettOrgEnhetLederMutation?.error?.response?.data?.message;

  return (
    <>
      <Alert
        css={css`
          width: 50%;
        `}
        variant="info"
      >
        Dagens leder: <b>{orgEnhet.leder.visningsnavn}</b> siden{" "}
        <b>{formatDisplayDate(aktivtLederskap?.startdato)}</b>
      </Alert>

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

              {!!mutateError && <Alert variant="error">{mutateError}</Alert>}
            </div>
          </VStack>
        </form>
      </FormProvider>
    </>
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

      <BodyLong>
        Det er registrert at{" "}
        <b>{formatVisningsnavn(planlagtLederskap.visningsnavn)}</b> blir leder
        for organisasjonsenheten fra og med{" "}
        <b>{formatDisplayDate(planlagtLederskap.startdato)}</b>.
      </BodyLong>

      <BodyLong>
        En registrering av ny leder overskriver den registrerte fremtidige
        lederen.
      </BodyLong>
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

function KorrigerOrgEnhetLederModalContent({
  request,
  aktivtLederskap,
}: {
  request: OpprettOrgEnhetLederRequest;
  aktivtLederskap: OrgEnhetLederResponse | undefined;
}) {
  const navident = request.navident ?? "";

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(navident),
    queryFn: () => getRessurs.queryFn(navident),
  });

  const ressurs = ressursQuery.data;

  if (ressursQuery.isLoading) {
    return <LoaderWithText />;
  }

  const gyldigFom = request.gyldigFom;

  return (
    <VStack gap="4">
      <BodyLong>
        <b>{formatVisningsnavn(ressurs?.visningsnavn)}</b> fra og med{" "}
        <b>{formatDisplayDate(gyldigFom)}</b>
      </BodyLong>

      <BodyLong>
        <b>{formatVisningsnavn(aktivtLederskap?.visningsnavn)}</b> til og med{" "}
        <b>
          {!!gyldigFom
            ? formatDisplayDate(subDays(new Date(gyldigFom), 1))
            : "dagen før"}
        </b>
      </BodyLong>
    </VStack>
  );
}

function KorrigerOrgEnhetLederKvitteringContent({
  orgEnhet,
  changeInfo,
}: {
  orgEnhet: OrgEnhet;
  changeInfo: any;
}) {
  const request = changeInfo.request;

  const navident = request.navident;

  const gyldigFom = request.gyldigFom;

  const aktivtLederskap = changeInfo.aktivtLederskap;

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(navident),
    queryFn: () => getRessurs.queryFn(navident),
    enabled: !!navident,
  });

  const ressurs = ressursQuery.data;

  if (ressursQuery.isLoading) {
    return <LoaderWithText />;
  }

  return (
    <VStack gap="4">
      <BodyLong>
        Leder oppdatert for <b>{orgEnhet.navn}</b>:
      </BodyLong>

      <BodyLong>
        <b>{formatVisningsnavn(ressurs?.visningsnavn)}</b> fra og med{" "}
        <b>{formatDisplayDate(gyldigFom)}</b>
      </BodyLong>

      <BodyLong>
        <b>{formatVisningsnavn(aktivtLederskap?.visningsnavn)}</b> til og med{" "}
        <b>
          {!!gyldigFom
            ? formatDisplayDate(subDays(new Date(gyldigFom), 1))
            : "dagen før"}
        </b>
      </BodyLong>
    </VStack>
  );
}

const validationSchema = yup.object({
  lederRessursNavident: yup.string().required("Obligatorisk"),
  gyldigFom: yup
    .string()
    .required("Obligatorisk")
    .test({
      name: "gyldigTom-ikke-før-aktivt-lederskap-startdato",
      message: "Kan ikke være før startdato",
      test: function (value) {
        const aktivtLederskapStart =
          this?.options?.context?.aktivtLederskapStart;

        if (
          !aktivtLederskapStart ||
          nomDateIsAfter(value, aktivtLederskapStart)
        ) {
          return true;
        }

        return this.createError({
          path: this.path,
          message: `Må være tidligst ${formatDisplayDate(addDays(aktivtLederskapStart, 1))}`,
        });
      },
    }),
});
