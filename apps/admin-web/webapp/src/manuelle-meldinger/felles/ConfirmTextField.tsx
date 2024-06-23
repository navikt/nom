import { TextField } from "@navikt/ds-react";
import { useFormContext } from "react-hook-form";
import { z } from "zod";

export function ConfirmTextField() {
  const { formState, register } = useFormContext<{ confirm: string }>();
  return (
    <TextField
      description="Dette er en potensielt farlig handling. Dobbeltsjekk at alt stemmer."
      error={formState.errors.confirm?.message}
      htmlSize={10}
      label="Skriv CONFIRM"
      {...register("confirm", {
        required: "Må fylles ut",
        validate: (v) => v === "CONFIRM",
      })}
    />
  );
}

export const confirmTextValidator = () =>
  z.string().regex(/CONFIRM/, "Ikke lik CONFIRM");
