import { css } from "@emotion/css";
import {
  Alert,
  Button,
  Heading,
  Label,
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
import Select, { createFilter } from "react-select";
import type { FilterOptionOption } from "react-select/dist/declarations/src/filters";
import { v4 as uuidv4 } from "uuid";

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
import { capitalizeName } from "~/resources/utils/utils";

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
        className={css`
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
  const MIN_INPUT_LENGTH = 3;
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = useForm({
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
          name="orgEnhetId"
          render={({
            field: { onChange, value, name },
            fieldState: { error },
          }) => {
            const fieldId = `${name}_${uuidv4()}`;
            return (
              <>
                <Label htmlFor={fieldId} size="medium">
                  Org.enhet
                </Label>
                {error && (
                  <Alert size="small" variant="error">
                    {error.message}
                  </Alert>
                )}
                <Select
                  backspaceRemovesValue
                  className={css`
                    cursor: text;
                    min-width: 200px;
                    border-radius: var(--navds-border-radius-medium);
                    border: 1px solid var(--navds-select-color-border);
                    z-index: 2; // 10.01.2023: Hack to get around the datepicker button having a z-index of 1 and overlaying on the dropdown.

                    &:focus-within {
                      outline: none;
                      box-shadow: var(--navds-shadow-focus);
                    }
                  `}
                  components={{
                    // eslint-disable-next-line unicorn/no-null
                    DropdownIndicator: () => null, // Remove dropdown icon
                    // eslint-disable-next-line unicorn/no-null
                    IndicatorSeparator: () => null, // Remove separator
                  }}
                  defaultMenuIsOpen={false}
                  escapeClearsValue={true}
                  filterOption={
                    (candidate: FilterOptionOption<OrgEnhet>, input: string) =>
                      input.length >= MIN_INPUT_LENGTH &&
                      createFilter({})(candidate, input) // Use Select's default filtering for string matching by creating filter
                  }
                  getOptionLabel={(orgenhet) =>
                    `${orgenhet.navn} (${orgenhet.id})`
                  }
                  getOptionValue={(orgenhet: OrgEnhet) => orgenhet.id}
                  inputId={fieldId}
                  isClearable
                  isMulti={false}
                  noOptionsMessage={(input) => {
                    if (input.inputValue.length < MIN_INPUT_LENGTH) {
                      return "Du må skrive minst 3 tegn for å søke";
                    }
                    return "Ingen valg tilgjengelig";
                  }}
                  onChange={(orgEnhet: OrgEnhet | null) =>
                    onChange(orgEnhet?.id)
                  }
                  openMenuOnClick={false}
                  openMenuOnFocus={false}
                  options={orgEnheter}
                  placeholder="Søk etter organisasjonsenhet..."
                  styles={{
                    // Removes default focus-border, so that it can be replaced with focus from DesignSystem
                    control: (base) => ({
                      ...base,
                      boxShadow: "none",
                      border: 0,
                      cursor: "text",
                      div: {
                        div: { color: `var(--navds-dropdown-menu-color)` },
                      },
                    }),
                    // Make border and size of input box to be identical with those from DesignSystem
                    valueContainer: (base) => ({
                      ...base,
                      padding: "8px",
                      color: "black",
                    }),
                  }}
                  value={orgEnheter.find((enhet) => enhet.id === value)}
                />
              </>
            );
          }}
          rules={{ required: "Må angis" }}
        />

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
