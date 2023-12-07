import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  BodyLong,
  Button,
  DatePicker,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React from "react";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import { avvikleRessurs, getRessurs } from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import ContentWrapper from "~/components/ContentWrapper";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { PageBanner } from "~/components/PageBanner";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import { ressursRoute, terminateRessursRoute } from "~/reactRouterRoutes";
import fjernRessursIcon from "~/resources/fjernRessursIcon.svg";
import type { AvvikleRessursRequest } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

export function TerminateResourcePage() {
  const { id } = useParams({ from: terminateRessursRoute.id });
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
        sluttDato: yup.string().required("Obligatorisk"),
      }),
    ),
  });

  const {
    handleSubmit,
    setValue,
    formState: { errors },
  } = methods;

  const sluttDatepicker = useDatepicker({
    onDateChange: (date) => {
      setValue("sluttDato", date ? formatDateWithoutTimezone(date) : "");
    },
  });

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
      navigate({ params: { id }, to: ressursRoute.id });
    },
  });

  return (
    <>
      <PageBanner text="Medarbeider slutter" />
      <ContentWrapper>
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
              <div
                css={css`
                  display: flex;
                  gap: var(--a-spacing-8);
                `}
              >
                <FullWidthDatePickerWrapper>
                  <DatePicker {...sluttDatepicker.datepickerProps}>
                    <DatePicker.Input
                      {...sluttDatepicker.inputProps}
                      error={errors.sluttDato?.message?.toString()}
                      label="Sluttdato (Siste arbeidsdag)"
                    />
                  </DatePicker>
                </FullWidthDatePickerWrapper>
                <Button
                  css={css`
                    margin-top: var(--a-spacing-10);
                    min-width: fit-content;
                    height: fit-content;
                  `}
                  onClick={() => {
                    sluttDatepicker.setSelected(new Date());
                  }}
                  size="small"
                  type="button"
                  variant="tertiary"
                >
                  Velg datoen i dag
                </Button>
              </div>
              <Alert variant="info">
                Husk at ved oppføring i Teamkatalogen, må medarbeideren også
                fjernes fra teamlisten der.
              </Alert>
              <ApiResponseError error={avvikleRessursMutation.error} />
              <div
                css={css`
                  display: flex;
                  justify-content: flex-end;
                  gap: var(--a-spacing-8);
                `}
              >
                <Button
                  onClick={() =>
                    navigate({ params: { id }, to: ressursRoute.id })
                  }
                  size="small"
                  type="button"
                  variant="secondary"
                >
                  Avbryt
                </Button>
                <Button
                  loading={avvikleRessursMutation.isPending}
                  size="small"
                  type="submit"
                >
                  {ressurs.sluttdato ? "Oppdater" : "Bekreft"} sluttdato
                </Button>
              </div>
            </ContainerWithHighlightedTop>
          )}
        </form>
      </ContentWrapper>
    </>
  );
}
