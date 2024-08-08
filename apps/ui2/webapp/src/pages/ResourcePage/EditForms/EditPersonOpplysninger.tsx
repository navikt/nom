import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Heading, HStack, Label, TextField, VStack } from "@navikt/ds-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import intersection from "lodash/intersection";
import React from "react";
import { FormProvider, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  ressurserKeys,
  updateFysiskeBrukersteder,
  updateNavnForRessurs,
  updateTelefonnummer,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { UserImage } from "~/components/UserImage";
import { showToast } from "~/hooks/showToast";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { SaveOrAbortButtons } from "~/pages/ResourcePage/EditForms/SaveOrAbortButtons";
import {
  DagligLeder,
  Epost,
  FysiskBrukersted,
  KortFortalt,
  Navident,
  SektorView,
  SluttDato,
  StartDato,
} from "~/pages/ResourcePage/RessursComponents/PersonOpplysninger";
import { type Ressurs } from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function EditPersonOpplysninger({
  ressurs,
  hasSelfServiceAccess,
}: {
  ressurs: Ressurs;
  hasSelfServiceAccess: boolean;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { editPersonopplysninger } = useSearch({ from: "/ressurs/$id" });

  const defaultValues = {
    fornavn: ressurs.visningsnavn.fornavn,
    mellomnavn: ressurs.visningsnavn.mellomnavn ?? "",
    etternavn: ressurs.visningsnavn.etternavn,
    primaryTelefonnummer: ressurs.primaryTelefonnummer ?? "",
    primaryFysiskBrukersted: ressurs.primaryFysiskBrukersted ?? "",
  };

  const methods = useForm({
    defaultValues,
    resolver: yupResolver(validationSchema),
  });

  const isUserAdmin = useHasAdminAccess();

  const { mutate, isPending } = useMutation<
    unknown,
    unknown,
    yup.InferType<typeof validationSchema>
  >({
    mutationFn: async (values) => {
      let promises: Promise<unknown>[] = [];
      if (hasSelfServiceAccess && !isUserAdmin) {
        if (
          checkIfDirtyField(
            ["primaryTelefonnummer"],
            Object.keys(methods.formState.dirtyFields),
          )
        ) {
          const updateTelefonPromise = updateTelefonnummer(ressurs.navident, {
            nummer: values.primaryTelefonnummer?.replaceAll(" ", "") ?? "",
          });
          promises = [...promises, updateTelefonPromise];
        }
      } else {
        if (
          checkIfDirtyField(
            ["primaryTelefonnummer"],
            Object.keys(methods.formState.dirtyFields),
          )
        ) {
          const updateTelefonPromise = updateTelefonnummer(ressurs.navident, {
            nummer: values.primaryTelefonnummer?.replaceAll(" ", "") ?? "",
          });
          promises = [...promises, updateTelefonPromise];
        }

        if (
          checkIfDirtyField(
            ["primaryFysiskBrukersted"],
            Object.keys(methods.formState.dirtyFields),
          )
        ) {
          const updateBrukerstedPromise = updateFysiskeBrukersteder(
            ressurs.fid,
            {
              primaryFysiskBrukersted: values.primaryFysiskBrukersted,
              fysiskeBrukersteder: [values.primaryFysiskBrukersted],
            },
          );

          promises = [...promises, updateBrukerstedPromise];
        }

        if (
          checkIfDirtyField(
            ["fornavn", "mellomnavn", "etternavn"],
            Object.keys(methods.formState.dirtyFields),
          )
        ) {
          const { fornavn, mellomnavn, etternavn } = values;
          const updateNamePromise = updateNavnForRessurs(ressurs.navident, {
            fornavn,
            mellomnavn,
            etternavn,
          });

          promises = [...promises, updateNamePromise];
        }
      }
      await Promise.allSettled(promises);
    },
    onSettled: () => {
      showToast({ text: "Lagret" });
      queryClient.invalidateQueries({
        queryKey: ressurserKeys.id(ressurs.navident),
      });
      navigate({ search: {}, params: (previous) => previous });
    },
  });

  const currentPrimaryFysiskBrukersted = useFysiskBrukersted(
    ressurs.primaryFysiskBrukersted,
  );

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit((values) => mutate(values))}>
        {hasSelfServiceAccess && !isUserAdmin ? (
          <>
            <Heading level="1" size="large">
              {formatVisningsnavn(ressurs.visningsnavn)}
            </Heading>
          </>
        ) : (
          <>
            <VStack
              css={css`
                margin-bottom: var(--a-spacing-4);
              `}
            >
              <Label>Visningsnavn</Label>
              <span>
                Navnet under vil bli brukt i alle NAVs applikasjoner.
                Medarbeideren må ha minst ett fornavn og minst ett etternavn.
              </span>
            </VStack>
            <div
              css={css`
                display: flex;
                gap: var(--a-spacing-2);
                align-items: flex-start;

                > * {
                  flex: 1;
                }
              `}
            >
              <TextField
                autoComplete="off"
                {...methods.register("fornavn")}
                data-cy="fornavn-textfield"
                error={methods.formState.errors.fornavn?.message}
                label="Fornavn"
              />
              <TextField
                autoComplete="off"
                data-cy="mellomnavn-textfield"
                {...methods.register("mellomnavn")}
                error={methods.formState.errors.mellomnavn?.message}
                label="Mellomnavn"
              />
              <TextField
                autoComplete="off"
                data-cy="etternavn-textfield"
                {...methods.register("etternavn")}
                error={methods.formState.errors.etternavn?.message}
                label="Etternavn"
              />
            </div>
          </>
        )}
        <KortFortalt />
        <HStack>
          <div
            css={css`
              flex: 1;
              display: grid;
              grid-template-columns: max-content 1fr;
              gap: var(--a-spacing-2) var(--a-spacing-4);
            `}
          >
            <Navident ressurs={ressurs} />
            <SektorView ressurs={ressurs} />
            <StartDato ressurs={ressurs} />
            <SluttDato ressurs={ressurs} />
            <Epost ressurs={ressurs} />
            <DagligLeder ressurs={ressurs} />
          </div>
          <UserImage
            includeInstructionsWhenNotFound
            navident={ressurs.navident}
            navn={formatVisningsnavn(ressurs.visningsnavn)}
            size="126px"
          />
        </HStack>
        <VStack
          css={css`
            margin: var(--a-spacing-6) 0;
          `}
          gap="4"
        >
          <TextField
            {...methods.register("primaryTelefonnummer")}
            autoComplete="off"
            css={css`
              width: 270px;
            `}
            data-cy="telefon-textfield"
            error={methods.formState.errors.primaryTelefonnummer?.message}
            label="Telefon"
          />
          {hasSelfServiceAccess && !isUserAdmin ? (
            <FysiskBrukersted ressurs={ressurs} />
          ) : (
            <FysiskBrukerstedSelectFormPart
              defaultValue={currentPrimaryFysiskBrukersted}
              name="primaryFysiskBrukersted"
            />
          )}
        </VStack>
        {editPersonopplysninger ? (
          <SaveOrAbortButtons isLoading={isPending} />
        ) : undefined}
      </form>
    </FormProvider>
  );
}

function checkIfDirtyField(fieldNames: string[], dirtyFieldNames: string[]) {
  return intersection(fieldNames, dirtyFieldNames).length > 0;
}

const validationSchema = yup.object({
  fornavn: yup.string().required("Obligatorisk"),
  mellomnavn: yup.string().optional(),
  etternavn: yup
    .string()
    .required("Obligatorisk")
    .matches(/^\S+$/, "Etternavn kan ikke inneholde mellomrom"),
  primaryTelefonnummer: yup.string().optionalPhoneNumber(),
  primaryFysiskBrukersted: yup.string().required("Obligatorisk"),
});
