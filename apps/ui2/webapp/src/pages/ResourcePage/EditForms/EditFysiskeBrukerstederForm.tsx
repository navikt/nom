import { css } from "@emotion/react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import React from "react";
import { FormProvider, useForm } from "react-hook-form";

import {
  ressurserKeys,
  updateFysiskeBrukersteder,
} from "~/api/react-query-endpoints";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { showToast } from "~/hooks/showToast";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import type {
  Ressurs,
  UpdateFysiskeBrukerstederRequest,
} from "~/types/api-models";

export function EditFysiskeBrukerstederForm({ ressurs }: { ressurs: Ressurs }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const currentPrimaryFysiskBrukersted = useFysiskBrukersted(
    ressurs.primaryFysiskBrukersted,
  );

  const defaultValues = {
    primaryFysiskBrukersted: ressurs.primaryFysiskBrukersted ?? "",
    fysiskeBrukersteder: ressurs.fysiskeBrukersteder ?? [],
  };

  const methods = useForm({ defaultValues });

  const { mutate, isPending } = useMutation<
    unknown,
    unknown,
    UpdateFysiskeBrukerstederRequest
  >({
    mutationFn: async (values) => {
      if (
        values.primaryFysiskBrukersted &&
        values.primaryFysiskBrukersted !== ressurs.primaryFysiskBrukersted
      ) {
        await updateFysiskeBrukersteder(ressurs.fid, {
          primaryFysiskBrukersted: values.primaryFysiskBrukersted,
          fysiskeBrukersteder: [values.primaryFysiskBrukersted],
        });
      }
    },
    onSuccess: () => {
      showToast({ text: "Lagret" });
      queryClient.invalidateQueries({
        queryKey: ressurserKeys.id(ressurs.navident),
      });
      navigate({ search: {}, params: (previous) => previous });
    },
  });

  return (
    <FormProvider {...methods}>
      <form
        css={css`
          display: grid;
          grid-template-columns: 350px min-content;
          grid-column: 1/-1;
          gap: var(--a-spacing-4);
          margin-bottom: var(--a-spacing-8);

          button {
            height: 3rem;
            align-self: flex-end;
          }
        `}
        onSubmit={methods.handleSubmit((values) => mutate(values))}
      >
        <FysiskBrukerstedSelectFormPart
          defaultValue={currentPrimaryFysiskBrukersted}
          name="primaryFysiskBrukersted"
        />
        <SaveOrAbortButtons isLoading={isPending} />
      </form>
    </FormProvider>
  );
}
