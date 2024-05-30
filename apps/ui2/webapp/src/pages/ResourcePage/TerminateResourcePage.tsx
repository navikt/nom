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
import ContentWrapper from "~/components/ContentWrapper";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { PageBanner } from "~/components/PageBanner";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import fjernRessursIcon from "~/resources/fjernRessursIcon.svg";
import type { AvvikleRessursRequest } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

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
      showToast({ text: "Avsluttet" });
      navigate({ params: { id }, to: "/ressurs/$id" });
    },
  });

  return (
    <>
      <PageBanner text="Medarbeider slutter" />
      <ContentWrapper>
        <FormProvider {...methods}>
          <form
            onSubmit={handleSubmit((values) =>
              avvikleRessursMutation.mutate(values),
            )}
          >
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
                <Alert variant="info">
                  Husk at ved oppføring i Teamkatalogen, må medarbeideren også
                  fjernes fra teamlisten der.
                </Alert>
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
    <HStack gap="8">
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
      <Button
        css={css`
          margin-top: var(--a-spacing-10);
          min-width: fit-content;
          height: fit-content;
        `}
        data-cy="velg-dagens-dato-button"
        onClick={() => {
          sluttDatoDatepicker.setSelected(new Date());
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
