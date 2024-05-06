import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import React from "react";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import { updateTelefonnummer } from "~/api/react-query-endpoints";
import { showToast } from "~/hooks/showToast";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import type { Ressurs } from "~/types/api-models";

const editPrimaryTelefonnummerValidationSchema = yup.object({
  primaryTelefonnummer: yup.string().length(8, "Må være 8 siffer"),
});

export function EditPhoneNumber({ ressurs }: { ressurs: Ressurs }) {
  const { handleSubmit, register, reset, formState } = useForm({
    defaultValues: {
      primaryTelefonnummer:
        ressurs.primaryTelefonnummer ?? ressurs.telefoner[0]?.nummer ?? "",
    },
    resolver: yupResolver(editPrimaryTelefonnummerValidationSchema),
  });

  const { mutate, isPending } = useMutation<
    unknown,
    unknown,
    yup.InferType<typeof editPrimaryTelefonnummerValidationSchema>
  >({
    mutationFn: async (values) =>
      updateTelefonnummer(ressurs.navident, {
        nummer: values.primaryTelefonnummer,
      }),
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
      <TextField
        {...register("primaryTelefonnummer")}
        autoComplete="off"
        error={formState.errors.primaryTelefonnummer?.message}
        hideLabel
        label="Telefon"
      />
      <SaveOrAbortButtons isLoading={isPending} />
    </form>
  );
}
