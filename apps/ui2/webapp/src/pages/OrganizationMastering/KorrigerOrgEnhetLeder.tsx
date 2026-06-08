import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  Checkbox,
  DatePicker,
  Heading,
  HStack,
  Loader,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
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
  nomDateIsAfter,
  nomDateIsSameOrAfter,
  nomDateIsSameOrBefore,
  nomDateIsTodayOrAfter,
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
        orgEnhetId={orgEnhetId}
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

  const formaterteLedere =
    periodiserteLedere
      ?.filter((leder) => nomDateIsTodayOrAfter(leder.sluttdato))
      .sort((a, b) => (nomDateIsAfter(a.startdato, b.startdato) ? 1 : -1)) ??
    [];

  const navigate = useNavigate();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
    defaultValues: {
      fungerendeLeder: false,
    },
    shouldUnregister: true,
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
      gyldigTom: values.fungerendeLeder ? values.gyldigTom : undefined,
    };

    await showModal(
      <BekreftModal
        content={
          <LeggTilLederModalContent
            fungerendeLeder={values.fungerendeLeder}
            lederplan={formaterteLedere}
            request={request}
          />
        }
        heading="Bekreft lederendring"
      />,
    ).then(() => {
      opprettOrgEnhetLederMutation.mutate(request);
    });
  };

  const fungerendeLeder = methods.watch("fungerendeLeder");

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="8">
          <VStack gap="6">
            <FormRessursSearchWrapper
              label="Ny leder på organisasjonsenhet"
              name="lederRessursNavident"
            />

            <div
              css={css`
                display: flex;
                justify-content: space-between;
              `}
            >
              <VStack
                css={css`
                  min-width: 450px;
                `}
                gap="2"
              >
                <Checkbox {...methods.register("fungerendeLeder")}>
                  Midlertidig fungerende leder
                </Checkbox>

                <HStack gap="6">
                  <FormDateWrapper label="Leder fra" name="gyldigFom" />

                  {fungerendeLeder && (
                    <FormDateWrapper label="Leder til" name="gyldigTom" />
                  )}
                </HStack>
              </VStack>

              <Lederplan lederplan={formaterteLedere} />
            </div>
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
        </VStack>
      </form>
    </FormProvider>
  );
}

function LeggTilLederModalContent({
  request,
  lederplan,
  fungerendeLeder,
}: {
  request: OpprettOrgEnhetLederRequest;
  lederplan: OrgEnhetLederResponse[];
  fungerendeLeder: boolean;
}) {
  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(request?.navident ?? ""),
    queryFn: () => getRessurs.queryFn(request?.navident ?? ""),
    enabled: !!request.navident,
  });

  const ressurs = ressursQuery.data;

  const aktueltLederskap = lederplan.find(
    (lederskap) =>
      nomDateIsSameOrBefore(lederskap.startdato, request.gyldigFom) &&
      nomDateIsSameOrAfter(lederskap.sluttdato, request.gyldigFom),
  );

  if (ressursQuery.isLoading) {
    return <LoaderWithText />;
  }

  return (
    <VStack gap="4">
      <span>
        Endringen innebærer at{" "}
        <b>{formatVisningsnavn(ressurs?.visningsnavn)}</b> blir leder på
        organisasjonsenheten fra <b>{formatDisplayDate(request.gyldigFom)}</b>.
      </span>
      <span>
        Dette betyr at{" "}
        <b>
          {!!aktueltLederskap
            ? formatVisningsnavn(aktueltLederskap.visningsnavn)
            : "den tidligere lederen"}
        </b>{" "}
        vil være leder på organisasjonsenheten til og med dagen før dette.
      </span>
    </VStack>
  );
}

function Lederplan({ lederplan }: { lederplan: OrgEnhetLederResponse[] }) {
  return (
    <div
      css={css`
        width: fit-content;
        background-color: var(--nom-secondary-hover);
        border-radius: 8px;
        padding: var(--a-spacing-4);
        height: fit-content;
      `}
    >
      <VStack gap="2">
        <Heading level="2" size="small">
          Lederplan:
        </Heading>

        {lederplan?.map((lederperiode) => (
          <VStack key={lederperiode.fid}>
            <b>{formatVisningsnavn(lederperiode.visningsnavn)}:</b>

            <span>
              {`${formatDisplayDate(lederperiode.startdato)} - ${
                lederperiode.sluttdato
                  ? formatDisplayDate(lederperiode.sluttdato)
                  : "ubestemt tid"
              }`}
            </span>
          </VStack>
        ))}
      </VStack>
    </div>
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
  fungerendeLeder: yup.boolean().default(false),
  gyldigFom: yup.string().required("Obligatorisk"),
  gyldigTom: yup.string().when("fungerendeLeder", {
    is: true,
    then: (schema) => schema.required("Obligatorisk"),
    otherwise: (schema) => schema.notRequired(),
  }),
});
