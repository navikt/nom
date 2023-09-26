import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { Alert, BodyShort, Loader, TextField } from "@navikt/ds-react";
import { Navigate, useNavigate, useSearch } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import * as yup from "yup";

import type { FolkeregisterPerson } from "~/api/generated/graphql_generated";
import { useHentPersonQuery } from "~/api/generated/graphql_generated";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { FolkeregisterPersonInfo } from "~/pages/CreateResourcePage/components/FolkeregisterPersonInfo";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { SearchForPerson } from "~/pages/CreateResourcePage/components/SearchForPerson";
import { rowStyles } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { createRessursRoute, ressursRoute } from "~/reactRouterRoutes";
import { capitalizeName } from "~/utils";

export function PersonopplysningerStep() {
  const { personident } = useSearch({
    from: createRessursRoute.id,
  });

  return (
    <ContainerWithHighlightedTop title="Personopplysninger">
      <SearchForPerson />
      {!!personident && <CreateResourceForm personident={personident} />}
    </ContainerWithHighlightedTop>
  );
}

function CreateResourceForm({ personident }: { personident: string }) {
  const hentPersonQuery = useHentPersonQuery({
    variables: { personident: personident ?? "" },
  });

  if (hentPersonQuery.loading) {
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
    return <Alert variant="error">{hentPersonQuery.error.message}</Alert>;
  }

  const personResult = hentPersonQuery.data?.person;

  if (personResult?.navident) {
    return (
      <Navigate
        params={{ id: personResult.navident }}
        replace
        search={{ alreadyExists: true }}
        to={ressursRoute.to}
      />
    );
  }

  if (personResult?.person) {
    return (
      <>
        <FolkeregisterPersonInfo person={personResult.person} />
        <VisningsnavnForm person={personResult.person} />
      </>
    );
  }

  return <></>;
}

function VisningsnavnForm({ person }: { person: FolkeregisterPerson }) {
  const navigate = useNavigate();
  const { visningsfornavn, visningsmellomnavn, visningsetternavn } = useSearch({
    from: createRessursRoute.to,
  });

  const methods = useForm({
    defaultValues: {
      visningsfornavn:
        visningsfornavn || capitalizeName(person?.navn.fornavn ?? ""),
      visningsmellomnavn:
        visningsmellomnavn || capitalizeName(person?.navn.mellomnavn ?? ""),
      visningsetternavn:
        visningsetternavn || capitalizeName(person?.navn.etternavn ?? ""),
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
            to: createRessursRoute.to,
            params: { step: "3" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <b>Visningnavn</b>
        <BodyShort spacing>
          Navnet under vil bli brukt i alle NAVs applikasjoner. Ressursen må ha
          minst ett fornavn og minst ett etternavn.
        </BodyShort>
        <div css={rowStyles}>
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
