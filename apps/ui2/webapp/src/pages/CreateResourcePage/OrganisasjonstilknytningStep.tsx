import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  BodyShort,
  Checkbox,
  DatePicker,
  HelpText,
  Radio,
  RadioGroup,
  useDatepicker,
} from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { addYears, isBefore, subDays } from "date-fns";
import { useEffect, useState } from "react";
import {
  Controller,
  FormProvider,
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
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { AnsettelsesTypeNameMapping } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { defaultFormStyle, fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { AnsettelsesType, Sektor } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

export function OrganisasjonstilknytningStep() {
  const { sektor } = useSearch({ from: "/ressurs/create/$step" });
  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIcon}
      title="Organisasjonstilknytninger"
    >
      {sektor === Sektor.NAV_STATLIG ? (
        <CreateStatligRessurs />
      ) : (
        <CreateEksternRessurs />
      )}
    </ContainerWithHighlightedTop>
  );
}

function CreateStatligRessurs() {
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
        <NavigationButtons />
      </form>
    </FormProvider>
  );
}

const statligValidationSchema = yup.object({
  orgEnhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  ansettelsesType: yup
    .mixed<AnsettelsesType>()
    .oneOf(Object.values(AnsettelsesType))
    .required("Obligatorisk"),
});

export const eksternPeriodeValidationSchema = yup.object().shape({
  gyldigFom: yup.string().required("Obligatorisk"),
  gyldigTom: yup
    .string()
    .required("Obligatorisk")
    .test("ensure-max-1-year", "Må være under et år frem i tid", (v) =>
      isBefore(new Date(v), subDays(addYears(new Date(), 1), 1)),
    ),
  setRessursSluttdato: yup.boolean(),
});

export const eksternValidationSchema = eksternPeriodeValidationSchema.concat(
  yup.object().shape({
    orgEnhetId: yup.string().required("Obligatorisk"),
  }),
);

function CreateEksternRessurs() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });
  const { orgEnhetId, gyldigFom, gyldigTom, fysiskBrukerstedNr } = useSearch({
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
      eksternValidationSchema.concat(
        yup.object().shape({
          fysiskBrukerstedNr: yup.string().required("Obligatorisk"),
        }),
      ),
    ),
  });

  const { handleSubmit } = methods;

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
        />
        <FysiskBrukerstedSelectFormPart />
        <NavigationButtons />
      </form>
    </FormProvider>
  );
}

export function OrganisasjonstilknytningInnerForm({
  defaultStartDato,
  defaultSluttDato,
  erDagligOppfolgingLockedToTrue = false,
}: {
  defaultStartDato?: string;
  defaultSluttDato?: string;
  erDagligOppfolgingLockedToTrue?: boolean;
}) {
  const {
    setValue,
    formState: { errors },
    watch,
  } = useFormContext();

  useEffect(() => {
    if (defaultStartDato) {
      setValue("gyldigFom", defaultStartDato);
    }
  }, [defaultStartDato]);

  useEffect(() => {
    if (defaultSluttDato) {
      setValue("gyldigTom", defaultSluttDato);
    }
  }, [defaultSluttDato]);

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigFom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: defaultStartDato ? new Date(defaultStartDato) : undefined,
  });

  const sluttDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigTom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: defaultSluttDato ? new Date(defaultSluttDato) : undefined,
  });

  const [erDagligOppfolgingChecked, setErDagligOppfolgingChecked] = useState(
    erDagligOppfolgingLockedToTrue,
  );

  return (
    <>
      <InnerOrgEnhetSelect />
      {watch("orgEnhetId") ? (
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
      ) : undefined}
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
              error={errors.gyldigFom?.message?.toString()}
              label={
                <span
                  css={css`
                    height: 24px;
                  `}
                >
                  Startdato (første arbeidsdag)
                </span>
              }
            />
          </DatePicker>
        </FullWidthDatePickerWrapper>
        <FullWidthDatePickerWrapper>
          <DatePicker {...sluttDatePicker.datepickerProps}>
            <DatePicker.Input
              {...sluttDatePicker.inputProps}
              error={errors.gyldigTom?.message?.toString()}
              label={
                <span
                  css={css`
                    display: inline-flex;
                    gap: var(--a-spacing-2);
                  `}
                >
                  Sluttdato
                  <HelpText strategy="fixed">
                    Bruk datoen for normal avvikling av avtalen. Opsjoner skal
                    ikke beregnes med her.
                  </HelpText>
                </span>
              }
            />
          </DatePicker>
        </FullWidthDatePickerWrapper>
      </div>
    </>
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
      <OrgEnhetSelectFormPart orgEnheter={orgEnheter} />
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
