import { useController } from "react-hook-form";
import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import React from "react";

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
