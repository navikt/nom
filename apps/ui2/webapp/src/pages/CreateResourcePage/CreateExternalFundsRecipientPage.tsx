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
import type { RessursNavn } from "~/types/api-models";
import {
  type GetPersonRequest,
  IdentType,
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
          <CreateNavidentForm />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
}

type FormStepState =
  | { state: "empty" }
  | {
      state: "got-person";
      ressursNavn: RessursNavn;
      fnr: string;
    }
  | {
      state: "got-existing-navident";
      ressursNavn: RessursNavn;
      fnr: string;
      navident: string;
    }
  | {
      state: "got-error";
      fnr: string;
      errorMessage: string;
      errorTitle: string;
    }
  | {
      state: "created-navident";
      navident: string;
      ressursNavn: RessursNavn;
      fnr: string;
    };

type FormStepProps<T extends FormStepState["state"]> = {
  formStepState: FormStepState & { state: T };
  setFormStepState: (fss: FormStepState) => void;
};

function CreateNavidentForm() {
  const [formStepState, setFormStepState] = React.useState<FormStepState>({
    state: "empty",
  });

  return (
    <>
      {formStepState.state === "empty" && (
        <StateIsEmpty
          formStepState={formStepState}
          setFormStepState={setFormStepState}
        />
      )}
      {formStepState.state === "got-person" && (
        <StateIsGotPerson
          formStepState={formStepState}
          setFormStepState={setFormStepState}
        />
      )}
      {formStepState.state === "got-existing-navident" && (
        <StateIsGotPersonWithExistingNavident
          formStepState={formStepState}
          setFormStepState={setFormStepState}
        />
      )}
      {formStepState.state === "created-navident" && (
        <StateIsCreatedNavident
          formStepState={formStepState}
          setFormStepState={setFormStepState}
        />
      )}
      {formStepState.state === "got-error" && (
        <StateIsGotError
          formStepState={formStepState}
          setFormStepState={setFormStepState}
        />
      )}
    </>
  );
}

function StateIsCreatedNavident(props: FormStepProps<"created-navident">) {
  return (
    <>
      <HGrid align={"center"} columns={"1fr 15rem"}>
        <VisningsNavnFormat
          fnr={props.formStepState.fnr}
          name={props.formStepState.ressursNavn}
        />
        <FinnAnnenPersonButton
          reset={() => props.setFormStepState({ state: "empty" })}
        />
      </HGrid>
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
              {props.formStepState.navident}
            </BodyShort>
            <CopyButton copyText={props.formStepState.navident} />
          </HStack>
          <BodyLong>For angitt person</BodyLong>
        </LocalAlert.Content>
      </LocalAlert>
    </>
  );
}

function StateIsGotError(props: FormStepProps<"got-error">) {
  return (
    <>
      <HGrid align={"center"} columns={"1fr 15rem"}>
        <VisningPersonNr fnr={props.formStepState.fnr} />
        <FinnAnnenPersonButton
          reset={() => props.setFormStepState({ state: "empty" })}
        />
      </HGrid>

      <LocalAlert status={"error"}>
        <LocalAlert.Header>
          <LocalAlert.Title>{props.formStepState.errorTitle}</LocalAlert.Title>
        </LocalAlert.Header>
        <LocalAlert.Content>
          {props.formStepState.errorMessage}
        </LocalAlert.Content>
      </LocalAlert>
    </>
  );
}

function StateIsEmpty(props: FormStepProps<"empty">) {
  const [error, setError] = React.useState<string | undefined>(undefined);
  const [loading, setLoading] = React.useState(false);

  const onSubmit = (e: React.SubmitEvent<HTMLFormElement>) => {
    const formData = new FormData(e.target);
    const fnr = formData.get("fnr");
    setLoading(true);

    if (fnr && typeof fnr === "string") {
      const maybeError = validateFnrFormat(fnr);
      if (maybeError) {
        setError(maybeError.errorMessage);
      } else {
        const personRequest: GetPersonRequest = {
          identType: IdentType.STANDARD,
          personident: fnr,
        };
        getPerson
          .queryFn(personRequest)
          .then((result) => {
            const common = {
              fnr: result.personident,
              ressursNavn: result.folkeregisterPerson.navn,
            };
            if (result.navident) {
              props.setFormStepState({
                state: "got-existing-navident",
                navident: result.navident,
                ...common,
              });
            } else {
              props.setFormStepState({ state: "got-person", ...common });
            }
          })
          .catch((error: AxiosError) => {
            props.setFormStepState({
              state: "got-error",
              errorTitle: "Feil under henting av person",
              errorMessage: "Kall mot PDL feilet med status " + error.status,
              fnr: fnr,
            });
          });
      }
    }
    return e.preventDefault();
  };

  if (loading) {
    return (
      <HStack>
        <Loader></Loader>
        <BodyShort>Henter person via PDL...</BodyShort>
      </HStack>
    );
  }

  return (
    <form onSubmit={onSubmit}>
      {
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
      }
    </form>
  );
}

function StateIsGotPerson(props: FormStepProps<"got-person">) {
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
        "Mottok respons fra backend med HTTP status " + res.status,
      );
    },
    retry: 0,
    onSuccess: (data) => {
      if (data) {
        props.setFormStepState({
          state: "created-navident",
          navident: data,
          ressursNavn: props.formStepState.ressursNavn,
          fnr: props.formStepState.fnr,
        });
      } else {
        props.setFormStepState({
          state: "got-error",
          errorMessage: "Uventet feil. Mangler data",
          errorTitle: "Opprettelse av navident feilet",
          fnr: props.formStepState.fnr,
        });
      }
    },
    onError: (error) => {
      props.setFormStepState({
        state: "got-error",
        errorMessage: error.message,
        errorTitle: "Opprettelse av navident feilet",
        fnr: props.formStepState.fnr,
      });
    },
  });

  if (createNavidentMutation.status === "pending") {
    return (
      <Box>
        <Loader /> Oppretter navident...
      </Box>
    );
  }

  return (
    <>
      <HGrid align={"center"} columns={"1fr 15rem"}>
        <VisningsNavnFormat
          fnr={props.formStepState.fnr}
          name={props.formStepState.ressursNavn}
        />
        <FinnAnnenPersonButton
          reset={() => props.setFormStepState({ state: "empty" })}
        />
      </HGrid>
      <Button
        onClick={() =>
          dynamicChooseActionMatrix(
            "Bekreft opprett navident for '" +
              formatVisningsNavn(props.formStepState.ressursNavn) +
              "'?",
            [
              {
                buttonText: "OK",
                onAction: () => {
                  createNavidentMutation.mutateAsync({
                    fnr: props.formStepState.fnr,
                  });
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
    </>
  );
}

function StateIsGotPersonWithExistingNavident(
  props: FormStepProps<"got-existing-navident">,
) {
  return (
    <>
      <HGrid align={"center"} columns={"1fr 15rem"}>
        <VisningsNavnFormat
          fnr={props.formStepState.fnr}
          name={props.formStepState.ressursNavn}
        />
        <FinnAnnenPersonButton
          reset={() => props.setFormStepState({ state: "empty" })}
        />
      </HGrid>
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
              {props.formStepState.navident}
            </BodyShort>
            <CopyButton copyText={props.formStepState.navident}></CopyButton>
          </HStack>
          <BodyLong>
            Denne personen har navident fra før, og den kan brukes videre.
          </BodyLong>
          <MaybeExistingResourceLink navIdent={props.formStepState.navident} />
        </LocalAlert.Content>
      </LocalAlert>
    </>
  );
}

function FinnAnnenPersonButton(props: { reset: () => void }) {
  return (
    <Button
      icon={<ArrowLeftIcon />}
      onClick={() =>
        dynamicChooseActionMatrix("Bekreft finn annen person", [
          {
            onAction: () => props.reset(),
            buttonText: "Ok",
          },
          { onAction: () => {}, buttonText: "Avbryt" },
        ])
      }
      variant={"secondary"}
    >
      Finn annen person...
    </Button>
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

function validateFnrFormat(fnr: string): undefined | { errorMessage: string } {
  fnr = fnr?.replaceAll(" ", "") || "";
  if (fnr.length !== 11) {
    return {
      errorMessage: "Feil antall siffer. Skal være 11, fant " + fnr.length,
    };
  }
}

function formatVisningsNavn(navn: Visningsnavn) {
  return [navn.fornavn, navn.mellomnavn, navn.etternavn]
    .filter((it) => it)
    .join(" ");
}

function VisningsNavnFormat(props: { name: Visningsnavn; fnr: string }) {
  const visningsNavn = formatVisningsNavn(props.name);

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
        <BodyShort>{props.fnr}</BodyShort>
      </Box>
      <Box>
        <Label>Navn</Label>
        <BodyShort>{visningsNavn}</BodyShort>
      </Box>
    </Box>
  );
}

function VisningPersonNr(props: { fnr: string }) {
  return (
    <Box
      css={css`
        display: grid;
        row-gap: 1rem;
      `}
      paddingBlock={"4"}
    >
      <Heading size={"medium"}>Kontekst</Heading>
      <Box>
        <Label>Personnummer</Label>
        <BodyShort>{props.fnr}</BodyShort>
      </Box>
    </Box>
  );
}
