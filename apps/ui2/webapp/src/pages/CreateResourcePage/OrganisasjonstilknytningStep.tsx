import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  DatePicker,
  HelpText,
  Radio,
  RadioGroup,
  useDatepicker,
} from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { addYears, isBefore } from "date-fns";
import { useEffect } from "react";
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
import { Divider } from "~/components/Divider";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { AnsettelsesTypeNameMapping } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { createRessursRoute } from "~/reactRouterRoutes";
import { defaultFormStyle, fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";
import { AnsettelsesType, Sektor } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

export function OrganisasjonstilknytningStep() {
  const { sektor } = useSearch({ from: createRessursRoute.fullPath });
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
  const navigate = useNavigate({ from: createRessursRoute.fullPath });
  const { ansettelsesType, orgEnhetId, gyldigFom } = useSearch({
    from: createRessursRoute.fullPath,
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
            to: createRessursRoute.id,
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
    .test("ensure-max-1-year", "Kan maks være 1 år frem i tid", (v) =>
      isBefore(new Date(v), addYears(new Date(), 1)),
    ),
});

// eslint-disable-next-line unicorn/prefer-spread
export const eksternValidationSchema = eksternPeriodeValidationSchema.concat(
  yup.object().shape({
    orgEnhetId: yup.string().required("Obligatorisk"),
  }),
);

function CreateEksternRessurs() {
  const navigate = useNavigate({ from: createRessursRoute.fullPath });
  const { orgEnhetId, gyldigFom, gyldigTom, fysiskBrukerstedNr } = useSearch({
    from: createRessursRoute.fullPath,
  });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: orgEnhetId,
      gyldigFom: undefined,
      gyldigTom: undefined,
      fysiskBrukerstedNr: fysiskBrukerstedNr,
    },

    resolver: yupResolver(
      // eslint-disable-next-line unicorn/prefer-spread
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
            to: createRessursRoute.id,
            params: { step: "9" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <OrganisasjonstilknytningInnerForm
          defaultSluttDato={
            gyldigTom || formatDateWithoutTimezone(addYears(new Date(), 1))
          }
          defaultStartDato={gyldigFom}
        />
        <Divider />
        <FysiskBrukerstedSelectFormPart description="Med fysisk brukersted menes hvor(kontor/lokasjon) medarbeideren skal arbeide." />
        <NavigationButtons />
      </form>
    </FormProvider>
  );
}

export function OrganisasjonstilknytningInnerForm({
  defaultStartDato,
  defaultSluttDato,
}: {
  defaultStartDato?: string;
  defaultSluttDato?: string;
}) {
  const {
    setValue,
    formState: { errors },
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

  return (
    <>
      <InnerOrgEnhetSelect />
      <div css={fullwidthRow}>
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
                    ikke beregnes med her
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
          <span>
            Enhet i Remedy: <b>{selectedOrgEnhet.remedyEnhetId ?? "-"}</b>
          </span>
        </div>
      ) : undefined}
    </>
  );
}
