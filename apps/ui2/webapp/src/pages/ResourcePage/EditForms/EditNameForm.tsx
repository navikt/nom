import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { TextField } from "@navikt/ds-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import React from "react";
import { useForm } from "react-hook-form";
import * as yup from "yup";

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

  const { handleSubmit, register, formState } = useForm({
    defaultValues,
    resolver: yupResolver(navnValidationSchema),
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
      <TextField
        {...register("fornavn")}
        error={formState.errors.fornavn?.message}
        label="Fornavn"
      />
      <TextField
        {...register("mellomnavn")}
        error={formState.errors.mellomnavn?.message}
        label="Mellomnavn"
      />
      <TextField
        {...register("etternavn")}
        error={formState.errors.etternavn?.message}
        label="Etternavn"
      />
      <SaveOrAbortButtons isLoading={isPending} />
    </form>
  );
}

const navnValidationSchema = yup.object({
  fornavn: yup.string().required("Obligatorisk"),
  mellomnavn: yup.string().optional(),
  etternavn: yup
    .string()
    .required("Obligatorisk")
    .matches(/^\S+$/, "Etternavn kan ikke inneholde mellomrom"),
});
