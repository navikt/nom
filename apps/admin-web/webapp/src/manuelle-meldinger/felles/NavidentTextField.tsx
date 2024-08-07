import { TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { z } from "zod";

export function NavidentTextField() {
  const { formState, register } = useFormContext<{ navident: string }>();
  return (
    <TextField
      error={formState.errors.navident?.message}
      htmlSize={50}
      label="Navident"
      {...register("navident", {
        required: "Må fylles ut",
        pattern: {
          value: /^[A-Z]\d{6}$/,
          message: "Dette ligner ikke en navident",
        },
      })}
    />
  );
}

export const navidentValidator = () =>
  z.string().regex(/^[A-Z]\d{6}$/, "Dette ligner ikke en navident");
