import { css } from "@emotion/react";
import { Button, TextField } from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import React, { useEffect } from "react";
import { useForm } from "react-hook-form";

import type { HentRessursMedNavidentQuery } from "~/api/generated/graphql_generated";
import {
  getRessursByRest,
  updateNavnForRessurs,
} from "~/api/react-query-endpoints";
import { showToast } from "~/hooks/showToast";
import type { Visningsnavn } from "~/types/api-models";

type GraphQlRessurs = NonNullable<HentRessursMedNavidentQuery["ressurs"]>;

export function EditNameForm({ ressurs }: { ressurs: GraphQlRessurs }) {
  /**
   * Why fetch ressurs (again) when it is already passed to this component?
   * Because the passed ressurs is from graphql that only has fornavn (which is actually fornavn+mellomnavn concatenated from state) and etternavn.
   * When fetching with REST we get the actual RessursState entry in return which has the "true" name.
   * Can be removed when we no longer use graphql for nom-UI
   */
  const ressursStateQuery = useQuery({
    queryKey: getRessursByRest.queryKey(ressurs.navident),
    queryFn: () => getRessursByRest.queryFn(ressurs.navident),
  });

  const visningsnavnFromRessursState = ressursStateQuery.data?.visningsnavn;

  const defaultValues = {
    fornavn: visningsnavnFromRessursState?.fornavn ?? "",
    mellomnavn: visningsnavnFromRessursState?.mellomnavn ?? "",
    etternavn: visningsnavnFromRessursState?.etternavn ?? "",
  };

  const {
    handleSubmit,
    register,
    reset,
    formState: { isDirty },
  } = useForm({
    defaultValues,
  });

  useEffect(() => {
    if (ressursStateQuery.isSuccess) {
      // When the form is initialized the RessursState is most likely not fetched yet.
      // Therefore, we reset the form with the correct defaults when the query is completed
      reset(defaultValues);
    }
  }, [ressursStateQuery.isSuccess]);

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
