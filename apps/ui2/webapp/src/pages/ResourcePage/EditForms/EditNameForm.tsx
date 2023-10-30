import { css } from "@emotion/react";
import { Button, TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import React from "react";
import { useForm } from "react-hook-form";

import { updateNavnForRessurs } from "~/api/react-query-endpoints";
import { showToast } from "~/hooks/showToast";
import type { Ressurs, Visningsnavn } from "~/types/api-models";

export function EditNameForm({ ressurs }: { ressurs: Ressurs }) {
  const defaultValues = {
    fornavn: ressurs.visningsnavn.fornavn,
    mellomnavn: ressurs.visningsnavn.mellomnavn,
    etternavn: ressurs.visningsnavn.etternavn,
  };

  const {
    handleSubmit,
    register,
    reset,
    formState: { isDirty },
  } = useForm({
    defaultValues,
  });

  const { mutate } = useMutation<unknown, unknown, Visningsnavn>({
    mutationFn: async (values) =>
      updateNavnForRessurs(ressurs.navident, values),
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
        margin-bottom: var(--a-spacing-8);

        button {
          height: 3rem;
          align-self: flex-end;
        }
      `}
      onSubmit={handleSubmit((values) => mutate(values))}
    >
      <TextField {...register("fornavn")} label="Fornavn" />
      <TextField {...register("mellomnavn")} label="Mellomnavn" />
      <TextField {...register("etternavn")} label="Etternavn" />
      {isDirty && (
        <Button size="small" type="submit">
          Lagre
        </Button>
      )}
    </form>
  );
}
