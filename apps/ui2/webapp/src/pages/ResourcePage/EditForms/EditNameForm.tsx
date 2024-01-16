import { css } from "@emotion/react";
import { TextField } from "@navikt/ds-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import React from "react";
import { useForm } from "react-hook-form";

import {
  ressurserKeys,
  updateNavnForRessurs,
} from "~/api/react-query-endpoints";
import { showToast } from "~/hooks/showToast";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import type { Ressurs, Visningsnavn } from "~/types/api-models";

export function EditNameForm({ ressurs }: { ressurs: Ressurs }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const defaultValues = {
    fornavn: ressurs.visningsnavn.fornavn,
    mellomnavn: ressurs.visningsnavn.mellomnavn,
    etternavn: ressurs.visningsnavn.etternavn,
  };

  const { handleSubmit, register } = useForm({
    defaultValues,
  });

  const { mutate, isPending } = useMutation<unknown, unknown, Visningsnavn>({
    mutationFn: async (values) =>
      updateNavnForRessurs(ressurs.navident, values),
    onSuccess: () => {
      showToast({ text: "Lagret" });
      queryClient.invalidateQueries({
        queryKey: ressurserKeys.id(ressurs.navident),
      });
      navigate({ search: {}, params: (previous) => previous });
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
      <SaveOrAbortButtons isLoading={isPending} />
    </form>
  );
}
