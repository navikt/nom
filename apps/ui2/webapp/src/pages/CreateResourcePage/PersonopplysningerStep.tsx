import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Alert, BodyShort, Loader, TextField } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Navigate, useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import { getPerson } from "~/api/react-query-endpoints";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { FolkeregisterPersonInfo } from "~/pages/CreateResourcePage/components/FolkeregisterPersonInfo";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { SearchForPerson } from "~/pages/CreateResourcePage/components/SearchForPerson";
import { createRessursRoute, ressursRoute } from "~/reactRouterRoutes";
import { fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import type { FolkeregisterPerson, Person } from "~/types/api-models";
import { capitalizeName } from "~/utils";

export function PersonopplysningerStep() {
  const { personident } = useSearch({
    from: createRessursRoute.fullPath,
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

  if (hentPersonQuery.isLoading) {
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
          ? "Ingen resultater for søket"
          : "En teknisk feil forhindret søket"}
      </Alert>
    );
  }

  const person = hentPersonQuery.data;

  if (person?.navident) {
    return (
      <Navigate
        params={{ id: person.navident }}
        replace
        search={{ alreadyExists: true }}
        to={ressursRoute.to}
      />
    );
  }

  if (person) {
    return (
      <>
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
  const navigate = useNavigate({ from: createRessursRoute.fullPath });
  const { visningsfornavn, visningsmellomnavn, visningsetternavn } = useSearch({
    from: createRessursRoute.fullPath,
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
            to: createRessursRoute.id,
            params: { step: "3" },
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
            error={errors.visningsfornavn?.message}
            label="Fornavn"
            type="text"
            {...register("visningsfornavn")}
          />
          <TextField
            autoComplete="off"
            error={errors.visningsmellomnavn?.message}
            label="Mellomnavn"
            type="text"
            {...register("visningsmellomnavn")}
          />
          <TextField
            autoComplete="off"
            error={errors.visningsetternavn?.message}
            label="Etternavn"
            type="text"
            {...register("visningsetternavn")}
          />
        </div>
        <NavigationButtons />
      </form>
    </>
  );
}

const navnValidationSchema = yup.object({
  visningsfornavn: yup.string().required("Obligatorisk"),
  visningsmellomnavn: yup.string().optional(),
  visningsetternavn: yup.string().required("Obligatorisk"),
});
