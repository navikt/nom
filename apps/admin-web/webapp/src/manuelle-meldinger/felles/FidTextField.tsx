import { TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { z } from "zod";

export function FidTextField() {
  const { formState, register } = useFormContext<{ fid: string }>();
  return (
    <TextField
      error={formState.errors.fid?.message}
      htmlSize={50}
      label="fid"
      {...register("fid", {
        required: "Må fylles ut",
        pattern: {
          value: /-[0-7][\dA-HJKMNP-TV-Z]{25}$/,
          message: "Dette ligner ikke en fid",
        },
      })}
    />
  );
}

export const fidValidator = () =>
  z.string().regex(/-[0-7][\dA-HJKMNP-TV-Z]{25}$/, "Dette ligner ikke en fid");
