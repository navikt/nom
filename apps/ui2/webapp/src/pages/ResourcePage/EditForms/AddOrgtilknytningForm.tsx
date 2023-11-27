import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { PencilIcon } from "@navikt/aksel-icons";
import { Alert, Button } from "@navikt/ds-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React from "react";
import { FormProvider, useForm } from "react-hook-form";

import {
  leggTilOrgtilknytning,
  orgtilknytningerKeys,
} from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import { showToast } from "~/hooks/showToast";
import {
  eksternValidationSchema,
  OrganisasjonstilknytningInnerForm,
} from "~/pages/CreateResourcePage/OrganisasjonstilknytningStep";
import { EditButtonWrapper } from "~/pages/ResourcePage/EditForms/EditButtonWrapper";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import { ressursRoute } from "~/reactRouterRoutes";
import { defaultFormStyle } from "~/resources/NomStyles";
import type { LeggTilOrgtilknytningRequest, Ressurs } from "~/types/api-models";
import type { StandardResponse } from "~/types/api-models";

export function AddOrgtilknytningForm({ ressurs }: { ressurs: Ressurs }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { addOrgtilknytning } = useSearch({ from: ressursRoute.id });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: undefined,
      startDato: undefined,
      sluttDato: undefined,
    },
    resolver: yupResolver(eksternValidationSchema),
  });

  const leggTilOrgtilknytningMutation = useMutation<
    unknown,
    AxiosError<StandardResponse>,
    LeggTilOrgtilknytningRequest
  >({
    mutationFn: async (values) => leggTilOrgtilknytning(values),
    onSuccess: () => {
      showToast({ text: "Lagret" });
      queryClient.invalidateQueries({
        queryKey: orgtilknytningerKeys.navident(ressurs.navident),
      });
      navigate({ search: {} });
    },
  });

  const { handleSubmit } = methods;

  if (addOrgtilknytning) {
    return (
      <FormProvider {...methods}>
        <form
          css={defaultFormStyle}
          onSubmit={handleSubmit((values) =>
            leggTilOrgtilknytningMutation.mutate({
              ...values,
              navident: ressurs.navident,
              gyldigFom: values.startDato,
              gyldigTom: values.sluttDato,
            }),
          )}
        >
          <OrganisasjonstilknytningInnerForm />
          <ApiResponseError error={leggTilOrgtilknytningMutation.error} />
          <SaveOrAbortButtons
            isLoading={leggTilOrgtilknytningMutation.isPending}
          />
        </form>
      </FormProvider>
    );
  }

  return (
    <>
      {leggTilOrgtilknytningMutation.isSuccess && (
        <Alert
          closeButton
          onClose={() => leggTilOrgtilknytningMutation.reset()}
          title="Fjerne? "
          variant="info"
        >
          Du har lagt til en ny tilknytning, vil du fjerne en gammel? Ekspander
          tilknytningen og trykk på endre.
        </Alert>
      )}
      <EditButtonWrapper ressurs={ressurs}>
        <Button
          css={css`
            width: fit-content;
            align-self: flex-end;
            margin-bottom: var(--a-spacing-4);
          `}
          icon={<PencilIcon aria-hidden />}
          onClick={() => navigate({ search: { addOrgtilknytning: true } })}
          size="small"
          variant="secondary"
        >
          Legg til ny
        </Button>
      </EditButtonWrapper>
    </>
  );
}
