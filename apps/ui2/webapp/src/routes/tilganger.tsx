import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Buildings3Icon, PlusCircleIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  Button,
  DatePicker,
  Detail,
  Heading,
  HGrid,
  HStack,
  Label,
  List,
  Loader,
  Table,
  Tag,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { isSameDay, subDays, subYears } from "date-fns";
import sortBy from "lodash/sortBy";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  addDelegering,
  fornyDelegering,
  getDelegerbareOrgEnheterForUserOptions,
  getOrgEnhet,
  getRessurserWhichCanBeDelegatedTo,
  getSession,
  getTilgangerDelegertForOrgEnhetOptions,
  getTilgangerDelegertTilMegOptions,
  tilgangerKeys,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { ApiResponseError } from "~/components/ApiResponseError";
import ContentWrapper from "~/components/ContentWrapper";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import { NavidentNameLookup } from "~/components/NavidentNameLookup";
import { PageBanner } from "~/components/PageBanner";
import { Pluralize } from "~/components/Pluralize";
import { DelegeringNavidentSelectFormPart } from "~/components/select/DelegeringNavidentSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  nomDateIsBefore,
  nomDateIsSameOrAfter,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { blueTopBorder } from "~/resources/NomStyles";
import type {
  LederTilgangForOrgenhetRequest,
  LederTilgangState,
  MinimalRessurs,
  OrgEnhet,
} from "~/types/api-models";
import {
  dateWithoutTimezone,
  formatDateWithoutTimezone,
  formatDisplayDate,
  isSameDateOrAfter,
  promiseIsFulfilled,
  promiseIsRejected,
  todayWithoutTimezone,
} from "~/utils";

export const Route = createFileRoute("/tilganger")({
  component: TilgangerPageWrapper,
  loader: async (options) => {
    const session =
      await options.context.queryClient.ensureQueryData(getSession);
    const navident = session.navident;
    const orgenheterLedetAvMegOgDelegerbart =
      await options.context.queryClient.ensureQueryData(
        getDelegerbareOrgEnheterForUserOptions(navident),
      );

    return { orgenheterLedetAvMegOgDelegerbart, navident };
  },
});

function TilgangerPageWrapper() {
  return <TilgangerPage />;
}

function TilgangerPage() {
  const hasAdminAccess = useHasAdminAccess();

  const { orgenheterLedetAvMegOgDelegerbart } = Route.useLoaderData();

  const delegerbareRessurserQuery = useQuery({
    queryKey: getRessurserWhichCanBeDelegatedTo.queryKey,
    queryFn: () => getRessurserWhichCanBeDelegatedTo.queryFn(),
  });

  const delegerbareRessurser = delegerbareRessurserQuery?.data;

  const delegertTilMeg =
    useQuery(getTilgangerDelegertTilMegOptions()).data?.sort((a, b) =>
      nomDateIsBefore(a.gyldigTom, b.gyldigTom) ? 1 : -1,
    ) ?? [];

  const filtrertDelegertTilMeg = hasAdminAccess
    ? delegertTilMeg
    : delegertTilMeg.filter((tilgang) =>
        nomDateIsSameOrAfter(
          tilgang.gyldigTom,
          dateWithoutTimezone(subYears(todayWithoutTimezone(), 2)),
        ),
      );

  function HarIkkeTilgangTilInnhold() {
    return (
      <List>
        <Heading
          css={css`
            margin-bottom: var(--a-spacing-2);
            margin-left: var(--a-spacing-2);
          `}
          level="2"
          size="small"
        >
          Følgende ledere kan delegere redigeringstilgang i NOM:
        </Heading>

        <List.Item>
          Ledere på driftsenhetsnivå (f.eks. Agder fylke, NKS styringsenhet,
          Arbeid og ytelser styringsenhet osv.)
        </List.Item>
        <List.Item>
          Ledere på arbeidsområdenivå (Bjerke (Nav-kontor), Familie- og
          pensjonsytelser Ålesund, HMS Buskerud osv.)
        </List.Item>
        <List.Item>
          Alle ledere på nivå mellom de to overnevnte nivåene
        </List.Item>
      </List>
    );
  }

  function HarTilgangState() {
    const brukerveiledningLenke: string =
      "https://navno.sharepoint.com/sites/intranett-it/SitePages/Brukerveiledninger-Mine-Tilganger.aspx";

    return (
      <VStack gap="6">
        {orgenheterLedetAvMegOgDelegerbart.length > 0 && (
          <>
            <Heading level="2" size="medium">
              Organisasjonsenheter du er leder for:
            </Heading>
            {orgenheterLedetAvMegOgDelegerbart.map((orgEnhet) => (
              <OrgEnhetCard
                delegerbareRessurser={delegerbareRessurser}
                isLoading={delegerbareRessurserQuery.isLoading}
                key={orgEnhet.id}
                orgEnhet={orgEnhet}
              />
            ))}
          </>
        )}

        {filtrertDelegertTilMeg.length > 0 && (
          <ContainerWithHighlightedTop gap={6} padding={9}>
            <Heading level="2" size="medium">
              Organisasjonsenheter du har fått redigeringstilgang til:
            </Heading>

            <BodyLong>
              For å få redigeringstilgang her i NOM, må du først ha fått
              skrivetilgang til NOM i Mine Tilganger. Brukerveiledning for Mine
              Tilganger finner du her:{" "}
              <Link target="_blank" to={brukerveiledningLenke}>
                Brukerveiledning Mine Tilganger
              </Link>
              .
            </BodyLong>

            {filtrertDelegertTilMeg.map((tilgang) => (
              <DelegertRolleCard key={tilgang.fid} tilgang={tilgang} />
            ))}
          </ContainerWithHighlightedTop>
        )}
      </VStack>
    );
  }

  return (
    <>
      <PageBanner text="Redigeringstilgang" />
      <ContentWrapper>
        {orgenheterLedetAvMegOgDelegerbart.length > 0 ||
        filtrertDelegertTilMeg.length > 0 ? (
          <HarTilgangState />
        ) : (
          <HarIkkeTilgangTilInnhold />
        )}
      </ContentWrapper>
    </>
  );
}

function DelegertRolleCard({ tilgang }: { tilgang: LederTilgangState }) {
  const orgEnhet = useQuery({
    queryKey: getOrgEnhet.queryKey(tilgang.orgenhetId),
    queryFn: () => getOrgEnhet.queryFn(tilgang.orgenhetId),
  }).data;

  return (
    <VStack
      css={css`
        padding: 2rem;
        border-radius: 5px;
        background: ${!tilgang.active ? "var(--a-surface-subtle)" : "#fff"};
        border: 1px solid
          ${
            !tilgang.active
              ? "var(--a-border-subtle)"
              : "var(--a-border-default)"
          };
      `}
      gap="6"
    >
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <VStack>
          <HStack gap="2">
            <Buildings3Icon fontSize="24" />
            <Heading
              level="3"
              size="small"
            >{`${orgEnhet?.navn} (${orgEnhet?.id})`}</Heading>
          </HStack>

          <Detail>
            <b>{orgEnhet?.nomNivaa ?? orgEnhet?.orgNiv}</b>
          </Detail>
        </VStack>

        {!tilgang.active && (
          <Tag
            css={css`
              height: fit-content;
            `}
            size="small"
            variant="warning"
          >
            Utløpt
          </Tag>
        )}
      </div>

      <HStack gap="4" key={tilgang.fid}>
        <span>
          Delegert fra:{" "}
          <b>
            <NavidentNameLookup navident={tilgang.av} />
          </b>
        </span>

        <span>
          Gyldig t.o.m.:{" "}
          <b>
            {tilgang.gyldigTom
              ? formatDisplayDate(tilgang.gyldigTom)
              : "Ingen utløpsdato"}
          </b>
        </span>
      </HStack>

      <BodyLong>
        Redigeringstilgangen gjelder for alle underliggende
        organisasjonsenheter.
      </BodyLong>
    </VStack>
  );
}

function OrgEnhetCard({
  orgEnhet,
  delegerbareRessurser,
  isLoading,
}: {
  orgEnhet: OrgEnhet;
  delegerbareRessurser: MinimalRessurs[] | undefined;
  isLoading: boolean;
}) {
  const [edit, setEdit] = useState(false);
  const queryClient = useQueryClient();

  const brukerveiledningLenke: string =
    "https://navno.sharepoint.com/sites/intranett-it/SitePages/Brukerveiledninger-Mine-Tilganger.aspx";

  const tilgangerDelegertAvMeg =
    useQuery({
      ...getTilgangerDelegertForOrgEnhetOptions(),
      select: (tilganger) => {
        const relevanteTilganger = tilganger.filter(
          (tilgang) => tilgang.orgenhetId === orgEnhet.id,
        );

        const aktiveTilganger = relevanteTilganger.filter(
          (tilgang) =>
            !tilgang.gyldigTom ||
            isSameDateOrAfter(new Date(tilgang.gyldigTom), new Date()),
        );

        return sortBy(aktiveTilganger, "gyldigTom");
      },
    }).data ?? [];

  const methods = useForm({
    shouldUnregister: true,
    resolver: yupResolver(tilgangSchema),
  });

  const { mutate, isPending, error } = useMutation<
    unknown,
    AxiosError<Error>,
    Record<string, { delegertTilNavident: string; gyldigTom: string }>
  >({
    mutationFn: async (values) => {
      const promises = Object.entries(values)
        .filter(([fid, formData]) => {
          const originalTilgang = tilgangerDelegertAvMeg.find(
            (tilgang) => tilgang.fid === fid,
          );

          // Send kun endre request der hvor gyldigTom forandret seg. For nye tilganger vil den alltid være endret (standard undefined)
          return !isSameDay(
            new Date(originalTilgang?.gyldigTom ?? ""),
            new Date(formData.gyldigTom),
          );
        })
        .map(([key, formData]) => {
          if (key === "newTilgang") {
            return addDelegering({
              ...formData,
              orgenhetId: orgEnhet.id,
            });
          } else {
            const fornyTilgangNavident = tilgangerDelegertAvMeg.find(
              (tilgang) => tilgang.fid === key,
            )?.til;

            return fornyDelegering({
              ...formData,
              delegertTilNavident: fornyTilgangNavident as string,
              orgenhetId: orgEnhet.id,
            });
          }
        });

      const settledPromises = await Promise.allSettled(promises);

      const fulfilled = settledPromises.filter(promiseIsFulfilled);

      const rejected = settledPromises.filter(promiseIsRejected);

      if (rejected.length > 0) {
        throw rejected[0].reason;
      }
      return fulfilled;
    },
    onSettled: () => {
      queryClient
        .invalidateQueries({
          queryKey: getTilgangerDelegertForOrgEnhetOptions().queryKey,
        })
        .then();
    },
    onSuccess: async () => {
      methods.reset();
      setEdit(false);
    },
  });

  const onCloseCard = () => {
    methods.reset();
    setEdit(false);
  };

  return (
    <FormProvider {...methods}>
      <VStack
        as="form"
        css={[
          blueTopBorder,
          css`
            padding: 2rem;
            background: white;
          `,
        ]}
        gap="6"
        onSubmit={methods.handleSubmit((values) => mutate(values))}
      >
        <Heading level="2" size="small">
          <HGrid columns={{ md: "max-content 1fr max-content" }} gap="2">
            <Buildings3Icon fontSize="24" />
            <span>{`${orgEnhet?.navn} (${orgEnhet?.id})`}</span>
            <Detail>{orgEnhet.nomNivaa ?? orgEnhet.orgNiv}</Detail>
          </HGrid>
        </Heading>
        <VStack gap="4">
          <Heading level="3" size="xsmall">
            Redigeringstilgang er delegert videre til{" "}
            {tilgangerDelegertAvMeg.length}{" "}
            <Pluralize
              numberOfElements={tilgangerDelegertAvMeg.length}
              pluralForm="personer"
              singleForm="person"
            />
          </Heading>

          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell
                  css={css`
                    width: 300px;
                  `}
                  scope="col"
                >
                  Navn
                </Table.HeaderCell>
                <Table.HeaderCell scope="col">
                  Gyldig til og med
                </Table.HeaderCell>
                <Table.HeaderCell
                  css={css`
                    width: 200px;
                  `}
                />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {tilgangerDelegertAvMeg.map((tilgang) =>
                edit ? (
                  <TilgangRowEditable key={tilgang.fid} tilgang={tilgang} />
                ) : (
                  <TilgangRowReadOnly key={tilgang.fid} tilgang={tilgang} />
                ),
              )}
              {edit ? (
                <AddTilgangRow
                  delegerbareRessurser={delegerbareRessurser}
                  isLoading={isLoading}
                />
              ) : undefined}
            </Table.Body>
          </Table>

          <HStack gap="2">
            <BodyLong>
              Redigeringstilgangen gjelder for alle underliggende
              organisasjonsenheter.
            </BodyLong>

            <BodyLong>
              For å delegere redigeringstilgang her i NOM, må medarbeideren
              først ha fått skrivetilgang til NOM i Mine Tilganger.
              Brukerveiledning for Mine Tilganger finner du her:{" "}
              <Link target="_blank" to={brukerveiledningLenke}>
                Brukerveiledning Mine Tilganger
              </Link>
              .
            </BodyLong>
          </HStack>
        </VStack>

        <ApiResponseError error={error} />

        {/*Normally you only use keys for arrays in react, but toggling buttons like this causes the form to submit even though it has type="button"*/}
        {/*This is a React peculiarity: https://github.com/facebook/react/issues/25089*/}
        <HStack
          css={css`
            align-self: flex-end;
          `}
          gap="2"
        >
          {edit ? (
            <>
              <Button
                key="end-edit"
                onClick={onCloseCard}
                size="small"
                type="button"
                variant="secondary"
              >
                Avbryt
              </Button>
              <Button
                key="submit-button"
                loading={isPending}
                size="small"
                type="submit"
                variant="primary"
              >
                Lagre endringer
              </Button>
            </>
          ) : (
            <Button
              key="start-edit"
              onClick={() => setEdit(true)}
              size="small"
              type="button"
              variant="secondary"
            >
              Rediger
            </Button>
          )}
        </HStack>
      </VStack>

      <ModalComponent />
    </FormProvider>
  );
}

function TilgangRowEditable({ tilgang }: { tilgang: LederTilgangState }) {
  const queryClient = useQueryClient();
  const { field, fieldState } = useController({
    name: `${tilgang.fid}.gyldigTom`,
    defaultValue: tilgang.gyldigTom,
  });

  const gyldigTomDatePicker = useDatepicker({
    defaultSelected: tilgang.gyldigTom
      ? new Date(tilgang.gyldigTom)
      : undefined,
    onDateChange: (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : "");
    },
  });

  const slettTilgangMutation = useMutation<
    unknown,
    unknown,
    LederTilgangForOrgenhetRequest
  >({
    mutationFn: async (values) => fornyDelegering(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tilgangerKeys.me }).then();
    },
  });

  return (
    <Table.Row>
      <Table.DataCell>
        <NavidentNameLookup navident={tilgang.til} />
      </Table.DataCell>
      <Table.DataCell align="right">
        <div
          css={css`
            display: flex;
          `}
        >
          <DatePicker {...gyldigTomDatePicker.datepickerProps}>
            <DatePicker.Input
              css={css`
                max-width: 158px;
              `}
              {...gyldigTomDatePicker.inputProps}
              error={fieldState.error?.message?.toString()}
              hideLabel
              label="Gyldig til og med"
              ref={field.ref}
              size="small"
            />
          </DatePicker>
        </div>
      </Table.DataCell>
      <Table.DataCell align="right">
        <Button
          loading={slettTilgangMutation.isPending}
          onClick={() =>
            slettTilgangMutation.mutate({
              delegertTilNavident: tilgang.til,
              gyldigTom: formatDateWithoutTimezone(subDays(new Date(), 1)), // Sette dato til igår anses som å slette tilgang
              orgenhetId: tilgang.orgenhetId,
            })
          }
          size="small"
          type="button"
          variant="secondary"
        >
          Avslutt
        </Button>
      </Table.DataCell>
    </Table.Row>
  );
}

function TilgangRowReadOnly({ tilgang }: { tilgang: LederTilgangState }) {
  return (
    <Table.Row>
      <Table.DataCell>
        <NavidentNameLookup navident={tilgang.til} />
      </Table.DataCell>
      <Table.DataCell>
        <b>
          {tilgang.gyldigTom === null
            ? "Ingen utløpsdato"
            : formatDisplayDate(tilgang.gyldigTom)}
        </b>
      </Table.DataCell>
      <Table.DataCell />
    </Table.Row>
  );
}

function AddTilgangRow({
  delegerbareRessurser,
  isLoading,
}: {
  delegerbareRessurser: MinimalRessurs[] | undefined;
  isLoading: boolean;
}) {
  const [isOpen, setIsOpen] = useState(false);

  if (!isOpen) {
    return (
      <Table.Row>
        <Table.DataCell colSpan={3}>
          <Button
            icon={<PlusCircleIcon />}
            onClick={() => setIsOpen(true)}
            size="small"
            variant="secondary"
          >
            Legg til ny tilgang
          </Button>
        </Table.DataCell>
      </Table.Row>
    );
  }

  return (
    <ActiveAddTilgangRow
      delegerbareRessurser={delegerbareRessurser}
      isLoading={isLoading}
      onClose={() => setIsOpen(false)}
    />
  );
}

function ActiveAddTilgangRow({
  onClose,
  delegerbareRessurser,
  isLoading,
}: {
  onClose: () => void;
  delegerbareRessurser: MinimalRessurs[] | undefined;
  isLoading: boolean;
}) {
  if (!delegerbareRessurser) {
    return isLoading ? (
      <Table.Row>
        <Table.DataCell>
          <div
            css={css`
              padding-left: var(--a-spacing-2);
              padding-top: var(--a-spacing-3);
            `}
          >
            <Loader />
          </div>
        </Table.DataCell>
        <Table.DataCell />
        <Table.DataCell />
      </Table.Row>
    ) : (
      <Label size="small">Det oppstod en feil</Label>
    );
  }

  const { field, fieldState } = useController({
    name: "newTilgang.gyldigTom",
  });

  const gyldigTomDatePicker = useDatepicker({
    onDateChange: (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : "");
    },
  });

  return (
    <Table.Row
      css={css`
        td {
          vertical-align: top;
        }
      `}
    >
      <Table.DataCell>
        <DelegeringNavidentSelectFormPart
          delegerbareRessurser={delegerbareRessurser}
          name="newTilgang.delegertTilNavident"
        />
      </Table.DataCell>
      <Table.DataCell>
        <div
          css={css`
            display: flex;
          `}
        >
          <DatePicker {...gyldigTomDatePicker.datepickerProps}>
            <DatePicker.Input
              {...gyldigTomDatePicker.inputProps}
              css={css`
                max-width: 258px;
              `}
              error={fieldState.error?.message?.toString()}
              label="Gyldig til og med (valgfritt)"
              ref={field.ref}
              size="small"
            />
          </DatePicker>
        </div>
      </Table.DataCell>
      <Table.DataCell align="right">
        <Button onClick={onClose} size="small" variant="secondary">
          Fjern
        </Button>
      </Table.DataCell>
    </Table.Row>
  );
}

const tilgangSchema = yup.lazy((values) => {
  const dynamicKeysToSchema = Object.keys(values).map((key) => [
    key,
    key === "newTilgang" ? newTilgangSchema : renewTilgangSchema,
  ]);

  return yup.object().shape(Object.fromEntries(dynamicKeysToSchema));
});

const newTilgangSchema = yup.object({
  delegertTilNavident: yup.string().required("Må oppgis"),
  gyldigTom: yup.string().notRequired().futureOrPresent(),
});

const renewTilgangSchema = yup.object({
  gyldigTom: yup.string().notRequired().futureOrPresent(),
});
