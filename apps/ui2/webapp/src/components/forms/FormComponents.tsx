import { DatePicker, useDatepicker } from "@navikt/ds-react";
import React from "react";
import { useController } from "react-hook-form";

import { OrgEnhetSearch } from "~/components/select/OrgEnhetSearch";
import { RessursSearch } from "~/components/select/RessursSearch";
import { formatDateWithoutTimezone } from "~/customUtils/dateUtils";
import type { Tilgangsenhet } from "~/types/api-models";
import { TilgangsenhetSelect } from "~/components/select/FysiskBrukerstedSelect";

export function FormDateWrapper({
  name,
  label,
}: {
  name: string;
  label: string;
}) {
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
        label={label}
      />
    </DatePicker>
  );
}

export function FormRessursSearchWrapper({
  name,
  label,
  visValgtRessursInfo = false,
}: {
  name: string;
  label: string;
  visValgtRessursInfo?: boolean;
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <RessursSearch
      label={label}
      onChange={field.onChange}
      error={fieldState.error}
      visValgtRessursInfo={visValgtRessursInfo}
    />
  );
}

export function FormOrgEnhetSearchWrapper({
  name,
  label,
}: {
  name: string;
  label: string;
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <OrgEnhetSearch
      label={label}
      onChange={field.onChange}
      error={fieldState.error}
    />
  );
}

export function FormTilgangsenhetSelectWrapper({
  name,
  tilgangsenheter,
}: {
  name: string;
  tilgangsenheter: Tilgangsenhet[];
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <TilgangsenhetSelect
      error={fieldState.error}
      label="Tilgangsenhet"
      tilgangsenheter={tilgangsenheter}
      value={field.value}
      onChange={field.onChange}
    />
  );
}
