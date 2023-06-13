import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  Button,
  DatePicker,
  Label,
  Loader,
  Radio,
  RadioGroup,
  TextField,
  useDatepicker,
} from "@navikt/ds-react";
import { decode } from "js-base64";
import type { SubmitHandler } from "react-hook-form";
import { FormProvider, useForm } from "react-hook-form";
import { Navigate, useNavigate, useParams } from "react-router";
import * as yup from "yup";

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
import { capitalizeName, formatDateWithoutTimezone } from "~/utils";

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
  const methods = useForm<Partial<FormValues>>({
    defaultValues: {
      personident: personident,
      visningsfornavn: capitalizeName(personResult?.navn.fornavn ?? ""),
      visningsmellomnavn: capitalizeName(personResult?.navn.mellomnavn ?? ""),
      visningsetternavn: capitalizeName(personResult?.navn.etternavn ?? ""),
      sektor: Sektor.NavStatlig,
      orgEnhetId: undefined,
      startDato: undefined,
    },
    resolver: yupResolver(validationSchema),
  });

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = methods;

  const selectedOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === watch("orgEnhetId")
  );
  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("startDato", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: undefined,
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

  const submitHandler: SubmitHandler<FormValues> = (validatedValues) => {
    opprettRessursMutation({
      variables: {
        request: validatedValues,
      },
    });
  };

  return (
    <FormProvider {...methods}>
      <form
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-5);
          margin-top: var(--a-spacing-8);
        `}
      >
        <Label as="span">Visningnavn</Label>
        <Alert
          css={css`
            margin-top: calc(-1 * var(--a-spacing-3));
          `}
          size="medium"
          variant="info"
        >
          Navnet under vil bli brukt i alle NAVs applikasjoner. Se hvilket navn
          ressursen selv har brukt i Webcruiter, og fjern eventuelle navn som
          ikke skal være med. Ressursen må ha minst ett fornavn og minst ett
          etternavn.
        </Alert>

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

        <RadioGroup legend="Sektor" size="medium" {...register("sektor")}>
          <Alert size="medium" variant="info">
            For øyeblikket kan du kun bruke NOM til å opprette faste, statlige
            ansatte. Midlertidige statlige ansatte må du opprette ved hjelp av
            ordinær prosess i Unit4.
          </Alert>
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
        {selectedOrgEnhet ? (
          <span>
            Leder for org.enhet:{" "}
            {selectedOrgEnhet.leder[0]?.ressurs.visningsnavn}
          </span>
        ) : undefined}

        <DatePicker {...startDatePicker.datepickerProps}>
          <DatePicker.Input
            {...startDatePicker.inputProps}
            error={errors.startDato?.message?.toString()}
            label="Første arbeidsdag"
          />
        </DatePicker>

        <div css={rowStyles}>
          <CreateResourceConfirmationModal
            formValues={watch()}
            loading={loading}
            onConfirm={handleSubmit((validValues) =>
              submitHandler(validValues as FormValues)
            )}
            orgEnheter={orgEnheter}
          />
          <Button
            onClick={() => navigate("/ressurs/create", { replace: true })}
            variant="secondary"
          >
            Avbryt
          </Button>
        </div>
        {error ? <Alert variant="error">{error.message}</Alert> : undefined}
      </form>
    </FormProvider>
  );
}

const validationSchema = yup.object({
  personident: yup.string().required("Obligatorisk"),
  visningsfornavn: yup.string().required("Obligatorisk"),
  visningsmellomnavn: yup.string().optional(),
  visningsetternavn: yup.string().required("Obligatorisk"),
  sektor: yup
    .mixed<Sektor>()
    .oneOf(Object.values(Sektor))
    .required("Obligatorisk"),
  orgEnhetId: yup.string().required("Obligatorisk"),
  startDato: yup.string().required("Obligatorisk"),
});

export type FormValues = yup.InferType<typeof validationSchema>;
