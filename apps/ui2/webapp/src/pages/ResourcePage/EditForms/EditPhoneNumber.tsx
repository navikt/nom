import { css } from "@emotion/react";
import { Button, TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import React from "react";
import { useForm } from "react-hook-form";

import { showToast } from "~/hooks/showToast";
import type { Ressurs } from "~/types/api-models";

type EditNameFormTypes = {
  telefonnummer: string;
};

export function EditPhoneNumber({ ressurs }: { ressurs: Ressurs }) {
  const {
    handleSubmit,
    register,
    reset,
    formState: { isDirty },
  } = useForm({
    defaultValues: {
      telefonnummer: ressurs.telefoner[0]?.nummer ?? "",
    },
  });

  const { mutate } = useMutation<unknown, unknown, EditNameFormTypes>({
    mutationFn: async (values) => alert(values),
    onSettled: () => {
      showToast({ text: "Lagret" });
      reset({}, { keepValues: true });
    },
  });

  return (
    <form
      css={css`
        display: flex;
        gap: var(--a-spacing-4);

        button {
          height: 3rem;
          align-self: flex-end;
        }
      `}
      onSubmit={handleSubmit((values) => mutate(values))}
    >
      <TextField {...register("telefonnummer")} hideLabel label="Telefon" />
      {isDirty && (
        <Button size="small" type="submit">
          Lagre
        </Button>
      )}
    </form>
  );
}
