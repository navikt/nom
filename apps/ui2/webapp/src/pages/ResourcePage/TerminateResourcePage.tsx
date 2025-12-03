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
import { Link, useNavigate, useParams } from "@tanstack/react-router";
import type { AxiosError } from "axios";
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
import {
  dateIsSameOrBefore,
  dateIsTodayOrBefore,
  formatDateWithoutTimezone,
  todayWithoutTimezone,
} from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { nomDateIsAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import fjernRessursIcon from "~/resources/fjernRessursIcon.svg";
import type { AvvikleRessursRequest } from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import { formatDisplayDate, formatVisningsnavn } from "~/utils";

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
            "sluttDato-ikke-før-startDato",
            "Kan ikke være før startdato",
            (sluttDato) => {
              const ressursStartdato = ressurs?.startdato;

              return (
                !ressursStartdato || nomDateIsAfter(sluttDato, ressursStartdato)
              );
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

  const onValid = async (values: { sluttDato: string }) => {
    const nySluttdato = values.sluttDato;
    const ressursSluttdato = ressurs.sluttdato;

    await (!ressursSluttdato ||
    dateIsSameOrBefore(nySluttdato, ressursSluttdato)
      ? showModal(
          <BekreftModal
            confirmButtonLabel={"Bekreft endring"}
            content={
              dateIsTodayOrBefore(nySluttdato) ? (
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

  const nomSharepointLenke: string =
    "https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Organisasjonsmaster-(NOM).aspx";

  const lonnsskjemaLenke: string =
    "https://navno.sharepoint.com/sites/intranett-lonnsadministrasjon/SitePages/L%C3%B8nnsskjema.aspx";

  return (
    <>
      <PageBanner text="Medarbeider slutter" />
      <ContentWrapper>
        <FormProvider {...methods}>
          <form onSubmit={handleSubmit(onValid)}>
            {ressurs && (
              <ContainerWithHighlightedTop
                icon={fjernRessursIcon}
                title="Medarbeider slutter"
              >
                <RessursCard ressurs={ressurs} />
                <BodyLong>
                  Ved å sette en endelig sluttdato for en medarbeider i NOM vil
                  medarbeideren bli registrert som sluttet i Nav på angitt
                  sluttdato.
                </BodyLong>
                <SluttDatoPicker />
                <ApiResponseError error={avvikleRessursMutation.error} />
                {ressurs.sektor === Sektor.NAV_KOMMUNAL && (
                  <Alert variant="info">
                    Du er i ferd med å sette sluttdato på en kommunal
                    medarbeider. Hvis medarbeideren er{" "}
                    <Link to={nomSharepointLenke}>
                      leder eller har utvidede tilganger i Unit4
                    </Link>
                    , må du også melde inn{" "}
                    <Link to={lonnsskjemaLenke}>
                      via eksisterende rutiner til Unit4
                    </Link>{" "}
                    at vedkommende slutter.
                  </Alert>
                )}
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
              label="Sluttdato (siste dag iht. kontrakt)"
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
          som medarbeider i Nav, og alle tilganger for medarbeideren vil bli
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
            som medarbeider i Nav, og alle tilganger for medarbeideren vil bli
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
