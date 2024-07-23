import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Buildings3Icon, PlusCircleIcon } from "@navikt/aksel-icons";
import {
  Button,
  DatePicker,
  Detail,
  Heading,
  HGrid,
  HStack,
  Table,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { isSameDay, subDays } from "date-fns";
import sortBy from "lodash/sortBy";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  addDelegering,
  fornyDelegering,
  getOrgEnhet,
  getOrgEnheterLedetAvOptions,
  getSession,
  getTilgangerDelegertByUserOptions,
  getTilgangerDelegertTilMegOptions,
  tilgangerKeys,
} from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import ContentWrapper from "~/components/ContentWrapper";
import { NavidentNameLookup } from "~/components/NavidentNameLookup";
import { PageBanner } from "~/components/PageBanner";
import { Pluralize } from "~/components/Pluralize";
import { NavidentSelectFormPart } from "~/components/select/NavidentSelect";
import { blueTopBorder } from "~/resources/NomStyles";
import type {
  LederTilgangForOrgenhetRequest,
  LederTilgangState,
  OrgEnhet,
} from "~/types/api-models";
import {
  formatDateWithoutTimezone,
  formatDisplayDate,
  isSameDateOrAfter,
  promiseIsFulfilled,
  promiseIsRejected,
} from "~/utils";

export const Route = createFileRoute("/tilganger")({
  component: TilgangerPage,
  loader: async (options) => {
    const session =
      await options.context.queryClient.ensureQueryData(getSession);
    const navident = session.navident;
    const orgenheterLedetAvMeg =
      await options.context.queryClient.ensureQueryData(
        getOrgEnheterLedetAvOptions(navident),
      );

    return { orgenheterLedetAvMeg, navident };
  },
});

function TilgangerPage() {
  const { orgenheterLedetAvMeg } = Route.useLoaderData();

  const delegertTilMeg =
    useQuery(getTilgangerDelegertTilMegOptions()).data ?? [];

  return (
    <>
      <PageBanner text="Redigeringstilgang" />
      <ContentWrapper>
        <VStack gap="6">
          {orgenheterLedetAvMeg.length > 0 && (
            <>
              <Heading level="1" size="medium">
                Organisasjonsenheter du er leder for:
              </Heading>
              {orgenheterLedetAvMeg.map((orgEnhet) => (
                <OrgEnhetCard key={orgEnhet.id} orgEnhet={orgEnhet} />
              ))}
            </>
          )}
          {delegertTilMeg.length > 0 && (
            <>
              <Heading level="1" size="medium">
                Organisasjonsenheter du har fått redigeringstilgang til:
              </Heading>
              {delegertTilMeg.map((tilgang) => (
                <DelegertRolleCard key={tilgang.fid} tilgang={tilgang} />
              ))}
            </>
          )}
        </VStack>
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
      css={[
        blueTopBorder,
        css`
          padding: 2rem;
          background: white;
        `,
      ]}
      gap="6"
    >
      <Heading level="2" size="small">
        <HGrid columns={{ md: "max-content 1fr max-content" }} gap="2">
          <Buildings3Icon fontSize="24" />
          <span>{orgEnhet?.navn}</span>
          <Detail>{orgEnhet?.nomNivaa ?? orgEnhet?.orgNiv}</Detail>
        </HGrid>
      </Heading>
      <HStack gap="4" key={tilgang.fid}>
        <span>Delegert fra:</span>
        <b>
          <NavidentNameLookup navident={tilgang.av} />
        </b>
        <span>Gyldig t.o.m</span>
        <b>{formatDisplayDate(tilgang.gyldigTom)}</b>
      </HStack>
      <span>
        Redigeringstilgangen gjelder for alle underliggende organisasjonsenheter
      </span>
    </VStack>
  );
}

function OrgEnhetCard({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const [edit, setEdit] = useState(false);
  const queryClient = useQueryClient();

  const tilgangerDelegertAvMeg =
    useQuery({
      ...getTilgangerDelegertByUserOptions(),
      select: (tilganger) => {
        const relevanteTilganger = tilganger.filter(
          (tilgang) => tilgang.orgenhetId === orgEnhet.id,
        );
        const aktiveTilganger = relevanteTilganger.filter((tilgang) =>
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
      // eslint-disable-next-line unicorn/no-array-callback-reference -- when using typeguard the function must be done this way
      const fulfilled = settledPromises.filter(promiseIsFulfilled);
      // eslint-disable-next-line unicorn/no-array-callback-reference
      const rejected = settledPromises.filter(promiseIsRejected);

      if (rejected.length > 0) {
        throw rejected[0].reason;
      }
      return fulfilled;
    },
    onSettled: () => {
      queryClient.invalidateQueries({
        queryKey: getTilgangerDelegertByUserOptions().queryKey,
      });
    },
    onSuccess: () => {
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
            <span>{orgEnhet.navn}</span>
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
              {edit ? <AddTilgangRow /> : undefined}
            </Table.Body>
          </Table>
          <span>
            Redigeringstilgangen gjelder for alle underliggende
            organisasjonsenheter
          </span>
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
                data-cy="avbryt-tilganger"
                key="end-edit"
                onClick={onCloseCard}
                size="small"
                type="button"
                variant="secondary"
              >
                Avbryt
              </Button>
              <Button
                data-cy="submit-tilganger"
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
              data-cy="rediger-tilganger"
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
    defaultSelected: new Date(tilgang.gyldigTom),
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
      queryClient.invalidateQueries({ queryKey: tilgangerKeys.me });
    },
  });

  return (
    <Table.Row>
      <Table.DataCell>
        <NavidentNameLookup navident={tilgang.til} />
      </Table.DataCell>
      <Table.DataCell align="right">
        <DatePicker {...gyldigTomDatePicker.datepickerProps}>
          <DatePicker.Input
            data-cy="gyldig-tom-datepicker"
            {...gyldigTomDatePicker.inputProps}
            error={fieldState.error?.message?.toString()}
            hideLabel
            label="Gyldig til og med"
            ref={field.ref}
            size="small"
          />
        </DatePicker>
      </Table.DataCell>
      <Table.DataCell align="right">
        <Button
          data-cy="slett-tilgang-button"
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
          Slett
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
        <b>{formatDisplayDate(tilgang.gyldigTom)}</b>
      </Table.DataCell>
      <Table.DataCell />
    </Table.Row>
  );
}

function AddTilgangRow() {
  const [isOpen, setIsOpen] = useState(false);

  if (!isOpen) {
    return (
      <Table.Row>
        <Table.DataCell colSpan={3}>
          <Button
            data-cy="legg-til-tilgang-button"
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

  return <ActiveAddTilgangRow onClose={() => setIsOpen(false)} />;
}

function ActiveAddTilgangRow({ onClose }: { onClose: () => void }) {
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
        <NavidentSelectFormPart
          label="Søk opp medarbeider"
          name="newTilgang.delegertTilNavident"
        />
      </Table.DataCell>
      <Table.DataCell>
        <DatePicker {...gyldigTomDatePicker.datepickerProps}>
          <DatePicker.Input
            {...gyldigTomDatePicker.inputProps}
            data-cy="newTilgang-gyldigTom-datepicker"
            error={fieldState.error?.message?.toString()}
            label="Gyldig til og med"
            ref={field.ref}
            size="small"
          />
        </DatePicker>
      </Table.DataCell>
      <Table.DataCell align="right">
        <Button
          data-cy="fjern-ny-tilgang-button"
          onClick={onClose}
          size="small"
          variant="secondary"
        >
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
  gyldigTom: yup
    .string()
    .required("Må oppgis")
    .futureOrPresent()
    .max1YearInTheFuture(),
});

const renewTilgangSchema = yup.object({
  gyldigTom: yup
    .string()
    .required("Må oppgis")
    .futureOrPresent()
    .max1YearInTheFuture(),
});
