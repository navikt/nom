import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  DatePicker,
  Heading,
  Label,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import React from "react";
import { useForm } from "react-hook-form";

import {
  getOrgEnhet,
  updateOrgtilknytningDato,
} from "~/api/react-query-endpoints";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { showToast } from "~/hooks/showToast";
import { rowStyles } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import type {
  EndreOrgtilknytningDatoRequest,
  OrgtilknytningState,
} from "~/types/api-models";
import { formatDateWithoutTimezone, formatVisningsnavn } from "~/utils";

export function EditOrgtilknytningForm({
  orgTilknytning,
}: {
  orgTilknytning: OrgtilknytningState;
}) {
  const methods = useForm({
    defaultValues: {
      gyldigFom: orgTilknytning.gyldigFom,
      gyldigTom: orgTilknytning.gyldigTom,
    },
  });

  const { handleSubmit, setValue } = methods;

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
    unknown,
    EndreOrgtilknytningDatoRequest
  >({
    mutationFn: async (values) =>
      updateOrgtilknytningDato(orgTilknytning.fid, values),
    onSuccess: () => {
      showToast({ text: "Lagret" });
      // TODO:
      // Sørge for at verdier lastes på nytt ved lagring
      //--- i toppen av komponenten
      // const queryClient = useQueryClient();
      //
      // --- i onSuccess på linja før navigate()
      // queryClient.invalidateQueries({ queryKey: orgtilknytningerKeys.navident(navident)});
      navigate({ search: { editOrgtilknytning: undefined } });
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
        onSubmit={handleSubmit((values) =>
          orgtilknytningDatoMutation.mutate(values),
        )}
      >
        <div
          css={css`
            display: flex;
            margin-top: var(--a-spacing-6);
            margin-bottom: var(--a-spacing-4);
            ${rowStyles}
          `}
        >
          <FullWidthDatePickerWrapper>
            <DatePicker {...startDatepicker.datepickerProps}>
              <DatePicker.Input
                {...startDatepicker.inputProps}
                label="Første arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
          <FullWidthDatePickerWrapper>
            <DatePicker {...sluttDatepicker.datepickerProps}>
              <DatePicker.Input
                {...sluttDatepicker.inputProps}
                label="Siste arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
        </div>
        {orgtilknytningDatoMutation.isError ? (
          <Alert variant="error">Noe gikk galt</Alert>
        ) : undefined}
        <span>
          Sluttdato på dagens dato eller tidligere vil avslutte tilknytningen
          med én gang.
        </span>
        <div
          css={css`
            display: flex;
            justify-content: flex-end;
            margin-top: var(--a-spacing-4);
          `}
        >
          <Button
            css={css`
              margin-right: var(--a-spacing-6);
            `}
            onClick={() =>
              navigate({ search: { editOrgtilknytning: undefined } })
            }
            size="small"
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
          <Button
            icon={<FloppydiskIcon aria-hidden />}
            size="small"
            type="submit"
            variant="primary"
          >
            Lagre
          </Button>
        </div>
      </form>
    </div>
  );
}
