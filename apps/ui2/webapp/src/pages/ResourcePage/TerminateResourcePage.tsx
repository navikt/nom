import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  HStack,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { isAfter } from "date-fns";
import { maxTime } from "date-fns/constants";
import React from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import { avvikleRessurs, getRessurs } from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import { useModal } from "~/components/modalProvider/ModalProvider";
import { PageBanner } from "~/components/PageBanner";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import fjernRessursIcon from "~/resources/fjernRessursIcon.svg";
import type { AvvikleRessursRequest } from "~/types/api-models";
import {
  dateWithoutTimezone,
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
  isSameDateOrBefore,
  isTodayOrBefore,
  todayWithoutTimezone,
} from "~/utils";

export function TerminateResourcePage() {
  const { id } = useParams({ from: "/ressurs-terminate/$id" });
  const navigate = useNavigate();

  const ressurs = useQuery({
    queryKey: getRessurs.queryKey(id),
    queryFn: () => getRessurs.queryFn(id),
  }).data;

  const methods = useForm({
    defaultValues: {
      sluttDato: "",
    },
    resolver: yupResolver(
      yup.object({
        sluttDato: yup
          .string()
          .max1YearInTheFuture()
          .required("Obligatorisk")
          .test(
            "sluttDato-etter-startDato",
            "Sluttdato kan ikke være før startdato",
            (sluttDato) => {
              return isAfter(sluttDato, ressurs?.startdato ?? maxTime);
            },
          ),
      }),
    ),
  });

  const { handleSubmit } = methods;

  const avvikleRessursMutation = useMutation<
    unknown,
    AxiosError<Error>,
    AvvikleRessursRequest
  >({
    mutationFn: async (values) => {
      if (ressurs) {
        return avvikleRessurs(ressurs.fid, values);
      }
    },
    onSuccess: () => {
      showToast({ text: "Oppdatert" });
      navigate({ params: { id }, to: "/ressurs/$id" }).then();
    },
  });

  const { showModal } = useModal();

  if (!ressurs) {
    return <></>;
  }

  const onSubmit = async (values: { sluttDato: string }) => {
    const nySluttdato = values.sluttDato;
    const ressursSluttdato = ressurs.sluttdato;

    await (!ressursSluttdato ||
    isSameDateOrBefore(
      dateWithoutTimezone(new Date(nySluttdato)),
      dateWithoutTimezone(new Date(ressursSluttdato)),
    )
      ? showModal(
          <BekreftModal
            confirmButtonLabel={"Bekreft endring"}
            content={
              isTodayOrBefore(new Date(nySluttdato)) ? (
                <ModalInnholdFortiden
                  navn={
                    formatVisningsnavn(ressurs?.visningsnavn) ?? "medarbeideren"
                  }
                />
              ) : (
                <ModalInnholdFremtiden
                  dato={formatDisplayDate(new Date(nySluttdato))}
                  navn={
                    formatVisningsnavn(ressurs?.visningsnavn) ?? "medarbeideren"
                  }
                />
              )
            }
            heading={"Bekreft endring"}
          />,
        ).then(() => {
          avvikleRessursMutation.mutate(values);
        })
      : showModal(
          <BekreftModal
            cancelButtonLabel={"Gå tilbake"}
            confirmButtonLabel={"Gå videre"}
            content={
              <span>
                Du har satt en sluttdato som er senere enn medarbeiderens
                nåværende sluttdato. For å gjøre dette må du angi hvor
                vedkommende jobber i den utvidede perioden. Trykk på{" "}
                <b>Gå videre</b> for å angi hvor medarbeideren jobber i
                perioden.
              </span>
            }
            heading={"Utsette sluttdato"}
          />,
        ).then(() => {
          navigate({
            to: `/ressurs/${ressurs.navident}`,
            search: { editOrgtilknytninger: true },
          });
        }));
  };

  return (
    <>
      <PageBanner text="Medarbeider slutter" />
      <ContentWrapper>
        <FormProvider {...methods}>
          <form onSubmit={handleSubmit(onSubmit)}>
            {ressurs && (
              <ContainerWithHighlightedTop
                icon={fjernRessursIcon}
                title="Medarbeider slutter"
              >
                <RessursCard ressurs={ressurs} />
                <BodyLong>
                  Ved å sette en endelig sluttdato for en medarbeider i NOM vil
                  medarbeideren bli registrert som sluttet i NAV på angitt
                  sluttdato.
                </BodyLong>
                <SluttDatoPicker />
                <ApiResponseError error={avvikleRessursMutation.error} />
                <HStack gap="8" justify="end">
                  <Button
                    onClick={() =>
                      navigate({ params: { id }, to: "/ressurs/$id" })
                    }
                    size="small"
                    type="button"
                    variant="secondary"
                  >
                    Avbryt
                  </Button>
                  <Button
                    data-cy="submit-sluttdato-button"
                    loading={avvikleRessursMutation.isPending}
                    size="small"
                    type="submit"
                  >
                    {ressurs.sluttdato ? "Oppdater" : "Bekreft"} sluttdato
                  </Button>
                </HStack>
              </ContainerWithHighlightedTop>
            )}

            <ModalComponent />
          </form>
        </FormProvider>
      </ContentWrapper>
    </>
  );
}

function SluttDatoPicker() {
  const { field, fieldState } = useController({
    name: "sluttDato",
  });

  const sluttDatoDatepicker = useDatepicker({
    onDateChange: (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : "");
    },
  });

  return (
    <HStack
      css={css`
        display: flex;
        justify-content: space-between;
      `}
    >
      <div
        css={css`
          display: flex;
          flex-grow: 1;
        `}
      >
        <FullWidthDatePickerWrapper>
          <DatePicker {...sluttDatoDatepicker.datepickerProps}>
            <DatePicker.Input
              data-cy="sluttDato-picker"
              {...sluttDatoDatepicker.inputProps}
              error={fieldState.error?.message?.toString()}
              label="Sluttdato (Siste arbeidsdag)"
              ref={field.ref}
            />
          </DatePicker>
        </FullWidthDatePickerWrapper>
      </div>

      <Button
        css={css`
          margin-left: var(--a-spacing-8);
          margin-top: var(--a-spacing-10);
          height: var(--a-spacing-8);
        `}
        data-cy="velg-dagens-dato-button"
        onClick={() => {
          sluttDatoDatepicker.setSelected(todayWithoutTimezone());
        }}
        size="small"
        type="button"
        variant="tertiary"
      >
        Velg datoen i dag
      </Button>
    </HStack>
  );
}

function ModalInnholdFremtiden({ navn, dato }: { navn: string; dato: string }) {
  return (
    <HStack gap="6">
      <BodyLong>
        Du er nå i ferd med å sette medarbeiderens sluttdato til <b>{dato}</b>.
      </BodyLong>
      <BodyLong>
        <b>
          Dette innebærer at etter {dato} vil {navn} ikke lenger være registrert
          som medarbeider i NAV, og alle tilganger for medarbeideren vil bli
          fjernet.
        </b>
      </BodyLong>
      <BodyLong>
        Hvis dette ikke er korrekt, må du trykke på <b>Avbryt</b>.
      </BodyLong>
      <BodyLong>
        Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
        <b>Bekreft endring</b>.
      </BodyLong>
    </HStack>
  );
}

function ModalInnholdFortiden({ navn }: { navn: string }) {
  return (
    <Alert variant="warning">
      <HStack gap="6">
        <BodyLong>
          Du er nå i ferd med å sette medarbeiderens sluttdato til dagens dato
          eller tidligere.
        </BodyLong>
        <BodyLong>
          <b>
            Dette innebærer at fra i dag vil {navn} ikke lenger være registrert
            som medarbeider i NAV, og alle tilganger for medarbeideren vil bli
            fjernet.
          </b>
        </BodyLong>
        <BodyLong>
          Hvis dette ikke er korrekt, må du trykke på <b>Avbryt</b>.
        </BodyLong>
        <BodyLong>
          Hvis dette er korrekt, kan du bekrefte endringen ved å trykke på{" "}
          <b>Bekreft endring</b>.
        </BodyLong>
      </HStack>
    </Alert>
  );
}
