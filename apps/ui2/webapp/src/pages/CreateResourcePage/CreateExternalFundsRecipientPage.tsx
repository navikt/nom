import { css } from "@emotion/react";
import {
  Alert,
  BodyLong,
  BodyShort,
  Box,
  Button,
  CopyButton,
  HStack,
  Loader,
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
import { FolkeregisterPersonInfo } from "~/pages/CreateResourcePage/components/FolkeregisterPersonInfo";
import { PanelLink } from "~/pages/CreateResourcePage/SummaryStep";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import type { RessursNavn } from "~/types/api-models";
import {
  type GetPersonRequest,
  IdentType,
  type Person,
} from "~/types/api-models";

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
  const [tmpFnr, setTmpFnr] = useState<string | undefined>();

  const personRequest: GetPersonRequest = {
    identType: IdentType.STANDARD,
    personident: fnr ?? "",
  };

  const personQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personRequest),
    queryFn: () => getPerson.queryFn(personRequest),
    enabled: !!fnr,
    retry: false,
    staleTime: Infinity,
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
  const maybePreviousNavident = personQuery.data?.navident;

  const maybeFnrFormatError =
    fnr && fnr.length !== 11 ? "Fødselsnummer må være på 11 siffer" : undefined;

  const maybeOpprettNavidentError =
    createNavidentMutation.error?.message ?? undefined;

  const searchInput = (
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
        error={maybeFnrFormatError}
        hideLabel={false}
        label={<div hidden={true}>Personsøk</div>}
        name={"fnr"}
        onChange={(value) => {
          setTmpFnr(value);
        }}
        onSearchClick={(v) => {
          setTmpFnr(v);
          setFnr(v);
        }}
        value={tmpFnr}
        variant={"secondary"}
      />
      {personQuery.isFetching && (
        <LoaderWithText>Henter fra PDL</LoaderWithText>
      )}
    </form>
  );

  const gotPerson = person && fnr;
  if (!gotPerson) {
    const maybePdlFetchError = personQuery.error
      ? `Kall mot PDL for henting av person feilet med HTTP status ${personQuery.error.status}`
      : undefined;
    return (
      <>
        {searchInput}
        {maybePdlFetchError && (
          <Alert variant="error">
            {maybePdlFetchError
              ? "Fødselsnummeret ble ikke funnet i Folkeregisteret. Kontroller at du har lagt inn riktig fødselsnummer."
              : "En teknisk feil forhindret søket"}
          </Alert>
        )}
      </>
    );
  }

  if (maybePreviousNavident) {
    return (
      <>
        {searchInput}
        <ShowPerson ressursNavn={person} />
        <ShowPreviousNavident navIdent={maybePreviousNavident} />
        <PanelLink href={"/"} label={"Gå tilbake til forsiden"} />
      </>
    );
  }

  return (
    <>
      {searchInput}
      <ShowPerson ressursNavn={person} />

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
        <>
          <ShowCreatedNavIdent navIdent={createNavidentMutation.data} />
          <PanelLink href={"/"} label={"Gå tilbake til forsiden"} />
        </>
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

function ShowPerson(props: { ressursNavn: RessursNavn }) {
  return (
    <FolkeregisterPersonInfo
      folkeregisterPerson={{ navn: props.ressursNavn }}
    />
  );
}

function ShowPreviousNavident(props: { navIdent: string }) {
  return (
    <Alert variant={"info"}>
      <BodyShort size={"large"} weight={"semibold"}>
        Fant eksisterende navident
      </BodyShort>
      <Box paddingBlock={"4"}>
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
      </Box>
    </Alert>
  );
}

function ShowError(props: {
  errorTitle: string;
  children: string | ReactNode;
  reset?: () => void;
}) {
  return (
    <Alert variant={"error"}>
      <BodyShort size={"large"} weight={"semibold"}>
        {props.errorTitle}
      </BodyShort>
      <Box paddingBlock={"4"}>
        <Box>{props.children}</Box>
        {props.reset && (
          <Box paddingBlock={"4"}>
            <Button onClick={props.reset} variant={"secondary"}>
              Forkast feilmelding
            </Button>
          </Box>
        )}
      </Box>
    </Alert>
  );
}

function ShowCreatedNavIdent(props: { navIdent: string }) {
  return (
    <Alert variant={"success"}>
      <BodyShort size={"large"} weight={"semibold"}>
        Navident opprettet
      </BodyShort>
      <Box paddingBlock={"4"}>
        <BodyLong>Har opprettet navident:</BodyLong>
        <HStack align={"center"}>
          <BodyShort size={"large"} weight={"semibold"}>
            {props.navIdent}
          </BodyShort>
          <CopyButton copyText={props.navIdent} />
        </HStack>
        <BodyLong>For angitt person</BodyLong>
      </Box>
    </Alert>
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
        <LoaderWithText>Sjekker om personen jobber i Nav...</LoaderWithText>
      )}
      {checkIsRessursQry.data && (
        <SmartLink href={"/ressurs/" + props.navIdent}>
          Personen er eller har vært medarbeider i Nav.
        </SmartLink>
      )}
      {checkIsRessursQry.isFetched && (
        <BodyShort>Personen har ikke vært medarbeider i Nav.</BodyShort>
      )}
    </>
  );
}
