import { DatePicker, useDatepicker } from "@navikt/ds-react";
import React from "react";
import { useController } from "react-hook-form";

import { OrgEnhetSearch } from "~/components/select/OrgEnhetSearch";
import { RessursSearch } from "~/components/select/RessursSearch";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";

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
}: {
  name: string;
  label: string;
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <RessursSearch
      label={label}
      onChange={field.onChange}
      error={fieldState.error}
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
