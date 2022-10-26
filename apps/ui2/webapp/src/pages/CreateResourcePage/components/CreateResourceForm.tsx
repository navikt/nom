import { css } from "@emotion/css";
import {
  Alert,
  Button,
  Heading,
  Loader,
  Radio,
  RadioGroup,
  TextField,
  UNSAFE_DatePicker,
  UNSAFE_useDatepicker,
} from "@navikt/ds-react";
import { decode } from "js-base64";
import { Controller, useForm } from "react-hook-form";
import { Navigate, useNavigate, useParams } from "react-router";

import type { FolkeregisterPerson } from "../../../api/generated/graphql_generated";
import {
  Sektor,
  useHentPersonQuery,
  useOpprettRessursMutation,
} from "../../../api/generated/graphql_generated";
import { capitalizeName } from "../../../resources/utils/utils";
import { CreateResourceConfirmationModal } from "./CreateResourceConfirmationModal";
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
    variables: { personident: personIdent ?? "" },
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

export const SektorNameMapping = {
  [Sektor.Ekstern]: "Ekstern",
  [Sektor.NavKommunal]: "Kommunal",
  [Sektor.NavStatlig]: "Statlig",
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
    control,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: {
      personident: personIdent,
      visningsfornavn: capitalizeName(personResult?.navn.fornavn ?? ""),
      visningsmellomnavn: capitalizeName(personResult?.navn.mellomnavn ?? ""),
      visningsetternavn: capitalizeName(personResult?.navn.etternavn ?? ""),
      sektor: Sektor.NavStatlig,
      startDato: new Date(),
    },
  });

  const { datepickerProps, inputProps } = UNSAFE_useDatepicker({
    defaultSelected: watch("startDato"),
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
            error={errors.visningsfornavn?.message}
            label="Fornavn"
            type="text"
            {...register("visningsfornavn", { required: "Må oppgis" })}
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
            {...register("visningsetternavn", { required: "Må oppgis" })}
          />
        </div>

        <RadioGroup legend="Sektor" size="medium" {...register("sektor")}>
          <Radio checked value={Sektor.NavStatlig}>
            {SektorNameMapping[Sektor.NavStatlig]}
          </Radio>
          <Radio disabled value={Sektor.NavKommunal}>
            {SektorNameMapping[Sektor.NavKommunal]}
          </Radio>
          <Radio disabled value={Sektor.Ekstern}>
            {SektorNameMapping[Sektor.Ekstern]}
          </Radio>
        </RadioGroup>

        <Controller
          control={control}
          name="startDato"
          render={({ field: { onChange } }) => (
            <UNSAFE_DatePicker {...datepickerProps} onSelect={onChange}>
              <UNSAFE_DatePicker.Input
                {...inputProps}
                label="Første arbeidsdag"
              />
            </UNSAFE_DatePicker>
          )}
        />

        <div className={rowStyles}>
          <CreateResourceConfirmationModal
            formValues={watch()}
            loading={loading}
            onConfirm={() =>
              handleSubmit((submittedValues) => {
                opprettRessursMutation({
                  variables: {
                    request: {
                      ...submittedValues,
                      startDato: submittedValues.startDato
                        .toISOString()
                        .slice(0, 10),
                    },
                  },
                });
              })()
            }
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
