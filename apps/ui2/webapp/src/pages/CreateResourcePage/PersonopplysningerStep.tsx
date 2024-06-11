import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Alert, BodyShort, Loader, TextField } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgTilknytninger, getPerson } from "~/api/react-query-endpoints";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { FolkeregisterPersonInfo } from "~/pages/CreateResourcePage/components/FolkeregisterPersonInfo";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { SearchForPerson } from "~/pages/CreateResourcePage/components/SearchForPerson";
import { fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import type { FolkeregisterPerson, Person } from "~/types/api-models";
import { capitalizeName, isTodayOrAfter, isTodayOrBefore } from "~/utils";

export function PersonopplysningerStep() {
  const { personident } = useSearch({
    from: "/ressurs/create/$step",
  });

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIcon}
      title="Personopplysninger"
    >
      <SearchForPerson />
      {!!personident && <CreateResourceForm personident={personident} />}
    </ContainerWithHighlightedTop>
  );
}

function CreateResourceForm({ personident }: { personident: string }) {
  const hentPersonQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personident),
    queryFn: () => getPerson.queryFn(personident),
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
    orgtilknytninger.some(
      (orgtilknytning) =>
        isTodayOrBefore(new Date(orgtilknytning.gyldigFom)) &&
        (!orgtilknytning.gyldigTom ||
          isTodayOrAfter(new Date(orgtilknytning.gyldigTom))),
    );

  if (hentPersonQuery.isLoading || hentOrgtilknytningerQuery.isLoading) {
    return (
      <Loader
        css={css`
          align-self: center;
        `}
        size="2xlarge"
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
        Denne personen finnes allerede som aktiv medarbeider med NAV-ident{" "}
        <b>{person?.navident}</b>. For å gjøre endringer, gå til{" "}
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
            Medarbeideren har jobbet i NAV tidligere og vil beholde sin
            tidligere NAV-ident <b>{person.navident}</b>.
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
  } = methods;

  return (
    <>
      <form
        onSubmit={handleSubmit((values) =>
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "2" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <b>Visningnavn</b>
        <BodyShort spacing>
          Navnet under vil bli brukt i alle NAVs applikasjoner. Medarbeideren må
          ha minst ett fornavn og minst ett etternavn.
        </BodyShort>
        <div css={fullwidthRow}>
          <TextField
            autoComplete="off"
            data-cy="fornavn-textfield"
            error={errors.visningsfornavn?.message}
            label="Fornavn"
            type="text"
            {...register("visningsfornavn")}
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
          />
        </div>
        <TextField
          {...register("primaryTelefonnummer")}
          autoComplete="off"
          css={css`
            max-width: 350px;
            margin-top: var(--a-spacing-4);
          `}
          data-cy="telefon-textfield"
          description="Telefonnummer er valgfritt"
          error={errors.primaryTelefonnummer?.message}
          label="Telefonnummer"
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
