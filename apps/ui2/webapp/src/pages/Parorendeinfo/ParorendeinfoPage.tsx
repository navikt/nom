import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  ChevronLeftIcon,
  FloppydiskIcon,
  PencilIcon,
  PersonIcon,
  PlusCircleIcon,
  TrashIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  Heading,
  HGrid,
  Loader,
  Select,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams, useSearch } from "@tanstack/react-router";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React, { type ReactNode } from "react";
import {
  FormProvider,
  useController,
  useFieldArray,
  useForm,
  useFormContext,
} from "react-hook-form";
import * as yup from "yup";

import type { Kontaktinfo } from "~/api/react-query-endpoints";
import {
  createParorendeForBruker,
  deleteParorendeForBruker,
  getParorendeForNavident,
  getRessurs,
  oppdaterParorendeForBruker,
} from "~/api/react-query-endpoints";
import { useGetSession } from "~/api/useGetSession";
import { BekreftModal } from "~/components/BekreftModal";
import ContentWrapper from "~/components/ContentWrapper";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { PageBanner } from "~/components/PageBanner";
import { Pluralize } from "~/components/Pluralize";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { Route } from "~/routes/kontaktpersoner.$id";
import type { Ressurs } from "~/types/api-models";
import { formatTlf, formatVisningsnavn } from "~/utils";

const tomKontaktinfo: Kontaktinfo = {
  kontaktId: "",
  fornavn: "",
  etternavn: "",
  relasjon: "",
  telefon: "",
  prioritet: 1,
};

export const ParorendeinfoPage = () => {
  const { id } = useParams({ from: "/kontaktpersoner/$id" });

  const { editKontaktpersoner } = useSearch({ from: "/kontaktpersoner/$id" });

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(id as string),
    queryFn: () => getRessurs.queryFn(id as string),
    enabled: !!id,
  });

  const ressurs = ressursQuery.data;

  const kontaktinfoQuery = useQuery({
    queryKey: getParorendeForNavident.queryKey(id),
    queryFn: () => getParorendeForNavident.queryFn(id),
    enabled: !!id,
  });

  const kontaktinfo = kontaktinfoQuery.data ?? [];

  const sortedKontaktinfo = kontaktinfo
    .map((kontaktperson) => {
      return { ...kontaktperson, prioritet: kontaktperson.prioritet ?? 1 };
    })
    .sort((a, b) => a.prioritet - b.prioritet);

  const isLoading = kontaktinfoQuery.isLoading || ressursQuery.isLoading;

  const isError =
    kontaktinfoQuery.isError ||
    kontaktinfoQuery.status !== "success" ||
    ressursQuery.isError ||
    ressursQuery.status !== "success";

  if (isLoading) {
    return (
      <StaticContentWrapper ressurs={ressurs}>
        <div
          css={css`
            width: fit-content;
            margin: var(--a-spacing-8) auto auto;
          `}
        >
          <Loader size="3xlarge" />
        </div>
      </StaticContentWrapper>
    );
  }

  if (isError) {
    return (
      <StaticContentWrapper ressurs={ressurs}>
        <Alert
          css={css`
            margin-top: var(--a-spacing-8);
          `}
          variant="error"
        >
          <Heading level="3" size="small">
            Det oppstod en feil ved henting av kontaktpersoner
          </Heading>
        </Alert>
      </StaticContentWrapper>
    );
  }

  return (
    <StaticContentWrapper ressurs={ressurs}>
      {editKontaktpersoner ? (
        <ModalProvider>
          <ParorendeForm kontaktinfo={sortedKontaktinfo} navident={id} />

          <ModalComponent />
        </ModalProvider>
      ) : (
        <ParorendeList
          id={id}
          kontaktinformasjon={sortedKontaktinfo}
          ressurs={ressurs}
        />
      )}
    </StaticContentWrapper>
  );
};

function StaticContentWrapper({
  children,
  ressurs,
}: {
  children: ReactNode;
  ressurs: Ressurs | undefined;
}) {
  return (
    <>
      <PageBanner text="Kontaktpersoner" />

      <ContentWrapper>
        {ressurs && (
          <ReactRouterLink
            css={css`
              display: flex;
              align-items: center;
              color: #000;
              font-size: 1.2rem;
              margin-bottom: var(--a-spacing-4);
            `}
            params={{ id: ressurs?.navident }}
            to="/ressurs/$id"
          >
            <ChevronLeftIcon fontSize="1.8rem" />

            <b>Tilbake til {formatVisningsnavn(ressurs.visningsnavn)}</b>
          </ReactRouterLink>
        )}

        <BodyLong>
          Denne informasjonen kan leses av din nærmeste leder og leders leder.
          Informasjonen kan bli brukt dersom noe skulle tilstøte deg i
          arbeidstiden eller i en beredskapssituasjon, slik at en kontaktperson
          bør varsles. I dette tilfellet vil én eller flere av dine kontakter
          kunne bli kontaktet. Du kan ha opptil tre kontaktpersoner.
        </BodyLong>

        {children}
      </ContentWrapper>
    </>
  );
}

function ParorendeList({
  id,
  kontaktinformasjon,
  ressurs,
}: {
  id: string;
  kontaktinformasjon: Kontaktinfo[];
  ressurs: Ressurs | undefined;
}) {
  const navigate = useNavigate();

  const innloggetBruker = useGetSession().data?.navident === id;

  return (
    <VStack
      css={css`
        margin-top: var(--a-spacing-8);
        display: flex;
        justify-content: space-between;
      `}
      gap="4"
    >
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <Heading level="1" size="medium">
          {innloggetBruker
            ? "Dine kontaktpersoner:"
            : `Kontaktpersoner for ${ressurs ? formatVisningsnavn(ressurs.visningsnavn) : id}:`}
        </Heading>

        {innloggetBruker && (
          <Button
            css={css`
              height: fit-content;
              align-self: end;
              width: fit-content;
            `}
            icon={<PencilIcon title="a11y-title" />}
            onClick={() =>
              navigate({
                to: "/kontaktpersoner/$id",
                params: { id },
                search: { editKontaktpersoner: true },
              })
            }
            size="small"
            type="button"
            variant="secondary"
          >
            Administrer kontaktpersoner
          </Button>
        )}
      </div>

      <VStack gap="6">
        {kontaktinformasjon.map((kontaktinfo, index) => {
          return (
            <ContainerWithHighlightedTop
              gap={6}
              horizontalPadding={12}
              key={index}
              verticalPadding={8}
            >
              <div
                css={css`
                  display: flex;
                `}
              >
                <PersonIcon
                  css={css`
                    align-self: center;
                  `}
                  fontSize="1.5rem"
                  title="a11y-title"
                />

                <Heading
                  css={css`
                    margin-left: var(--a-spacing-2);
                  `}
                  level="2"
                  size="medium"
                >
                  {`${kontaktinfo.fornavn} ${kontaktinfo.etternavn}`}
                </Heading>
              </div>

              <div
                css={css`
                  display: flex;
                `}
              >
                <VStack gap="4">
                  {kontaktinfo.relasjon && <BodyShort>Relasjon:</BodyShort>}
                  {kontaktinfo.telefon && <BodyShort>Telefon:</BodyShort>}
                  {kontaktinfo.prioritet && <BodyShort>Prioritet:</BodyShort>}
                </VStack>

                <VStack
                  css={css`
                    margin-left: var(--a-spacing-4);
                  `}
                  gap="4"
                >
                  {kontaktinfo.relasjon && (
                    <BodyShort>
                      <b>
                        {RelasjonNameMapping[kontaktinfo.relasjon as Relasjon]}
                      </b>
                    </BodyShort>
                  )}
                  {kontaktinfo.telefon && (
                    <BodyShort>
                      <b>
                        {kontaktinfo.telefon.length === 8
                          ? formatTlf(kontaktinfo.telefon)
                          : kontaktinfo.telefon}
                      </b>
                    </BodyShort>
                  )}
                  {kontaktinfo.prioritet && (
                    <BodyShort>
                      <b>{kontaktinfo.prioritet}</b>
                    </BodyShort>
                  )}
                </VStack>
              </div>
            </ContainerWithHighlightedTop>
          );
        })}
      </VStack>
    </VStack>
  );
}

function ParorendeForm({
  kontaktinfo,
  navident,
}: {
  kontaktinfo: Kontaktinfo[];
  navident: string;
}) {
  const eksisterendeKontaktinfo = kontaktinfo;

  const { showModal } = useModal();

  const navigate = useNavigate();

  const queryClient = useQueryClient();

  type FormValues = {
    kontaktinfo: Kontaktinfo[];
  };

  const methods = useForm<FormValues>({
    defaultValues: {
      kontaktinfo:
        eksisterendeKontaktinfo && eksisterendeKontaktinfo.length > 0
          ? eksisterendeKontaktinfo
          : [tomKontaktinfo],
    },
    // @ts-expect-error -- Type inferred fra resolver er typen etter submit der flere felter er required, men under utfylling av formet vil flere verdier kunne være undefined. Derfor bruker vi heller eksplisitt type til UseForm
    resolver: yupResolver(formValuesSchema),
  });

  const {
    register,
    formState: { errors },
    handleSubmit,
    control,
  } = methods;

  const { fields, append, remove } = useFieldArray({
    control: control,
    name: "kontaktinfo",
  });

  const opprettParorendeMutation = useMutation<
    unknown,
    AxiosError<Error>,
    Kontaktinfo
  >({
    mutationFn: async (kontakt) => {
      return createParorendeForBruker(kontakt);
    },
  });

  const oppdaterParorendeMutation = useMutation<
    unknown,
    AxiosError<Error>,
    Kontaktinfo
  >({
    mutationFn: async (kontakt) => {
      return oppdaterParorendeForBruker(kontakt);
    },
  });

  const deleteParorendeMutation = useMutation<
    unknown,
    AxiosError<Error>,
    string
  >({
    mutationFn: async (kontaktId) => {
      return deleteParorendeForBruker(kontaktId);
    },
  });

  const visBekreftelsesmodal = async (slettKontakter: Kontaktinfo[]) => {
    if (slettKontakter.length > 0) {
      await showModal(
        <BekreftModal
          content={
            <VStack gap="2">
              <BodyLong>
                Bekreft sletting av{" "}
                <Pluralize
                  numberOfElements={slettKontakter.length}
                  pluralForm="kontaktpersoner"
                  singleForm="kontaktperson"
                />
                :
              </BodyLong>
              {slettKontakter.map((kontaktinfo) => (
                <BodyLong key={kontaktinfo.kontaktId}>
                  <b>{`${kontaktinfo.fornavn} ${kontaktinfo.etternavn}`}</b>
                </BodyLong>
              ))}
            </VStack>
          }
          heading="Bekreft endringer"
        />,
      );
    }
  };

  const { id } = Route.useParams();

  async function onValid(data: { kontaktinfo?: Kontaktinfo[] }) {
    const opprettKontakter = data.kontaktinfo?.filter(
      (kontakt) => !kontakt.kontaktId,
    );

    const oppdaterKontakter = data.kontaktinfo?.filter(
      (kontakt) => !!kontakt.kontaktId,
    );

    const slettKontakter = eksisterendeKontaktinfo.filter(
      (kontakt) =>
        !oppdaterKontakter?.some(
          (oppdaterKontakt) => oppdaterKontakt.kontaktId === kontakt.kontaktId,
        ),
    );

    await visBekreftelsesmodal(slettKontakter).then(async () => {
      try {
        const deletePromises: Promise<unknown>[] = [];

        for (const kontakt of slettKontakter) {
          deletePromises.push(
            deleteParorendeMutation.mutateAsync(kontakt.kontaktId),
          );
        }

        await Promise.all(deletePromises);

        const mutationPromises: Promise<unknown>[] = [];

        for (const kontakt of oppdaterKontakter ?? []) {
          mutationPromises.push(oppdaterParorendeMutation.mutateAsync(kontakt));
        }

        for (const kontakt of opprettKontakter ?? []) {
          mutationPromises.push(opprettParorendeMutation.mutateAsync(kontakt));
        }

        await Promise.all(mutationPromises);

        showToast({ text: "Endringer lagret", variant: "success" });

        await navigate({
          to: "/kontaktpersoner/$id",
          params: { id },
          search: { editKontaktpersoner: undefined },
        });
      } catch {
        showToast({ text: "Noe gikk galt", variant: "error" });

        await navigate({
          to: "/kontaktpersoner/$id",
          params: { id },
          search: { editKontaktpersoner: undefined },
        });
      } finally {
        await queryClient.invalidateQueries({
          queryKey: ["paarorende", navident],
        });
      }
    });
  }

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onValid)}>
        <VStack
          css={css`
            margin-top: var(--a-spacing-8);
            display: flex;
            justify-content: space-between;
          `}
          gap="4"
        >
          <div
            css={css`
              display: flex;
              justify-content: space-between;
            `}
          >
            <Heading level="1" size="medium">
              Dine kontaktpersoner:
            </Heading>

            <div>
              {fields.length < 3 && (
                <Button
                  css={css`
                    float: right;
                  `}
                  icon={<PlusCircleIcon title="a11y-title" />}
                  onClick={() => append(tomKontaktinfo)}
                  size="small"
                  type="button"
                  variant="secondary"
                >
                  Legg til ny kontaktperson
                </Button>
              )}
            </div>
          </div>

          <VStack gap="4">
            {fields.map((field, index) => (
              <ContainerWithHighlightedTop
                horizontalPadding={10}
                key={index}
                verticalPadding={6}
              >
                <div
                  css={css`
                    display: flex;
                  `}
                >
                  <HGrid
                    columns={2}
                    css={css`
                      width: 100%;
                    `}
                    gap="4"
                  >
                    <TextField
                      {...register(`kontaktinfo.${index}.fornavn`)}
                      autoComplete="off"
                      error={
                        errors?.kontaktinfo?.[index]?.fornavn?.message ?? ""
                      }
                      label="Fornavn"
                    />
                    <TextField
                      {...register(`kontaktinfo.${index}.etternavn`)}
                      autoComplete="off"
                      error={
                        errors?.kontaktinfo?.[index]?.etternavn?.message ?? ""
                      }
                      label="Etternavn"
                    />
                    <RelasjonFormPart
                      error={
                        errors?.kontaktinfo?.[index]?.relasjon?.message ?? ""
                      }
                      name={`kontaktinfo.${index}.relasjon`}
                    />
                    <TextField
                      {...register(`kontaktinfo.${index}.telefon`)}
                      autoComplete="off"
                      error={
                        errors?.kontaktinfo?.[index]?.telefon?.message ?? ""
                      }
                      label="Telefon"
                    />
                    <PrioritetFormPart
                      error={
                        errors?.kontaktinfo?.[index]?.prioritet?.message ?? ""
                      }
                      name={`kontaktinfo.${index}.prioritet`}
                    />

                    <Button
                      css={css`
                        height: fit-content;
                        width: fit-content;
                        place-self: end end;
                      `}
                      icon={<TrashIcon title="a11y-title" />}
                      onClick={() => remove(index)}
                      size="small"
                      type="button"
                      variant="secondary"
                    >
                      Slett kontaktperson
                    </Button>
                  </HGrid>
                </div>
              </ContainerWithHighlightedTop>
            ))}

            <div>
              <Button
                css={css`
                  width: fit-content;
                  float: right;
                  margin-left: var(--a-spacing-4);
                `}
                icon={<FloppydiskIcon title="a11y-title" />}
                size="small"
                type="submit"
              >
                Lagre endringer
              </Button>

              <Button
                css={css`
                  width: fit-content;
                  float: right;
                `}
                onClick={async () =>
                  await showModal(
                    <BekreftModal
                      content="Er du sikker på at du vil forkaste endringene dine?"
                      heading="Forkast endringer"
                    />,
                  )
                    .then(() =>
                      navigate({
                        to: "/kontaktpersoner/$id",
                        params: { id },
                        search: { editKontaktpersoner: undefined },
                      }),
                    )
                    .catch(() => {})
                }
                size="small"
                type="button"
                variant="secondary"
              >
                Avbryt uten å lagre
              </Button>
            </div>
          </VStack>
        </VStack>
      </form>
    </FormProvider>
  );
}

function RelasjonFormPart({ name, error }: { name: string; error?: string }) {
  const { field } = useController({ name });

  const { watch } = useFormContext();

  return (
    <Select
      error={error}
      label="Relasjon"
      onChange={(value) => field.onChange(value)}
      ref={field.ref}
      value={watch(name)}
    >
      <option value="" />
      {Object.values(Relasjon).map((relasjon) => (
        <option key={relasjon} value={relasjon}>
          {RelasjonNameMapping[relasjon]}
        </option>
      ))}
    </Select>
  );
}

function PrioritetFormPart({ name, error }: { name: string; error?: string }) {
  const { field } = useController({ name });

  const { watch } = useFormContext();

  return (
    <Select
      css={css`
        width: 70px;
      `}
      error={error}
      label="Prioritet"
      onChange={(value) => field.onChange(value)}
      ref={field.ref}
      value={watch(name)}
    >
      <option value={1}>1</option>
      <option value={2}>2</option>
      <option value={3}>3</option>
    </Select>
  );
}

const kontaktinfoSchema = yup.object({
  fornavn: yup.string().required("Obligatorisk"),
  etternavn: yup.string().required("Obligatorisk"),
  relasjon: yup.string().required("Obligatorisk"),
  telefon: yup.string().required("Obligatorisk"),
});

const formValuesSchema = yup.object().shape({
  kontaktinfo: yup.array().of(kontaktinfoSchema).required(),
});

export enum Relasjon {
  EKTEFELLE = "EKTEFELLE",
  SAMBOER = "SAMBOER",
  BARN = "BARN",
  SOSKEN = "SOSKEN",
  FORELDER = "FORELDER",
  ANNET = "ANNET",
}

export const RelasjonNameMapping = {
  [Relasjon.EKTEFELLE]: "Ektefelle/Partner",
  [Relasjon.SAMBOER]: "Samboer",
  [Relasjon.BARN]: "Barn",
  [Relasjon.SOSKEN]: "Søsken",
  [Relasjon.FORELDER]: "Forelder",
  [Relasjon.ANNET]: "Annet",
};
