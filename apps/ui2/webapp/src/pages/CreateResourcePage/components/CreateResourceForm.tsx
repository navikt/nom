import { css } from "@emotion/react";
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
import { Controller, FormProvider, useForm } from "react-hook-form";
import { Navigate, useNavigate, useParams } from "react-router";

import type {
  FolkeregisterPerson,
  OrgEnhet,
} from "~/api/generated/graphql_generated";
import {
  GyldigTomSelection,
  OrgnivSelection,
  ResultLimit,
  Sektor,
  useHentPersonQuery,
  useOpprettRessursMutation,
  useSearchOrgenheterQuery,
} from "~/api/generated/graphql_generated";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { capitalizeName } from "~/resources/utils/utils";

import { CreateResourceConfirmationModal } from "./CreateResourceConfirmationModal";
import { ResourceMetadata } from "./ResourceMetadata";

const rowStyles = css`
  display: flex;
  gap: 1rem;
  align-items: flex-start;

  & > * {
    flex: 1;
  }
`;

export default function CreateResourceForm() {
  const { personidentInBase64 } = useParams();
  const personident = decode(personidentInBase64 ?? "");

  const hentPersonQuery = useHentPersonQuery({
    variables: { personident: personident ?? "" },
    errorPolicy: "all",
  });

  const searchOrgenheterQuery = useSearchOrgenheterQuery({
    variables: {
      term: "*:*",
      filter: {
        gyldigTomSelection: GyldigTomSelection.IkkePassert,
        orgnivSelection: OrgnivSelection.OrgnivOrgenhet,
        limit: ResultLimit.Unlimited,
      },
    },
    errorPolicy: "all",
  });

  if (hentPersonQuery.loading || searchOrgenheterQuery.loading) {
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
  if (searchOrgenheterQuery.error) {
    return <Alert variant="error">{searchOrgenheterQuery.error.message}</Alert>;
  }

  const personResult = hentPersonQuery.data?.person;
  const enheter = searchOrgenheterQuery.data?.searchOrgEnhet || [];

  if (personResult?.navident) {
    return (
      <Navigate
        replace
        state={{ alreadyExists: true }}
        to={`/ressurs/${personResult.navident}`}
      />
    );
  }

  if (personResult?.person) {
    return (
      <>
        <ResourceMetadata person={personResult.person} />
        <CreateResourceFormInner
          orgEnheter={enheter as OrgEnhet[]}
          personResult={personResult.person}
          personident={personResult.personident}
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
  personident,
  orgEnheter,
}: {
  personResult: FolkeregisterPerson;
  personident: string;
  orgEnheter: OrgEnhet[];
}) {
  const navigate = useNavigate();
  const methods = useForm({
    defaultValues: {
      personident: personident,
      visningsfornavn: capitalizeName(personResult?.navn.fornavn ?? ""),
      visningsmellomnavn: capitalizeName(personResult?.navn.mellomnavn ?? ""),
      visningsetternavn: capitalizeName(personResult?.navn.etternavn ?? ""),
      sektor: Sektor.NavStatlig,
      orgEnhetId: undefined,
      startDato: new Date(),
    },
  });

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = methods;

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
    <FormProvider {...methods}>
      <form>
        <div
          css={css`
            display: flex;
            flex-direction: column;
            gap: var(--a-spacing-5);
            margin-top: var(--a-spacing-8);
          `}
        >
          <Heading size="small">Opplysninger til NOM</Heading>
          <Alert css={css({ marginTop: "1rem" })} size="medium" variant="info">
            Navnet under vil bli brukt i alle NAVs applikasjoner. Fjern
            eventuelle navn du ikke vil ha med. Ressursen må ha minst ett
            fornavn og minst ett etternavn.
          </Alert>

          <div css={rowStyles}>
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

          <OrgEnhetSelectFormPart orgEnheter={orgEnheter} />

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

          <div css={rowStyles}>
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
    </FormProvider>
  );
}
