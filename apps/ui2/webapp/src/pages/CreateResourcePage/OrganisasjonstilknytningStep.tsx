import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  BodyShort,
  Checkbox,
  DatePicker,
  HelpText,
  useDatepicker,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { addYears, subDays } from "date-fns";
import pickBy from "lodash/pickBy";
import { useEffect, useState } from "react";
import {
  FormProvider,
  useController,
  useForm,
  useFormContext,
} from "react-hook-form";
import * as yup from "yup";

import {
  GyldigTomSelection,
  OrgnivSelection,
  ResultLimit,
  useSearchOrgenheterQuery,
} from "~/api/generated/graphql_generated";
import { harLederTilgangForOrgenhetOptions } from "~/api/react-query-endpoints";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { defaultFormStyle, fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { Sektor } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

export function OrganisasjonstilknytningStep() {
  const { sektor } = useSearch({ from: "/ressurs/create/$step" });
  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIcon}
      title="Organisasjonstilknytninger"
    >
      {(() => {
        switch (sektor) {
          case Sektor.NAV_KOMMUNAL: {
            return <CreateKommunaleRessurs />;
          }
          case Sektor.EKSTERN: {
            return <CreateEksternRessurs />;
          }
          case Sektor.NAV_STATLIG: {
            return <CreateKommunaleRessurs />;
          }
        }
      })()}
    </ContainerWithHighlightedTop>
  );
}

// TODO: må refaktoreres når den tid kommer
/*function CreateStatligRessurs() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });
  const { ansettelsesType, orgEnhetId, gyldigFom } = useSearch({
    from: "/ressurs/create/$step",
  });
  const methods = useForm({
    defaultValues: {
      ansettelsesType: ansettelsesType ?? AnsettelsesType.FAST,
      orgEnhetId,
      gyldigFom: undefined,
    },
    resolver: yupResolver(statligValidationSchema),
  });

  const {
    handleSubmit,
    setValue,
    control,
    formState: { errors },
  } = methods;

  const navigerTilbake = async () => {
    const values = methods.watch();

    const validValues = pickBy(values, (value, key) => {
      try {
        statligValidationSchema.validateSyncAt(key, { [key]: value });
        return true;
      } catch {
        return false;
      }
    });

    navigate({
      to: "/ressurs/create/$step",
      params: { step: "2" },
      search: (oldSearch) => ({ ...oldSearch, ...validValues }),
    });
  };

  useEffect(() => {
    if (gyldigFom) {
      setValue("gyldigFom", gyldigFom);
    }
  }, [gyldigFom]);

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigFom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: gyldigFom ? new Date(gyldigFom) : undefined,
  });

  return (
    <FormProvider {...methods}>
      <form
        css={defaultFormStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "9" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <InnerOrgEnhetSelect />
        <Controller
          control={control}
          name="ansettelsesType"
          render={({ field }) => (
            <RadioGroup {...field} legend="Ansettelsestype" size="medium">
              <Radio checked value={AnsettelsesType.FAST}>
                {AnsettelsesTypeNameMapping[AnsettelsesType.FAST]}
              </Radio>
              <Radio value={AnsettelsesType.MIDLERTIDIG}>
                {AnsettelsesTypeNameMapping[AnsettelsesType.MIDLERTIDIG]}
              </Radio>
            </RadioGroup>
          )}
        />

        <DatePicker {...startDatePicker.datepickerProps}>
          <DatePicker.Input
            {...startDatePicker.inputProps}
            error={errors.gyldigFom?.message?.toString()}
            label="Startdato (første arbeidsdag)"
          />
        </DatePicker>
        <NavigationButtons goBackCallback={navigerTilbake} />
      </form>
    </FormProvider>
  );
}*/

function CreateEksternRessurs() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });
  const { orgEnhetId, gyldigFom, gyldigTom, fysiskBrukerstedNr, sektor } =
    useSearch({
      from: "/ressurs/create/$step",
    });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: orgEnhetId,
      gyldigFom: undefined,
      gyldigTom: undefined,
      fysiskBrukerstedNr: fysiskBrukerstedNr,
    },

    resolver: yupResolver(
      eksternValidationSchema.concat(fysiskBrukerstedNrSchema),
    ),
  });

  const { handleSubmit } = methods;

  if (sektor === undefined) {
    return <Alert variant="error">Mangler informasjon om sektor</Alert>;
  }

  const navigerTilbake = async () => {
    const values = methods.watch();

    const validValues = pickBy(values, (value, key) => {
      try {
        eksternValidationSchema
          .concat(fysiskBrukerstedNrSchema)
          .validateSyncAt(key, { [key]: value });
        return true;
      } catch {
        return false;
      }
    });

    navigate({
      to: "/ressurs/create/$step",
      params: { step: "2" },
      search: (oldSearch) => ({ ...oldSearch, ...validValues }),
    });
  };

  return (
    <FormProvider {...methods}>
      <form
        css={defaultFormStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "9" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <OrganisasjonstilknytningInnerForm
          defaultSluttDato={
            gyldigTom ||
            formatDateWithoutTimezone(subDays(addYears(new Date(), 1), 1))
          }
          defaultStartDato={gyldigFom}
          erDagligOppfolgingLockedToTrue
          sektor={sektor}
        />
        <FysiskBrukerstedSelectFormPart />
        <NavigationButtons goBackCallback={navigerTilbake} />
      </form>
    </FormProvider>
  );
}

function CreateKommunaleRessurs() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });
  const { orgEnhetId, gyldigFom, gyldigTom, fysiskBrukerstedNr, sektor } =
    useSearch({
      from: "/ressurs/create/$step",
    });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: orgEnhetId,
      gyldigFom: undefined,
      gyldigTom: undefined,
      fysiskBrukerstedNr: fysiskBrukerstedNr,
    },

    resolver: yupResolver(
      kommunalValidationSchema.concat(fysiskBrukerstedNrSchema),
    ),
  });

  const { handleSubmit } = methods;

  const navigerTilbake = async () => {
    const values = methods.watch();

    const validValues = pickBy(values, (value, key) => {
      try {
        kommunalValidationSchema
          .concat(fysiskBrukerstedNrSchema)
          .validateSyncAt(key, { [key]: value });
        return true;
      } catch {
        return false;
      }
    });

    navigate({
      to: "/ressurs/create/$step",
      params: { step: "2" },
      search: (oldSearch) => ({ ...oldSearch, ...validValues }),
    });
  };

  if (sektor === undefined) {
    return <Alert variant="error">Mangler informasjon om sektor</Alert>;
  }

  return (
    <FormProvider {...methods}>
      <form
        css={defaultFormStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "9" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <OrganisasjonstilknytningInnerForm
          defaultSluttDato={gyldigTom}
          defaultStartDato={gyldigFom}
          erDagligOppfolgingLockedToTrue
          sektor={sektor}
        />
        <FysiskBrukerstedSelectFormPart />
        <NavigationButtons goBackCallback={navigerTilbake} />
      </form>
    </FormProvider>
  );
}

const fysiskBrukerstedNrSchema = yup.object().shape({
  fysiskBrukerstedNr: yup.string().required("Obligatorisk"),
});

const orgEnhetIdSchema = yup.object().shape({
  orgEnhetId: yup.string().required("Obligatorisk"),
});

/*
const statligValidationSchema = yup.object({
  orgEnhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  ansettelsesType: yup
    .mixed<AnsettelsesType>()
    .oneOf(Object.values(AnsettelsesType))
    .required("Obligatorisk"),
});
*/
const kommunalPeriodeValidationSchema = yup.object().shape({
  gyldigFom: yup.string().required("Obligatorisk"),
  gyldigTom: yup.string().optional(),
});

const kommunalValidationSchema =
  kommunalPeriodeValidationSchema.concat(orgEnhetIdSchema);

export const eksternPeriodeValidationSchema = yup.object().shape({
  gyldigFom: yup.string().required("Obligatorisk"),
  gyldigTom: yup.string().max1YearInTheFuture().required("Obligatorisk"),
  setRessursSluttdato: yup.boolean(),
});

export const eksternValidationSchema =
  eksternPeriodeValidationSchema.concat(orgEnhetIdSchema);

export function OrganisasjonstilknytningInnerForm({
  defaultStartDato,
  defaultSluttDato,
  sektor,
  erDagligOppfolgingLockedToTrue = false,
}: {
  defaultStartDato?: string;
  defaultSluttDato?: string;
  sektor: Sektor;
  erDagligOppfolgingLockedToTrue?: boolean;
}) {
  const { setValue, watch } = useFormContext();

  const [erDagligOppfolgingChecked, setErDagligOppfolgingChecked] = useState(
    erDagligOppfolgingLockedToTrue,
  );

  const hasAccessToOrgenhetQuery = useQuery(
    harLederTilgangForOrgenhetOptions(watch("orgEnhetId")),
  );
  // Antar at man har tilgang mens den er pending, slik at man slipper at Alert'en "popper" midlertidig
  const hasWriteAccessForOrgenhet =
    (hasAccessToOrgenhetQuery.isPending || hasAccessToOrgenhetQuery.data) ??
    false;

  return (
    <>
      <InnerOrgEnhetSelect />
      {watch("orgEnhetId") ? (
        <>
          {hasWriteAccessForOrgenhet ? undefined : (
            <Alert variant="error">
              Du har ikke tilgang til å opprette medarbeidere i denne enheten.
            </Alert>
          )}
          <Checkbox
            checked={erDagligOppfolgingChecked}
            disabled={erDagligOppfolgingLockedToTrue}
            onChange={(event) => {
              setErDagligOppfolgingChecked(event.target.checked);
              setValue("erDagligOppfolging", event.target.checked);
            }}
          >
            <BodyShort>
              Denne lederen har ansvar for den daglige oppfølgingen av
              medarbeideren
            </BodyShort>
          </Checkbox>
        </>
      ) : undefined}
      <Orgtilknytningsvarighet
        defaultSluttDato={defaultSluttDato}
        defaultStartDato={defaultStartDato}
        sektor={sektor}
      />
    </>
  );
}

function Orgtilknytningsvarighet({
  defaultStartDato,
  defaultSluttDato,
  sektor,
}: {
  defaultStartDato?: string;
  defaultSluttDato?: string;
  sektor: Sektor;
}) {
  const gyldigFomController = useController({
    name: "gyldigFom",
  });

  const gyldigTomController = useController({
    name: "gyldigTom",
  });

  useEffect(() => {
    if (defaultStartDato) {
      gyldigFomController.field.onChange(defaultStartDato);
    }
  }, [defaultStartDato]);

  useEffect(() => {
    if (defaultSluttDato) {
      gyldigTomController.field.onChange(defaultSluttDato);
    }
  }, [defaultSluttDato]);

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      gyldigFomController.field.onChange(
        date ? formatDateWithoutTimezone(date) : "",
      ),
    defaultSelected: defaultStartDato ? new Date(defaultStartDato) : undefined,
  });

  const sluttDatePicker = useDatepicker({
    onDateChange: (date) =>
      gyldigTomController.field.onChange(
        date ? formatDateWithoutTimezone(date) : "",
      ),
    defaultSelected: defaultSluttDato ? new Date(defaultSluttDato) : undefined,
  });

  return (
    <div
      css={css(
        fullwidthRow,
        css`
          margin-top: var(--a-spacing-8);
        `,
      )}
    >
      <FullWidthDatePickerWrapper>
        <DatePicker {...startDatePicker.datepickerProps}>
          <DatePicker.Input
            {...startDatePicker.inputProps}
            error={gyldigFomController.fieldState.error?.message?.toString()}
            label="Startdato (første arbeidsdag)"
            ref={gyldigFomController.field.ref}
          />
        </DatePicker>
      </FullWidthDatePickerWrapper>
      <FullWidthDatePickerWrapper>
        <DatePicker {...sluttDatePicker.datepickerProps}>
          <DatePicker.Input
            {...sluttDatePicker.inputProps}
            data-cy="sluttDato-picker"
            error={gyldigTomController.fieldState.error?.message?.toString()}
            label={(() => {
              switch (sektor) {
                case Sektor.EKSTERN: {
                  return (
                    <span
                      css={css`
                        display: inline-flex;
                        gap: var(--a-spacing-2);
                      `}
                    >
                      Sluttdato
                      <HelpText strategy="fixed">
                        Bruk datoen for normal avvikling av avtalen. Opsjoner
                        skal ikke beregnes med her.
                      </HelpText>
                    </span>
                  );
                }
                case Sektor.NAV_KOMMUNAL:
                case Sektor.NAV_STATLIG: {
                  return "Sluttdato (valgfritt)";
                }
              }
            })()}
            ref={gyldigTomController.field.ref}
          />
        </DatePicker>
      </FullWidthDatePickerWrapper>
    </div>
  );
}

function InnerOrgEnhetSelect() {
  const { watch } = useFormContext();

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

  const orgEnheter = searchOrgenheterQuery.data?.searchOrgEnhet || [];

  const selectedOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === watch("orgEnhetId"),
  );

  return (
    <>
      <OrgEnhetSelectFormPart name="orgEnhetId" orgEnheter={orgEnheter} />
      {selectedOrgEnhet ? (
        <div
          css={css`
            display: flex;
            gap: var(--a-spacing-4);
          `}
        >
          <span>
            Leder: <b>{selectedOrgEnhet.leder[0]?.ressurs.visningsnavn}</b>
          </span>
          <div
            css={css`
              display: inline-flex;
              gap: var(--a-spacing-2);
            `}
          >
            <span>
              Enhet i Remedy: <b>{selectedOrgEnhet.remedyEnhetId ?? "-"}</b>
            </span>
            <HelpText>
              Brukes blant annet til å opprette trygdeident og er nødvendig for
              enkelte tilganger. Utledes automatisk basert på
              organisasjonsenheten medarbeideren tilhører.
            </HelpText>
          </div>
        </div>
      ) : undefined}
    </>
  );
}
