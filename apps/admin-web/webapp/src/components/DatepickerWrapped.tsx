import type { DateInputProps } from "@navikt/ds-react";
import { DatePicker, useDatepicker } from "@navikt/ds-react";
import { useEffect } from "react";
import { useController } from "react-hook-form";

import { formatDateWithoutTimezone } from "~/utils.ts";

// eslint-disable-next-line @typescript-eslint/no-explicit-any -- usikker på hvordan rules skal types, og om det er såå viktig.
type DatePickerWrappedProps = { name: string; rules?: any } & DateInputProps;

export function DatePickerWrapped({
  name,
  rules = {},
  ...dateInputProps
}: DatePickerWrappedProps) {
  const { field, fieldState } = useController({
    name,
    rules,
  });

  useEffect(() => {
    if (!field.value) {
      datePickerProperties.reset();
    }
  }, [field.value]);

  const datePickerProperties = useDatepicker({
    onDateChange: (date) =>
      field.onChange(date ? formatDateWithoutTimezone(date) : null),
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        {...dateInputProps}
        ref={field.ref}
        {...datePickerProperties.inputProps}
      />
    </DatePicker>
  );
}
