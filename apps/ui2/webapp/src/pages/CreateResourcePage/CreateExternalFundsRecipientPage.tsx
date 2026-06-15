import { css } from "@emotion/react";
import { ArrowLeftIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  BodyShort,
  Box,
  Button,
  CopyButton,
  Heading,
  HStack,
  Label,
  Loader,
  LocalAlert,
  Search,
} from "@navikt/ds-react";
import {
  type DefaultError,
  useMutation,
  useQuery,
} from "@tanstack/react-query";
import type { AxiosError } from "axios";
import React, { Fragment } from "react";

import { getPerson, getRessurs } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { dynamicChooseActionMatrix } from "~/components/ExpiryNotifier";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import {
  type GetPersonRequest,
  IdentType,
  type Person,
  type Visningsnavn,
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
          <InputPersonIdent />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
}

function validateFnrFormat(fnr: string): undefined | { errorMessage: string } {
  fnr = fnr || "";
  if (fnr.length !== 11) {
    return {
      errorMessage: "Feil antall siffer. Skal være 11, fant " + fnr.length,
    };
  }
}

function InputPersonIdent() {
  const [queryFnr, setQueryFnr] = React.useState<string | undefined>(undefined);
  const [error, setError] = React.useState<string | undefined>(undefined);
  const [createdNavIdent, setCreatedNavIdent] = React.useState<
    string | undefined
  >(undefined);

  const personRequest: GetPersonRequest = {
    identType: IdentType.STANDARD,
    personident: queryFnr ?? "",
  };

  const personQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personRequest),
    queryFn: () => getPerson.queryFn(personRequest),
    enabled: !!queryFnr,
    retry: false,
  });

  const maybeVisningsNavn = personQuery.data?.folkeregisterPerson.navn;
  const maybeFormattedVisningsNavn = maybeVisningsNavn
    ? formatVisningsNavn(maybeVisningsNavn)
    : undefined;
  const maybeEksisterendeNavIdent = personQuery.data?.navident;

  return (
    <form
      onSubmit={(e) => {
        const formData = new FormData(e.target);
        const fnr = formData.get("fnr");

        if (fnr && typeof fnr === "string") {
          const maybeError = validateFnrFormat(fnr);
          if (maybeError) {
            setError(maybeError.errorMessage);
          } else {
            setQueryFnr(fnr);
          }
        }
        return e.preventDefault();
      }}
    >
      {!queryFnr && (
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
          error={error}
          hideLabel={false}
          label={<div hidden={true}>Personsøk</div>}
          name={"fnr"}
          variant={"secondary"}
        />
      )}
      {queryFnr && (
        <Button
          icon={<ArrowLeftIcon />}
          onClick={() =>
            dynamicChooseActionMatrix("Bekreft finn annen person", [
              {
                onAction: () => {
                  setQueryFnr(undefined);
                  setCreatedNavIdent(undefined);
                  setError(undefined);
                },
                buttonText: "Ok",
              },
              { onAction: () => {}, buttonText: "Avbryt" },
            ])
          }
          variant={"secondary"}
        >
          Finn annen person...
        </Button>
      )}
      <Box paddingBlock={"8"}>
        {personQuery.isLoading && (
          <Box>
            <Loader />
            Henter person...
          </Box>
        )}
        {personQuery.error && (
          <LocalAlert status={"error"}>
            <LocalAlert.Header>
              <LocalAlert.Title>
                Kunne ikke hente info om person
              </LocalAlert.Title>
            </LocalAlert.Header>
            <LocalAlert.Content>
              HTTP statuskode fra PDL: {personQuery.failureReason?.status}
            </LocalAlert.Content>
          </LocalAlert>
        )}
        <Fragment key={queryFnr}>
          <MaybeVisningsNavnFormat
            maybeFnr={queryFnr}
            maybeName={maybeVisningsNavn}
          />
          <MaybeExistingNavIdent navIdent={maybeEksisterendeNavIdent} />
          <MaybeCreateNavidentButton
            hasNavIdentFromEarlier={
              !!maybeEksisterendeNavIdent || !!createdNavIdent
            }
            maybeFnr={queryFnr}
            maybeVisningsNavn={maybeFormattedVisningsNavn}
            onSuccess={(createdNavIdent) => {
              setCreatedNavIdent(createdNavIdent);
            }}
          />
          <MaybeCreatedNavIdent navIdent={createdNavIdent} />
        </Fragment>
      </Box>
    </form>
  );
}

function MaybeExistingNavIdent(props: { navIdent: string | undefined }) {
  if (!props.navIdent) {
    return null;
  }

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

function MaybeCreatedNavIdent(props: { navIdent: string | undefined }) {
  if (!props.navIdent) {
    return null;
  }
  return (
    <LocalAlert
      css={css`
        margin: 2rem 0;
      `}
      status={"success"}
    >
      <LocalAlert.Header>
        <LocalAlert.Title>Navident opprettet!</LocalAlert.Title>
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

function MaybeCreateNavidentButton(props: {
  hasNavIdentFromEarlier: boolean;
  maybeVisningsNavn: string | undefined;
  maybeFnr: string | undefined;
  onSuccess: (fnr: string) => void;
}) {
  const createNavidentMutation = useMutation<
    string,
    DefaultError,
    { fnr: string },
    string
  >({
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
    onSuccess: (data) => {
      if (data) {
        props.onSuccess(data);
      }
    },
  });

  if (
    props.hasNavIdentFromEarlier ||
    !props.maybeVisningsNavn ||
    !props.maybeFnr
  ) {
    return;
  }

  if (createNavidentMutation.status === "pending") {
    return (
      <Box>
        <Loader />
        Oppretter navident...
      </Box>
    );
  }

  if (createNavidentMutation.error) {
    return (
      <LocalAlert status={"error"}>
        <LocalAlert.Header>
          <LocalAlert.Title>Kunne ikke opprette navident</LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          <Box>{createNavidentMutation.error.message}</Box>
          <Box paddingBlock={"4"}>
            <Button
              onClick={createNavidentMutation.reset}
              variant={"secondary"}
            >
              Forkast feilmelding
            </Button>
          </Box>
        </LocalAlert.Content>
      </LocalAlert>
    );
  }

  if (props.maybeVisningsNavn && props.maybeFnr) {
    return (
      <Button
        onClick={() =>
          dynamicChooseActionMatrix(
            "Bekreft opprett navident for '" + props.maybeVisningsNavn + "'?",
            [
              {
                buttonText: "OK",
                onAction: () => {
                  createNavidentMutation.mutateAsync({ fnr: props.maybeFnr!! });
                },
              },
              {
                buttonText: "Avbryt",
                onAction: () => {},
              },
            ],
          )
        }
      >
        Opprett navident ...{" "}
      </Button>
    );
  }
}

function formatVisningsNavn(navn: Visningsnavn) {
  return [navn.fornavn, navn.mellomnavn, navn.etternavn]
    .filter((it) => it)
    .join(" ");
}

function MaybeVisningsNavnFormat(props: {
  maybeName: Visningsnavn | undefined;
  maybeFnr: string | undefined;
}) {
  if (!props.maybeName || !props.maybeFnr) {
    return null;
  }
  const visningsNavn = formatVisningsNavn(props.maybeName);

  return (
    <Box
      css={css`
        display: grid;
        row-gap: 1rem;
      `}
      paddingBlock={"4"}
    >
      <Heading size={"medium"}>Om valgt person</Heading>
      <Box>
        <Label>Personnummer</Label>
        <BodyShort>{props.maybeFnr}</BodyShort>
      </Box>
      <Box>
        <Label>Navn</Label>
        <BodyShort>{visningsNavn}</BodyShort>
      </Box>
    </Box>
  );
}
