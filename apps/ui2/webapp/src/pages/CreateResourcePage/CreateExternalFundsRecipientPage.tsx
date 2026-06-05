import {
  BodyLong,
  BodyShort,
  Box,
  Button,
  Heading,
  Label,
  Link,
  Loader,
  LocalAlert,
  ReadMore,
  Search,
} from "@navikt/ds-react";
import React, { Fragment } from "react";

import ContentWrapper from "~/components/ContentWrapper";
import {PageBanner} from "~/components/PageBanner";
import {ContainerWithHighlightedTop} from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import {GetPersonRequest, IdentType, Person, Visningsnavn,} from "~/types/api-models";
import {DefaultError, useMutation, useQuery} from "@tanstack/react-query";
import type {AxiosError} from "axios";
import {getPerson} from "~/api/react-query-endpoints";
import {css} from "@emotion/react";
import {dynamicChooseActionMatrix} from "~/components/ExpiryNotifier";
import {SmartLink} from "~/components/navigation/SmartLink";

export function CreateExternalFundsRecipientPage() {
  return (
    <>
      <PageBanner text="Opprett ekstern lønnsmottaker" />

      <ContentWrapper>
        <ContainerWithHighlightedTop icon={opprettRessursIconRoundedBackground}>
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
  const [createdNavIdent, setCreatedNavIdent] = React.useState<string | undefined>(undefined);

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
  const maybeFormattedVisningsNavn = maybeVisningsNavn ? formatVisningsNavn(maybeVisningsNavn) : undefined;
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
      <Search
        description={"Finn personen i folkeregisteret via personnummer"}
        error={error}
        hideLabel={false}
        label={"Fødselsnr"}
        name={"fnr"}
      />
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
            maybeName={maybeVisningsNavn}
            maybeFnr={queryFnr}
          />
          <MaybeExistingNavIdent navIdent={maybeEksisterendeNavIdent} />
          <MaybeCreateNavidentButton
            maybeFnr={queryFnr}
            hasNavIdentFromEarlier={!!maybeEksisterendeNavIdent || !!createdNavIdent}
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

function MaybeExistingNavIdent(props: {navIdent: string | undefined}){
  if(!props.navIdent){
    return null;
  }
  return <Box>
    Fant tidligere navident <SmartLink href={"/ressurs/"+props.navIdent}>{props.navIdent}</SmartLink> på denne personen, og denne navidenten kan brukes videre.
  </Box>
}

function MaybeCreatedNavIdent(props: {navIdent: string | undefined}){
  if(!props.navIdent){
    return null;
  }
  return <Box>
    Har opprettet navident <strong>{props.navIdent}</strong> for denne personen.
  </Box>
}

function MaybeCreateNavidentButton(props: {hasNavIdentFromEarlier: boolean, maybeVisningsNavn: string | undefined, maybeFnr: string | undefined, onSuccess: (fnr: string) => void }) {
  const createNavidentMutation = useMutation<string,DefaultError,{fnr: string},string>({
    mutationFn: async (data) =>{
      const res = await fetch("/frackend/nom-api/api/v1/navident", {
        method: "POST",
        body: JSON.stringify({
          personident: data.fnr,
          identType: IdentType.STANDARD
        }),
        headers:{
          "Content-Type": "application/json",
        }
      });
      if(res.status === 200 || res.status === 201){
        const json = await res.json();
        return json && typeof json === "object" && json["navident"];
      }
      throw new Error("Opprettelse av navident: Mottok respons med HTTP status " + res.status);
    },
    onSuccess: (data) => {
      if(data){
        props.onSuccess(data);
      }
    },
    onError: (err) =>{
      console.error(err);
    }
  })

  if (props.hasNavIdentFromEarlier || !props.maybeVisningsNavn || !props.maybeFnr) {
    return;
  }

  if(createNavidentMutation.status === "pending"){
    return (
      <Box>
        <Loader />
        Oppretter navident...
      </Box>
    );
  }

  if(createNavidentMutation.error){
    return (
      <LocalAlert status={"error"}>
        <LocalAlert.Header>
          <LocalAlert.Title>Kunne ikke opprette navident</LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          <Box>{createNavidentMutation.error.message}
          </Box>
          <Box paddingBlock={"4"}>
            <Button variant={"secondary"} onClick={createNavidentMutation.reset}>Forkast feilmelding</Button>
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
                onAction: () => {
                },
              },
            ],
          )
        }
      >Opprett navident ... </Button>
    );
  }
}

function formatVisningsNavn(navn: Visningsnavn){
  return [navn.fornavn, navn.mellomnavn, navn.etternavn]
    .filter((it) => it)
    .join(" ");
}

function MaybeVisningsNavnFormat(props: {maybeName: Visningsnavn | undefined, maybeFnr: string | undefined}){
  if(!props.maybeName || !props.maybeFnr){
    return null;
  }
  const visningsNavn = formatVisningsNavn(props.maybeName)

  return (
    <Box
      paddingBlock={"4"}
      css={css`
        display: grid;
        row-gap: 1rem;
        //border-left: solid black 0.1rem;
        //padding-left: 2rem;
      `}
    >
      <Heading size={"medium"}>Fant person</Heading>
      <Box>
        <Label>Personnummer</Label>
        <BodyShort>{props.maybeFnr}</BodyShort>
      </Box>
      <Box>
        <Label>Navn</Label>
        <BodyShort>{visningsNavn}</BodyShort>
        <ReadMore header={"Mer om navnet"}>
          <Box
            as={"dl"}
            css={css`
              display: flex;
              column-gap: 4rem;
              dt {
                text-align: left;
              }
            `}
          >
            <Box>
              <Label as={"dt"}>Fornavn:</Label>
              <BodyShort as={"dd"}>{props.maybeName.fornavn}</BodyShort>
            </Box>
            <Box>
              <Label as={"dt"}>Mellomnavn:</Label>
              <BodyShort as={"dd"}>
                {props.maybeName.mellomnavn ?? "-- ikke satt --"}
              </BodyShort>
            </Box>
            <Box>
              <Label as={"dt"}>Etternavn:</Label>
              <BodyShort as={"dd"}>{props.maybeName.etternavn}</BodyShort>
            </Box>
          </Box>
        </ReadMore>
      </Box>
    </Box>
  );
}
