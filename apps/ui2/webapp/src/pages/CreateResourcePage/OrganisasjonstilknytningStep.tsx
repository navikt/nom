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
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import {
  AnsettelsesTypeNameMapping,
  rowStyles,
} from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { createRessursRoute } from "~/reactRouterRoutes";
import { AnsettelsesType, Sektor } from "~/types/api-models";
import { formatDateWithoutTimezone } from "~/utils";

export function OrganisasjonstilknytningStep() {
  const { sektor } = useSearch({ from: createRessursRoute.id });
  return (
    <ContainerWithHighlightedTop title="Organisasjonstilknytninger">
      {sektor === Sektor.NAV_STATLIG ? (
        <CreateStatligRessurs />
      ) : (
        <CreateEksternRessurs />
      )}
    </ContainerWithHighlightedTop>
  );
}

function CreateStatligRessurs() {
  const navigate = useNavigate({ from: createRessursRoute.id });
  const { ansettelsesType, orgEnhetId, startDato } = useSearch({
    from: createRessursRoute.to,
  });
  const methods = useForm({
    defaultValues: {
      ansettelsesType: ansettelsesType ?? AnsettelsesType.FAST,
      orgEnhetId,
      startDato: undefined,
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
    if (startDato) {
      setValue("startDato", startDato);
    }
  }, [startDato]);

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("startDato", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: startDato ? new Date(startDato) : undefined,
  });

  return (
    <FormProvider {...methods}>
      <form
        css={formStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: createRessursRoute.to,
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
            error={errors.startDato?.message?.toString()}
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
  startDato: yup.string().required("Obligatorisk"),
  ansettelsesType: yup
    .mixed<AnsettelsesType>()
    .oneOf(Object.values(AnsettelsesType))
    .required("Obligatorisk"),
});

const eksternValidationSchema = yup.object().shape({
  orgEnhetId: yup.string().required("Obligatorisk"),
  startDato: yup.string().required("Obligatorisk"),
  sluttDato: yup.string().required("Obligatorisk"),
});

function CreateEksternRessurs() {
  const navigate = useNavigate();
  const { orgEnhetId, startDato, sluttDato } = useSearch({
    from: createRessursRoute.to,
  });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: orgEnhetId,
      startDato: undefined,
      sluttDato: undefined,
    },
    resolver: yupResolver(eksternValidationSchema),
  });

  const {
    handleSubmit,
    setValue,
    formState: { errors },
  } = methods;

  useEffect(() => {
    if (startDato) {
      setValue("startDato", startDato);
    }
  }, [startDato]);

  useEffect(() => {
    if (sluttDato) {
      setValue("sluttDato", sluttDato);
    }
  }, [sluttDato]);

  const startDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("startDato", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: startDato ? new Date(startDato) : undefined,
  });

  const sluttDatePicker = useDatepicker({
    onDateChange: (date) =>
      setValue("sluttDato", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: sluttDato ? new Date(sluttDato) : undefined,
  });

  return (
    <FormProvider {...methods}>
      <form
        css={formStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: createRessursRoute.to,
            params: { step: "9" },
            search: (oldSearch) => ({ ...oldSearch, ...values }),
          }),
        )}
      >
        <InnerOrgEnhetSelect />
        <div css={rowStyles}>
          <FullWidthDatePickerWrapper>
            <DatePicker {...startDatePicker.datepickerProps}>
              <DatePicker.Input
                {...startDatePicker.inputProps}
                error={errors.startDato?.message?.toString()}
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
                error={errors.sluttDato?.message?.toString()}
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
        <NavigationButtons />
      </form>
    </FormProvider>
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

const formStyle = css`
  display: flex;
  flex-direction: column;
  margin-top: var(--a-spacing-8);

  > div {
    margin-bottom: var(--a-spacing-6);
  }
`;
