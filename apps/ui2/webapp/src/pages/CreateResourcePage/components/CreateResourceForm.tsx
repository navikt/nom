import { css } from "@emotion/css";
import {
  Alert,
  Button,
  Heading,
  Loader,
  Radio,
  RadioGroup,
  TextField,
} from "@navikt/ds-react";
import { decode } from "js-base64";
import { useForm } from "react-hook-form";
import { Navigate, useNavigate, useParams } from "react-router";

import type {
  FolkeregisterPerson,
  Sektor,
} from "../../../api/generated/graphql_generated";
import {
  useHentPersonQuery,
  useOpprettRessursMutation,
} from "../../../api/generated/graphql_generated";
import { capitalizeName } from "../../../resources/utils/utils";
import CreateResourceConfirmationModal from "./CreateResourceConfirmationModal";
import { ResourceMetadata } from "./ResourceMetadata";

const rowStyles = css({
  display: "flex",
  gap: "1rem",
  alignItems: "flex-start",

  "& > *": {
    flex: 1,
  },
});

export default function CreateResourceForm() {
  const { personIdentInBase64 } = useParams();
  const personIdent = decode(personIdentInBase64 ?? "");

  const hentPersonQuery = useHentPersonQuery({
    variables: { personIdent: personIdent ?? "" },
    errorPolicy: "all",
  });

  if (hentPersonQuery.loading) {
    return (
      <Loader
        className={css`
          align-self: center;
        `}
        size="2xlarge"
        title={`Søker etter ${personIdent}`}
      />
    );
  }

  if (hentPersonQuery.error) {
    return <Alert variant="error">{hentPersonQuery.error.message}</Alert>;
  }

  const personResult = hentPersonQuery.data?.person;

  if (personResult?.navIdent) {
    return (
      <Navigate
        replace
        state={{ alreadyExists: true }}
        to={`/ressurs/${personResult.navIdent}`}
      />
    );
  }

  if (personResult?.person) {
    return (
      <>
        <ResourceMetadata person={personResult.person} />
        <CreateResourceFormInner
          personIdent={personResult.personIdent}
          personResult={personResult.person}
        />
      </>
    );
  }

  return <></>;
}

export type OpprettRessursFormTypes = {
  fornavn: string;
  mellomnavn?: string;
  etternavn: string;
  sektor: string;
};

function CreateResourceFormInner({
  personResult,
  personIdent,
}: {
  personResult: FolkeregisterPerson;
  personIdent: string;
}) {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<OpprettRessursFormTypes>({
    defaultValues: {
      fornavn: capitalizeName(personResult?.navn.fornavn ?? ""),
      mellomnavn: capitalizeName(personResult?.navn.mellomnavn ?? ""),
      etternavn: capitalizeName(personResult?.navn.etternavn ?? ""),
      sektor: "NAV_STATLIG",
    },
  });

  const [opprettRessursMutation, { loading, error }] =
    useOpprettRessursMutation({
      onCompleted: (data) => {
        navigate(
          `/ressurs/create/${data.opprettRessurs?.navident ?? ""}/receipt`,
          {
            replace: true,
          }
        );
      },
    });

  const handleOpprettRessurs = (submittedValues: OpprettRessursFormTypes) =>
    opprettRessursMutation({
      variables: {
        request: {
          personident: personIdent,
          visningsfornavn: submittedValues.fornavn,
          visningsmellomnavn: submittedValues.mellomnavn,
          visningsetternavn: submittedValues.etternavn,
          sektor: submittedValues.sektor as Sektor,
        },
      },
    });

  return (
    <form>
      <div
        className={css`
          display: flex;
          flex-direction: column;
          gap: var(--navds-spacing-5);
          margin-top: var(--navds-spacing-8);
        `}
      >
        <Heading size="small">Opplysninger til NOM</Heading>
        <Alert
          className={css({ marginTop: "1rem" })}
          size="medium"
          variant="info"
        >
          Navnet under vil bli brukt i alle NAVs applikasjoner. Fjern eventuelle
          navn du ikke vil ha med. Ressursen må ha minst ett fornavn og minst
          ett etternavn.
        </Alert>

        <div className={rowStyles}>
          <TextField
            autoComplete="off"
            error={errors.fornavn?.message}
            label="Fornavn"
            type="text"
            {...register("fornavn", { required: "Må oppgis" })}
          />
          <TextField
            autoComplete="off"
            error={errors.mellomnavn?.message}
            label="Mellomnavn"
            type="text"
            {...register("mellomnavn")}
          />
          <TextField
            autoComplete="off"
            error={errors.etternavn?.message}
            label="Etternavn"
            type="text"
            {...register("etternavn", { required: "Må oppgis" })}
          />
        </div>

        <RadioGroup legend="Sektor" size="medium" {...register("sektor")}>
          <Radio checked value="NAV_STATLIG">
            NAV statlig
          </Radio>
          <Radio disabled value="NAV_KOMMUNAL">
            NAV kommunal
          </Radio>
          <Radio disabled value="EKSTERN">
            Ekstern
          </Radio>
        </RadioGroup>

        <div className={rowStyles}>
          <CreateResourceConfirmationModal
            formValues={watch()}
            loading={loading}
            onConfirm={() => handleSubmit(handleOpprettRessurs)()}
          />
          <Button
            onClick={() => navigate("/ressurs/create", { replace: true })}
            variant="secondary"
          >
            Avbryt
          </Button>
        </div>

        {error ? <Alert variant="error">{error.message}</Alert> : undefined}
      </div>
    </form>
  );
}
