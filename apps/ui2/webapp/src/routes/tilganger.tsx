import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Buildings3Icon } from "@navikt/aksel-icons";
import {
  Button,
  DatePicker,
  Heading,
  HStack,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import React, { useState } from "react";
import { FormProvider, useForm, useFormContext } from "react-hook-form";
import * as yup from "yup";

import {
  addDelegering,
  fornyDelegering,
  getOrgEnhet,
  getOrgEnheterLedetAvOptions,
  getSession,
  getTilgangerDelegertByUserOptions,
  getTilgangerDelegertTilMegOptions,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { NavidentNameLookup } from "~/components/NavidentNameLookup";
import { PageBanner } from "~/components/PageBanner";
import { Pluralize } from "~/components/Pluralize";
import { NavidentSelectFormPart } from "~/components/select/NavidentSelect";
import { blueTopBorder } from "~/resources/NomStyles";
import type { LederTilgang, OrgEnhet } from "~/types/api-models";
import { formatDateWithoutTimezone, formatDisplayDate } from "~/utils";

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
  const { navident } = Route.useLoaderData();
  const orgenheterLedetAvMeg =
    useQuery(getOrgEnheterLedetAvOptions(navident)).data ?? [];

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

function DelegertRolleCard({ tilgang }: { tilgang: LederTilgang }) {
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
        <HStack align="center" gap="2">
          <Buildings3Icon fontSize="24" />
          {orgEnhet?.navn}
        </HStack>
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
      select: (tilganger) =>
        tilganger.filter((tilgang) => tilgang.orgenhetId === orgEnhet.id),
    }).data ?? [];

  const methods = useForm({
    resolver: yupResolver(tilgangSchema),
  });

  const { mutate, isPending } = useMutation<
    unknown,
    unknown,
    Record<string, { delegertTilNavident: string; gyldigTom: string }>
  >({
    mutationFn: async (values) => {
      const promises = Object.entries(values)
        .filter(
          ([, formData]) =>
            Object.values(formData).length > 0 &&
            !Object.values(formData).some((v) => !v),
        )
        .map(([key, formData]) => {
          return key === "newTilgang"
            ? addDelegering({
                ...formData,
                orgenhetId: orgEnhet.id,
              })
            : fornyDelegering({
                ...formData,
                delegertTilNavident: key,
                orgenhetId: orgEnhet.id,
              });
        });

      return Promise.allSettled(promises);
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
          <HStack align="center" gap="2">
            <Buildings3Icon fontSize="24" />
            {orgEnhet.navn}
          </HStack>
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
          {tilgangerDelegertAvMeg.map((tilgang) => (
            <Delegasjon edit={edit} key={tilgang.fid} tilgang={tilgang} />
          ))}
          <span>
            Redigeringstilgangen gjelder for alle underliggende
            organisasjonsenheter
          </span>
        </VStack>

        {edit ? <AddDelegationFields /> : undefined}
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
    </FormProvider>
  );
}

function Delegasjon({
  tilgang,
  edit,
}: {
  edit: boolean;
  tilgang: LederTilgang;
}) {
  const {
    setValue,
    formState: { errors },
  } = useFormContext();

  const gyldigTomDatePicker = useDatepicker({
    onDateChange: (date) => {
      setValue(
        `${tilgang.til}.gyldigTom`,
        date ? formatDateWithoutTimezone(date) : "",
      );
    },
  });

  return (
    <HStack gap="4" key={tilgang.fid}>
      <span>Delegert til:</span>
      <b>
        <NavidentNameLookup navident={tilgang.til} />
      </b>

      {edit ? (
        <DatePicker {...gyldigTomDatePicker.datepickerProps}>
          <DatePicker.Input
            {...gyldigTomDatePicker.inputProps}
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-expect-error
            error={errors[tilgang.til]?.gyldigTom?.message?.toString()}
            label="Gyldig til og med:"
            size="small"
          />
        </DatePicker>
      ) : (
        <>
          <span>Gyldig t.o.m</span>
          <b>{formatDisplayDate(tilgang.gyldigTom)}</b>
        </>
      )}
    </HStack>
  );
}

function AddDelegationFields() {
  const {
    setValue,
    formState: { errors },
  } = useFormContext();

  const gyldigTomDatePicker = useDatepicker({
    onDateChange: (date) => {
      setValue(
        `newTilgang.gyldigTom`,
        date ? formatDateWithoutTimezone(date) : "",
      );
    },
  });

  return (
    <VStack gap="4">
      <Heading size="xsmall">Deleger til:</Heading>
      <NavidentSelectFormPart
        label="Søk opp medarbeider"
        name="newTilgang.delegertTilNavident"
      />
      <DatePicker {...gyldigTomDatePicker.datepickerProps}>
        <DatePicker.Input
          {...gyldigTomDatePicker.inputProps}
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
          // @ts-expect-error
          error={errors.newTilgang?.gyldigTom?.message?.toString()}
          label="Gyldig til og med:"
          size="small"
        />
      </DatePicker>
    </VStack>
  );
}

const tilgangSchema = yup.lazy((values) => {
  const dynamicKeysToSchema = Object.keys(values).map((key) => [
    key,
    key === "newTilgang" ? newTilgangSchema : renewTilgangSchema,
  ]);

  return yup.object().shape(Object.fromEntries(dynamicKeysToSchema));
});

export const newTilgangSchema = yup
  .object({
    delegertTilNavident: yup.string(),
    gyldigTom: yup.string().when("delegertTilNavident", {
      is: (delegertTilNavident: string) => !!delegertTilNavident,
      // eslint-disable-next-line unicorn/no-thenable
      then: (schema) =>
        schema.required("Må oppgis").futureOrPresent().max1YearInTheFuture(),
      otherwise: (schema) => schema.optional(),
    }),
  })
  .optional();

const renewTilgangSchema = yup.object({
  gyldigTom: yup.string().futureOrPresent().max1YearInTheFuture(),
});
