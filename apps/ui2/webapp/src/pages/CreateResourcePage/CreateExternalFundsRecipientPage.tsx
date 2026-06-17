import { css } from "@emotion/react";
import { ArrowLeftIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  BodyShort,
  Box,
  Button,
  CopyButton,
  Heading,
  HGrid,
  HStack,
  Label,
  Loader,
  LocalAlert,
  Search,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import type { AxiosError } from "axios";
import type { ReactNode } from "react";
import React, { useState } from "react";

import { getPerson, getRessurs } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { LoaderWithText } from "~/components/LoaderWithText";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import type { RessursNavn } from "~/types/api-models";
import {
  type GetPersonRequest,
  IdentType,
  type Person,
} from "~/types/api-models";
import { formatVisningsnavn } from "~/utils";

export function CreateExternalFundsRecipientPage() {
  return (
    <>
      <PageBanner text="Opprett ekstern utbetalingsmottaker" />

      <ContentWrapper>
        <ContainerWithHighlightedTop
          icon={opprettRessursIconRoundedBackground}
          title={"Personopplysninger"}
        >
          <ExternalFundsRecipientForm />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
}

function ExternalFundsRecipientForm() {
  const [fnr, setFnr] = useState<string | undefined>();

  const personRequest: GetPersonRequest = {
    identType: IdentType.STANDARD,
    personident: fnr ?? "",
  };

  const personQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personRequest),
    queryFn: () => getPerson.queryFn(personRequest),
    enabled: !!fnr,
    retry: false,
  });

  const createNavidentMutation = useMutation<
    string,
    AxiosError,
    { fnr: string },
    string
  >({
    mutationKey: ["opprett-navident", fnr],
    mutationFn: async (data) => {
      const res = await fetch("/frackend/nom-api/api/v1/navident", {
        method: "POST",
        body: JSON.stringify({
          personident: data.fnr,
          identType: IdentType.STANDARD,
        }),
        headers: {
          "Content-Type": "application/json",
        },
      });
      if (res.status === 200 || res.status === 201) {
        const json = await res.json();
        return json && typeof json === "object" && json["navident"];
      }
      throw new Error(
        "Opprettelse av navident: Mottok respons med HTTP status " + res.status,
      );
    },
  });

  const person = personQuery.data?.folkeregisterPerson?.navn;
  const maybeNavident = personQuery.data?.navident;

  const maybeFnrFormatError =
    fnr && fnr.length !== 11 ? "Fødselsnummer må være på 11 siffer" : undefined;

  const maybePdlFetchError = personQuery.error
    ? `Kall mot PDL for henting av person feilet med HTTP status ${personQuery.error.status}`
    : undefined;

  const maybeOpprettNavidentError =
    createNavidentMutation.error?.message ?? undefined;

  if (!person || !fnr) {
    return (
      <form
        onSubmit={(event) => {
          event.preventDefault();
        }}
      >
        <Search
          autoComplete="off"
          description={
            <strong
              css={css`
                color: var(--a-text-default);
              `}
            >
              Fyll inn fødselsnummer/D-nummer, så finner vi personen i
              Folkeregisteret
            </strong>
          }
          error={maybeFnrFormatError || maybePdlFetchError}
          hideLabel={false}
          label={<div hidden={true}>Personsøk</div>}
          name={"fnr"}
          onSearchClick={(v) => setFnr(v)}
          pattern={"\d{11}"}
          variant={"secondary"}
        />
        {personQuery.isFetching && (
          <LoaderWithText>Henter fra PDL</LoaderWithText>
        )}
      </form>
    );
  }

  if (!maybeNavident) {
    return (
      <>
        <ShowPerson
          backToFnrForm={() => setFnr(undefined)}
          fnr={fnr}
          ressursNavn={person}
        />
        {createNavidentMutation.isIdle && (
          <Button
            onClick={() => createNavidentMutation.mutateAsync({ fnr: fnr })}
          >
            Opprett navident
          </Button>
        )}
        {createNavidentMutation.isPending && (
          <LoaderWithText>Oppretter navident...</LoaderWithText>
        )}
        {createNavidentMutation.data && (
          <ShowCreatedNavIdent navIdent={createNavidentMutation.data} />
        )}
        {maybeOpprettNavidentError && (
          <ShowError
            errorTitle={"Opprettelse av Nav-Ident feilet"}
            reset={createNavidentMutation.reset}
          >
            {maybeOpprettNavidentError}
          </ShowError>
        )}
      </>
    );
  }

  return (
    <>
      <ShowPerson
        backToFnrForm={() => setFnr(undefined)}
        fnr={fnr}
        ressursNavn={person}
      />
      <ShowNavident navIdent={maybeNavident} />
    </>
  );
}

function ShowPerson(props: {
  ressursNavn: RessursNavn;
  fnr: string;
  backToFnrForm: () => void;
}) {
  const visningsNavn = formatVisningsnavn(props.ressursNavn);
  return (
    <Box
      css={css`
        display: grid;
        row-gap: 1rem;
      `}
      paddingBlock={"4"}
    >
      <Heading size={"medium"}>Om valgt person</Heading>
      <HGrid align={"center"} columns={"1fr 15rem"}>
        <Box>
          <Box>
            <Label>Personnummer</Label>
            <BodyShort>{props.fnr}</BodyShort>
          </Box>
          <Box>
            <Label>Navn</Label>
            <BodyShort>{visningsNavn}</BodyShort>
          </Box>
        </Box>
        <Button
          icon={<ArrowLeftIcon />}
          onClick={() => props.backToFnrForm()}
          variant={"secondary"}
        >
          Bruk annen person
        </Button>
      </HGrid>
    </Box>
  );
}

function ShowNavident(props: { navIdent: string }) {
  return (
    <LocalAlert
      css={css`
        margin: 2rem 0;
      `}
      status={"announcement"}
    >
      <LocalAlert.Header>
        <LocalAlert.Title>Fant eksisterende navident</LocalAlert.Title>
      </LocalAlert.Header>
      <LocalAlert.Content>
        <HStack align={"center"}>
          <BodyShort size={"large"} weight={"semibold"}>
            {props.navIdent}
          </BodyShort>
          <CopyButton copyText={props.navIdent}></CopyButton>
        </HStack>
        <BodyLong>
          Denne personen har navident fra før, og den kan brukes videre.
        </BodyLong>
        {props.navIdent && (
          <MaybeExistingResourceLink navIdent={props.navIdent} />
        )}
      </LocalAlert.Content>
    </LocalAlert>
  );
}

function ShowError(props: {
  errorTitle: string;
  children: string | ReactNode;
  reset?: () => void;
}) {
  return (
    <LocalAlert status={"error"}>
      <LocalAlert.Header>
        <LocalAlert.Title>{props.errorTitle}</LocalAlert.Title>
      </LocalAlert.Header>
      <LocalAlert.Content>
        <Box>{props.children}</Box>
        {props.reset && (
          <Box paddingBlock={"4"}>
            <Button onClick={props.reset} variant={"secondary"}>
              Forkast feilmelding
            </Button>
          </Box>
        )}
      </LocalAlert.Content>
    </LocalAlert>
  );
}

function ShowCreatedNavIdent(props: { navIdent: string }) {
  return (
    <LocalAlert
      css={css`
        margin: 2rem 0;
      `}
      status={"success"}
    >
      <LocalAlert.Header>
        <LocalAlert.Title>Navident opprettet</LocalAlert.Title>
      </LocalAlert.Header>
      <LocalAlert.Content>
        <BodyLong>Har opprettet navident:</BodyLong>
        <HStack align={"center"}>
          <BodyShort size={"large"} weight={"semibold"}>
            {props.navIdent}
          </BodyShort>
          <CopyButton copyText={props.navIdent} />
        </HStack>
        <BodyLong>For angitt person</BodyLong>
      </LocalAlert.Content>
    </LocalAlert>
  );
}

function MaybeExistingResourceLink(props: { navIdent: string }) {
  const checkIsRessursQry = useQuery({
    queryKey: getRessurs.queryKey(props.navIdent),
    queryFn: () => getRessurs.queryFn(props.navIdent),
    retry: false,
  });

  return (
    <>
      {checkIsRessursQry.isLoading && (
        <HStack>
          <Loader></Loader>
          <BodyShort>Sjekker om personen jobber i Nav...</BodyShort>
        </HStack>
      )}
      {checkIsRessursQry.data && (
        <SmartLink href={"/ressurs/" + props.navIdent}>
          Personen er en medarbeider i Nav
        </SmartLink>
      )}
    </>
  );
}
