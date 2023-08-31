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
import type { AxiosError, AxiosResponse } from "axios";
import axios from "axios";
import { decode } from "js-base64";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useMutation } from "react-query";
import { Navigate, useNavigate, useParams } from "react-router";
import * as yup from "yup";

import type {
  FolkeregisterPerson,
  OrgEnhet,
} from "~/api/generated/graphql_generated";
import {
  AnsettelsesType,
  GyldigTomSelection,
  OrgnivSelection,
  ResultLimit,
  Sektor,
  useHentPersonQuery,
  useSearchOrgenheterQuery,
} from "~/api/generated/graphql_generated";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import type { OpprettRessursRequest, OpprettRessursResponse } from "~/types";
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

export const AnsettelsesTypeNameMapping = {
  [AnsettelsesType.Fast]: "Fast",
  [AnsettelsesType.Midlertidig]: "Midlertidig",
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
      ansettelsesType: AnsettelsesType.Fast,
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
    control,
    formState: { errors },
  } = methods;

  const selectedOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === watch("orgEnhetId"),
  );

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("startDato", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: undefined,
  });

  const opprettRessursMutation = useMutation<
    AxiosResponse<OpprettRessursResponse>,
    AxiosError<Error>,
    OpprettRessursRequest
  >(
    (opprettRessursRequest) =>
      axios.post("/nom-api/app/ressurs", opprettRessursRequest),
    {
      onSuccess: (opprettRessursResponse) => {
        // Because ansettelsesType is not yet persisted in NOM we must retrieve it from the form.
        const ansettelsesType = watch("ansettelsesType");
        navigate(
          `/ressurs/create/${
            opprettRessursResponse.data.navident ?? ""
          }/receipt?ansettelsesType=${ansettelsesType}`,
          {
            replace: true,
          },
        );
      },
    },
  );

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

        <Controller
          control={control}
          name="sektor"
          render={({ field }) => (
            <RadioGroup legend="Sektor" size="medium" {...field}>
              <Alert size="medium" variant="info">
                For øyeblikket kan du kun bruke NOM til å opprette statlige
                ansatte med fast eller midlertidig ansettelsesforhold.
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
          )}
        />

        <Controller
          control={control}
          name="ansettelsesType"
          render={({ field }) => (
            <RadioGroup {...field} legend="Ansettelsestype" size="medium">
              <Radio checked value={AnsettelsesType.Fast}>
                {AnsettelsesTypeNameMapping[AnsettelsesType.Fast]}
              </Radio>
              <Radio value={AnsettelsesType.Midlertidig}>
                {AnsettelsesTypeNameMapping[AnsettelsesType.Midlertidig]}
              </Radio>
            </RadioGroup>
          )}
        />

        <OrgEnhetSelectFormPart orgEnheter={orgEnheter} />
        {selectedOrgEnhet ? (
          <div
            css={css`
              display: flex;
              justify-content: space-between;
            `}
          >
            <span>
              Leder: <b>{selectedOrgEnhet.leder[0]?.ressurs.visningsnavn}</b>
            </span>
            {selectedOrgEnhet.remedyEnhetId && (
              <span>
                Enhetsnummer: <b>{selectedOrgEnhet.remedyEnhetId}</b>
              </span>
            )}
          </div>
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
            loading={opprettRessursMutation.isLoading}
            onConfirm={handleSubmit((validValues) =>
              opprettRessursMutation.mutate(validValues),
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
        {opprettRessursMutation.isError ? (
          <Alert variant="error">{opprettRessursMutation.error.message}</Alert>
        ) : undefined}
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
  ansettelsesType: yup
    .mixed<AnsettelsesType>()
    .oneOf(Object.values(AnsettelsesType))
    .required("Obligatorisk"),
});

export type FormValues = yup.InferType<typeof validationSchema>;
