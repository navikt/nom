import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  BodyShort,
  HelpText,
  Label,
  Loader,
  TextField,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React, { useEffect } from "react";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgTilknytninger, getPerson } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { FolkeregisterPersonInfo } from "~/pages/CreateResourcePage/components/FolkeregisterPersonInfo";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { SearchForPerson } from "~/pages/CreateResourcePage/components/SearchForPerson";
import { nomDateIsTodayOrAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import type {
  FolkeregisterPerson,
  GetPersonRequest,
  Person,
} from "~/types/api-models";
import { IdentType } from "~/types/api-models";
import { capitalizeName } from "~/utils";

export function PersonopplysningerStep() {
  const { personident } = useSearch({
    from: "/ressurs/create/$step",
  });

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIconRoundedBackground}
      title="Personopplysninger"
    >
      <SearchForPerson />
      {!!personident && <CreateResourceForm personident={personident} />}
    </ContainerWithHighlightedTop>
  );
}

function CreateResourceForm({ personident }: { personident: string }) {
  const personRequest: GetPersonRequest = {
    identType: IdentType.STANDARD,
    personident: personident,
  };

  const hentPersonQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personRequest),
    queryFn: () => getPerson.queryFn(personRequest),
    retry: false,
  });

  const person = hentPersonQuery.data;

  const hentOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(person?.navident ?? ""),
    queryFn: () => getOrgTilknytninger.queryFn(person?.navident ?? ""),
    enabled: !!person?.navident,
  });

  const orgtilknytninger = hentOrgtilknytningerQuery.data;

  const erAktiv =
    person &&
    orgtilknytninger &&
    orgtilknytninger.some((orgtilknytning) =>
      nomDateIsTodayOrAfter(orgtilknytning.gyldigTom),
    );

  if (hentPersonQuery.isLoading || hentOrgtilknytningerQuery.isLoading) {
    return (
      <Loader
        css={css`
          align-self: center;
        `}
        size="3xlarge"
        title={`Søker etter ${personident}`}
      />
    );
  }

  if (hentPersonQuery.error) {
    return (
      <Alert variant="error">
        {hentPersonQuery.error.response?.status === 404
          ? "Ingen resultater for søket. Det må hånteres manuelt."
          : "En teknisk feil forhindret søket"}
      </Alert>
    );
  }

  if (erAktiv) {
    return (
      <Alert variant="error">
        Denne personen finnes allerede som aktiv eller fremtidig medarbeider med
        Nav-ident <b>{person?.navident}</b>. For å gjøre endringer, gå til{" "}
        <Link to={`/ressurs/${person?.navident}`}>
          medarbeiderens profilside
        </Link>
        .
      </Alert>
    );
  }

  if (person) {
    return (
      <>
        {person.navident && (
          <Alert variant="info">
            Medarbeideren har jobbet i Nav tidligere og vil beholde sin
            tidligere Nav-ident <b>{person.navident}</b>.
          </Alert>
        )}
        <FolkeregisterPersonInfo
          folkeregisterPerson={person.folkeregisterPerson}
        />
        <VisningsnavnForm folkeregisterPerson={person.folkeregisterPerson} />
      </>
    );
  }

  return <></>;
}

function VisningsnavnForm({
  folkeregisterPerson,
}: {
  folkeregisterPerson: FolkeregisterPerson;
}) {
  const hasAdminAccess = useHasAdminAccess();

  const navigate = useNavigate({ from: "/ressurs/create/$step" });

  const {
    visningsfornavn,
    visningsmellomnavn,
    visningsetternavn,
    primaryTelefonnummer,
  } = useSearch({
    from: "/ressurs/create/$step",
  });

  const methods = useForm({
    defaultValues: {
      visningsfornavn:
        visningsfornavn ||
        capitalizeName(folkeregisterPerson?.navn.fornavn ?? ""),
      visningsmellomnavn:
        visningsmellomnavn ||
        capitalizeName(folkeregisterPerson?.navn.mellomnavn ?? ""),
      visningsetternavn:
        visningsetternavn ||
        capitalizeName(folkeregisterPerson?.navn.etternavn ?? ""),
      primaryTelefonnummer: primaryTelefonnummer ?? "",
    },
    resolver: yupResolver(navnValidationSchema),
  });

  const {
    handleSubmit,
    register,
    formState: { errors },
    reset,
  } = methods;

  useEffect(() => {
    reset({
      visningsfornavn:
        visningsfornavn ||
        capitalizeName(folkeregisterPerson?.navn.fornavn ?? ""),
      visningsmellomnavn:
        visningsmellomnavn ||
        capitalizeName(folkeregisterPerson?.navn.mellomnavn ?? ""),
      visningsetternavn:
        visningsetternavn ||
        capitalizeName(folkeregisterPerson?.navn.etternavn ?? ""),
      primaryTelefonnummer: primaryTelefonnummer ?? "",
    });
  }, [
    visningsfornavn,
    visningsmellomnavn,
    visningsetternavn,
    primaryTelefonnummer,
    folkeregisterPerson,
    reset,
  ]);

  return (
    <>
      <form
        onSubmit={handleSubmit((values) => {
          const nummerUtenSpaces = values.primaryTelefonnummer?.replaceAll(
            " ",
            "",
          );
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "2" },
            search: (oldSearch) => ({
              ...oldSearch,
              ...values,
              primaryTelefonnummer: nummerUtenSpaces,
            }),
          }).then();
        })}
      >
        <b>Visningnavn</b>
        <BodyShort spacing>
          Visningsnavnet blir benyttet til å generere medarbeiderens
          e-postadresse, og det vil også bli brukt i alle Navs applikasjoner.
          Undersøk derfor hvordan medarbeider har skrevet sitt navn i
          rekrutteringsprosessen, og vurder å fjerne eventuelle mellomnavn.
        </BodyShort>
        <div css={fullwidthRow}>
          <TextField
            autoComplete="off"
            data-cy="fornavn-textfield"
            error={errors.visningsfornavn?.message}
            label="Fornavn"
            type="text"
            {...register("visningsfornavn")}
            readOnly={!hasAdminAccess}
          />
          <TextField
            autoComplete="off"
            data-cy="mellomnavn-textfield"
            error={errors.visningsmellomnavn?.message}
            label="Mellomnavn"
            type="text"
            {...register("visningsmellomnavn")}
          />
          <TextField
            autoComplete="off"
            data-cy="etternavn-textfield"
            error={errors.visningsetternavn?.message}
            label="Etternavn"
            type="text"
            {...register("visningsetternavn")}
            readOnly={!hasAdminAccess}
          />
        </div>
        <TextField
          {...register("primaryTelefonnummer")}
          autoComplete="off"
          css={css`
            margin-top: var(--a-spacing-4);
            width: 33%;
            padding-right: var(--a-spacing-2);
          `}
          data-cy="telefon-textfield"
          description="Telefonnummer er valgfritt"
          error={errors.primaryTelefonnummer?.message}
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
                nummer må starte med landkode, og ha maks 21 siffer.
              </HelpText>
            </div>
          }
        />
        <NavigationButtons goBackCallback={() => {}} />
      </form>
    </>
  );
}

const navnValidationSchema = yup.object().shape({
  primaryTelefonnummer: yup.string().optionalPhoneNumber(),
  visningsfornavn: yup.string().required("Obligatorisk"),
  visningsmellomnavn: yup.string().optional(),
  visningsetternavn: yup
    .string()
    .required("Obligatorisk")
    .matches(/^\S+$/, "Etternavn kan ikke inneholde mellomrom"),
});
