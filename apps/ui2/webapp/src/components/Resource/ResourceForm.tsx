import {
  Alert,
  Button,
  Heading,
  Loader,
  Radio,
  RadioGroup,
  Search,
  TextField,
} from "@navikt/ds-react";
import { useForm } from "react-hook-form";
import { css } from "@emotion/css";
import { Navigate, useNavigate, useParams } from "react-router";
import ResourceMetadata from "./ResourceMetadata";
import {
  FolkeregisterPerson,
  Sektor,
  useHentPersonQuery,
  useOpprettRessursMutation,
} from "../../api/generated/graphql_generated";
import CreateResourceConfirmationModal from "./CreateResourceConfirmationModal";

const rowStyles = css({
  display: "flex",
  gap: "1rem",
  alignItems: "flex-start",

  "& > *": {
    flex: 1,
  },
});

export default function ResourceForm() {
  const { personIdent } = useParams();

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
    return <Navigate to={`/ressurs/${personResult.navIdent}`} replace />;
  }

  if (personResult?.person) {
    return (
      <>
        <ResourceMetadata person={personResult.person} />
        <ResourceFormInner
          personResult={personResult.person}
          personIdent={personResult.personIdent}
        />
      </>
    );
  }

  return null;
}

export type OpprettRessursFormTypes = {
  fornavn: string;
  etternavn: string;
  sektor: string;
};

function ResourceFormInner({
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
      fornavn: personResult?.navn.fornavn ?? "",
      etternavn: personResult?.navn.etternavn ?? "",
      sektor: "NAV_STATLIG",
    },
  });

  const [opprettRessursMutation, { loading, error }] =
    useOpprettRessursMutation({
      onCompleted: (data) => {
        navigate(`/ressurs/${data.opprettRessurs?.navident}`, {
          replace: true,
        });
      },
    });

  const handleOpprettRessurs = (submittedValues: OpprettRessursFormTypes) =>
    opprettRessursMutation({
      variables: {
        request: {
          personident: personIdent,
          visningsfornavn: submittedValues.fornavn,
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
          variant="info"
          size="medium"
          className={css({ marginTop: "1rem" })}
        >
          Navnet under vil bli brukt i alle NAVs applikasjoner. Fjern eventuelle
          navn du ikke vil ha med. Ressursen må ha minst ett fornavn og minst
          ett etternavn.
        </Alert>

        <div className={rowStyles}>
          <TextField
            type="text"
            label="Fornavn"
            error={errors.fornavn?.message}
            autoComplete="off"
            {...register("fornavn", { required: "Må oppgis" })}
          />
          <TextField
            type="text"
            label="Etternavn"
            error={errors.etternavn?.message}
            autoComplete="off"
            {...register("etternavn", { required: "Må oppgis" })}
          />
        </div>

        <Search
          label="Org.enhet"
          variant="secondary"
          hideLabel={false}
          disabled
        />

        <RadioGroup legend="Sektor" size="medium" {...register("sektor")}>
          <Radio value="NAV_STATLIG" checked>
            NAV statlig
          </Radio>
          <Radio value="NAV_KOMMUNAL" disabled>
            NAV kommunal
          </Radio>
          <Radio value="EKSTERN" disabled>
            Ekstern
          </Radio>
        </RadioGroup>

        <div className={rowStyles}>
          <CreateResourceConfirmationModal
            loading={loading}
            onConfirm={() => handleSubmit(handleOpprettRessurs)()}
            formValues={watch()}
          />
          <Button
            variant="secondary"
            onClick={() => navigate("/ressurs/create", { replace: true })}
          >
            Avbryt
          </Button>
        </div>

        {error ? <Alert variant="error">{error.message}</Alert> : null}
      </div>
    </form>
  );
}
