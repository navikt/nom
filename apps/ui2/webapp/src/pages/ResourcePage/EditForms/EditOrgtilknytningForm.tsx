import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { DatePicker, Heading, Label, useDatepicker } from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React from "react";
import { useForm } from "react-hook-form";

import {
  getOrgEnhet,
  orgtilknytningerKeys,
  updateOrgtilknytningDato,
} from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { showToast } from "~/hooks/showToast";
import { eksternPeriodeValidationSchema } from "~/pages/CreateResourcePage/OrganisasjonstilknytningStep";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import { defaultFormStyle, fullwidthRow } from "~/resources/NomStyles";
import type {
  EndreOrgtilknytningDatoRequest,
  OrgtilknytningState,
  StandardResponse,
} from "~/types/api-models";
import { formatDateWithoutTimezone, formatVisningsnavn } from "~/utils";

export function EditOrgtilknytningForm({
  orgTilknytning,
}: {
  orgTilknytning: OrgtilknytningState;
}) {
  const queryClient = useQueryClient();
  const methods = useForm({
    defaultValues: {
      gyldigFom: orgTilknytning.gyldigFom,
      gyldigTom: orgTilknytning.gyldigTom,
    },
    // @ts-expect-error -- For some strange reason yupResolver requires defaultValues to be either both string, or both undefined, but not a mix of both, which is our case
    resolver: yupResolver(eksternPeriodeValidationSchema),
  });

  const {
    handleSubmit,
    setValue,
    formState: { errors },
  } = methods;

  const startDatepicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigFom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: new Date(orgTilknytning.gyldigFom),
  });

  const sluttDatepicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigTom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: orgTilknytning.gyldigTom
      ? new Date(orgTilknytning.gyldigTom)
      : undefined,
  });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgTilknytning.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgTilknytning.orgEnhetId),
  });

  const orgtilknytningDatoMutation = useMutation<
    unknown,
    AxiosError<StandardResponse>,
    EndreOrgtilknytningDatoRequest
  >({
    mutationFn: async (values) =>
      updateOrgtilknytningDato(orgTilknytning.fid, values),
    onSuccess: () => {
      showToast({ text: "Lagret" });
      queryClient.invalidateQueries({
        queryKey: orgtilknytningerKeys.navident(orgTilknytning.navident),
      });
      navigate({
        search: { editOrgtilknytning: undefined },
        params: (previous) => previous,
      });
    },
  });

  const orgEnhet = orgEnhetQuery.data;

  const navigate = useNavigate();

  if (!orgEnhet) {
    return <></>;
  }

  const orgEnhetLeder = orgEnhet.ledere[0];

  return (
    <div
      aria-label="datoendring"
      css={css`
        margin-bottom: var(--a-spacing-8);
        background-color: var(--a-surface-default);
        border-radius: var(--a-border-radius-large);
        border: 1px solid var(--a-border-default);
        padding: var(--a-spacing-3) var(--a-spacing-4);
      `}
    >
      <Heading
        css={css`
          margin-bottom: var(--a-spacing-1);
        `}
        level="3"
        size="medium"
      >
        {orgEnhet.navn}
      </Heading>
      <Label size="small">
        <span
          css={css`
            font-weight: var(--a-font-weight-regular);
          `}
        >
          Leder:{" "}
        </span>
        {formatVisningsnavn(orgEnhetLeder?.visningsnavn)}
      </Label>
      {orgEnhet.remedyEnhetId ? (
        <Label
          css={css`
            margin-left: 14px;
          `}
          size="small"
        >
          <span
            css={css`
              font-weight: var(--a-font-weight-regular);
            `}
          >
            Enhet i Remedy:{" "}
          </span>
          {orgEnhet.remedyEnhetId}
        </Label>
      ) : undefined}
      <form
        css={defaultFormStyle}
        onSubmit={handleSubmit((values) =>
          orgtilknytningDatoMutation.mutate(values),
        )}
      >
        <div
          css={css`
            ${fullwidthRow}
          `}
        >
          <FullWidthDatePickerWrapper>
            <DatePicker {...startDatepicker.datepickerProps}>
              <DatePicker.Input
                {...startDatepicker.inputProps}
                error={errors.gyldigFom?.message?.toString()}
                label="Første arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
          <FullWidthDatePickerWrapper>
            <DatePicker {...sluttDatepicker.datepickerProps}>
              <DatePicker.Input
                {...sluttDatepicker.inputProps}
                error={errors.gyldigTom?.message?.toString()}
                label="Siste arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
        </div>
        <ApiResponseError error={orgtilknytningDatoMutation.error} />
        <span>
          Sluttdato på dagens dato eller tidligere vil avslutte tilknytningen
          med én gang.
        </span>
        <SaveOrAbortButtons isLoading={orgtilknytningDatoMutation.isPending} />
      </form>
    </div>
  );
}
