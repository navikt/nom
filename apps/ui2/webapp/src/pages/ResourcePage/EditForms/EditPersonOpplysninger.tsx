import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  Button,
  Heading,
  HelpText,
  HStack,
  Label,
  TextField,
  VStack,
} from "@navikt/ds-react";
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
import { BekreftModal } from "~/components/BekreftModal";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import { useModal } from "~/components/modalProvider/ModalProvider";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { UserImage } from "~/components/UserImage";
import { showToast } from "~/hooks/showToast";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import {
  DagligLeder,
  Epost,
  FysiskBrukersted,
  KortFortalt,
  Navident,
  SektorView,
  SluttDato,
  StartDato,
  TelefonNummere,
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
      queryClient
        .invalidateQueries({
          queryKey: ressurserKeys.id(ressurs.navident),
        })
        .then();
      navigate({
        to: "/ressurs/$id",
        search: { editPersonopplysninger: undefined },
        params: {
          id: ressurs?.navident,
        },
      }).then();
    },
  });

  const currentPrimaryFysiskBrukersted = useFysiskBrukersted(
    ressurs.primaryFysiskBrukersted,
  );

  const { showModal } = useModal();

  const onValid = async (values: {
    primaryTelefonnummer?: string | undefined;
    mellomnavn?: string | undefined;
    etternavn: string;
    fornavn: string;
    primaryFysiskBrukersted: string;
  }) => {
    const endretNavn =
      values.fornavn !== ressurs.visningsnavn.fornavn ||
      (values.mellomnavn ?? "") !== (ressurs.visningsnavn.mellomnavn ?? "") ||
      values.etternavn !== ressurs.visningsnavn.etternavn;

    if (endretNavn) {
      await showModal(
        <BekreftModal
          content={
            <>
              Du har endret medarbeiderens navn til{" "}
              <b>
                {formatVisningsnavn({
                  fornavn: values.fornavn,
                  mellomnavn: values.mellomnavn,
                  etternavn: values.etternavn,
                })}
              </b>
              . Det nye visningsnavnet vil brukes i de fleste av Navs
              applikasjoner. Det vil også genereres en ny e-postadresse som
              gjenspeiler det nye navnet. Vennligst bekreft endringen.
            </>
          }
          heading="Bekreft navneendring"
        />,
      ).then(() => {
        mutate(values);
      });
    } else {
      mutate(values);
    }
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
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
                Visningsnavnet vil bli brukt i alle Navs applikasjoner, og
                fornavn og etternavn er obligatorisk. Navnet skal som hovedregel
                være samme som folkeregistrert navn, men man kan om ønskelig
                fjerne mellomnavn. Endring av visningsnavn vil innebære at det
                vil genereres en ny e-postadresse for medarbeideren.
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
                error={methods.formState.errors.fornavn?.message}
                label="Fornavn"
              />
              <TextField
                autoComplete="off"
                {...methods.register("mellomnavn")}
                error={methods.formState.errors.mellomnavn?.message}
                label="Mellomnavn"
              />
              <TextField
                autoComplete="off"
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
            <TelefonNummere redigering ressurs={ressurs} />
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
            error={methods.formState.errors.primaryTelefonnummer?.message}
            label={
              <div
                css={css`
                  display: flex;
                `}
              >
                <Label>Telefonnummer</Label>

                <HelpText
                  css={css`
                    margin-left: var(--a-spacing-2);
                  `}
                  title="Regler for telefonnummer"
                >
                  Norsk telefonnummer kan legges inn som 8 siffer. Utenlandsk
                  nummer må starte med landkode, og ha maks 21 siffer. Nummer
                  kan fjernes ved å lagre med tomt felt.
                </HelpText>
              </div>
            }
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
        {editPersonopplysninger && (
          <div
            css={css`
              display: flex;
              justify-content: flex-end;
              gap: var(--a-spacing-4);
            `}
          >
            <Button
              onClick={() =>
                navigate({
                  to: "/ressurs/$id",
                  search: { editPersonopplysninger: undefined },
                  params: {
                    id: ressurs?.navident,
                  },
                })
              }
              size="small"
              type="button"
              variant="secondary"
            >
              Avbryt
            </Button>

            <Button
              icon={<FloppydiskIcon aria-hidden />}
              loading={isPending}
              size="small"
              type="submit"
              variant="primary"
            >
              Lagre
            </Button>
          </div>
        )}

        <ModalComponent />
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
