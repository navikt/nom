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
  getOrgEnheterSomKanHaRessurser,
  harLederTilgangForOrgenhetOptions,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { FysiskBrukerstedSelectFormPart } from "~/components/select/FysiskBrukerstedSelect";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { NavigationButtons } from "~/pages/CreateResourcePage/components/NavigationButtons";
import { nomDateIsSameOrAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { defaultFormStyle, fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import { Sektor, Tilknytningtype } from "~/types/api-models";
import { validateFormDate } from "~/types/yupUtility";
import { formatDateWithoutTimezone } from "~/utils";

export function OrganisasjonstilknytningStep() {
  const { sektor } = useSearch({
    from: "/ressurs/create/$step",
  });

  return (
    <ContainerWithHighlightedTop
      icon={opprettRessursIconRoundedBackground}
      title="Organisasjonstilknytninger"
    >
      {sektor === Sektor.EKSTERN ? (
        <CreateEksternRessurs />
      ) : (
        <CreateInternRessurs />
      )}
    </ContainerWithHighlightedTop>
  );
}

function CreateInternRessurs() {
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

    resolver: yupResolver(validationSchema),
  });

  const { handleSubmit } = methods;

  const navigerTilbake = async () => {
    const values = methods.watch();

    const validValues = pickBy(values, (value, key) => {
      try {
        validationSchema.validateSyncAt(key, { [key]: value });
        return true;
      } catch {
        return false;
      }
    });

    navigate({
      to: "/ressurs/create/$step",
      params: { step: "2" },
      search: (oldSearch) => ({
        ...oldSearch,
        ...validValues,
        gyldigTom: values.gyldigTom ?? undefined,
      }),
    }).then();
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
            search: (oldSearch) => ({
              ...oldSearch,
              ...values,
              gyldigTom: values.gyldigTom ?? undefined,
            }),
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

function CreateEksternRessurs() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });

  const {
    orgEnhetId,
    gyldigFom,
    gyldigTom,
    fysiskBrukerstedNr,
    sektor,
    tilknytningtype,
  } = useSearch({
    from: "/ressurs/create/$step",
  });

  const methods = useForm({
    defaultValues: {
      orgEnhetId: orgEnhetId,
      gyldigFom: undefined,
      gyldigTom: undefined,
      fysiskBrukerstedNr: fysiskBrukerstedNr,
    },

    context: { tilknytningtype, sektor },
    resolver: yupResolver(validationSchema),
  });

  const { handleSubmit } = methods;

  if (sektor === undefined) {
    return <Alert variant="error">Mangler informasjon om sektor</Alert>;
  }

  const navigerTilbake = async () => {
    const values = methods.watch();

    const validValues = pickBy(values, (value, key) => {
      try {
        validationSchema.validateSyncAt(
          key,
          { [key]: value },
          { context: { tilknytningtype, sektor } },
        );
        return true;
      } catch {
        return false;
      }
    });

    navigate({
      to: "/ressurs/create/$step",
      params: { step: "2" },
      search: (oldSearch) => ({ ...oldSearch, ...validValues }),
    }).then();
  };

  return (
    <FormProvider {...methods}>
      <form
        css={defaultFormStyle}
        onSubmit={handleSubmit((values) =>
          navigate({
            to: "/ressurs/create/$step",
            params: { step: "9" },
            search: (oldSearch) => ({
              ...oldSearch,
              ...values,
              gyldigTom: values.gyldigTom ?? undefined,
            }),
          }),
        )}
      >
        <OrganisasjonstilknytningInnerForm
          defaultSluttDato={
            gyldigTom ||
            (tilknytningtype === Tilknytningtype.HELFO ||
            tilknytningtype === Tilknytningtype.SKATTEETATEN
              ? undefined
              : formatDateWithoutTimezone(subDays(addYears(new Date(), 1), 1)))
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

  const hasAdminAccess = useHasAdminAccess();

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
          {hasAdminAccess || hasWriteAccessForOrgenhet ? undefined : (
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

  const { tilknytningtype } = useSearch({
    from: "/ressurs/create/$step",
  });

  const optionalExternal =
    tilknytningtype === Tilknytningtype.HELFO ||
    tilknytningtype === Tilknytningtype.SKATTEETATEN;

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
            label="Startdato (første dag iht. kontrakt)"
            ref={gyldigFomController.field.ref}
          />
        </DatePicker>
      </FullWidthDatePickerWrapper>

      <FullWidthDatePickerWrapper>
        <DatePicker {...sluttDatePicker.datepickerProps}>
          <DatePicker.Input
            {...sluttDatePicker.inputProps}
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
                      Sluttdato {optionalExternal && " (valgfritt)"}
                      <HelpText strategy="fixed">
                        Siste arbeidsdag iht. avtalen. Opsjoner skal ikke
                        beregnes med her, før de utløses. Datoen må være under
                        et år frem i tid fra dagens dato.
                      </HelpText>
                    </span>
                  );
                }
                default: {
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

  const orgEnheterSomKanHaRessurser =
    useQuery(getOrgEnheterSomKanHaRessurser).data?.filter(
      (enhet) => !enhet.navn.toLowerCase().includes("vikafossen"),
    ) || [];

  const selectedOrgEnhet = orgEnheterSomKanHaRessurser.find(
    (orgEnhet) => orgEnhet.id === watch("orgEnhetId"),
  );

  return (
    <>
      <OrgEnhetSelectFormPart
        name="orgEnhetId"
        orgEnheter={orgEnheterSomKanHaRessurser}
      />

      {selectedOrgEnhet && (
        <div
          css={css`
            display: flex;
            gap: var(--a-spacing-4);
          `}
        >
          <span>
            Leder: <b>{selectedOrgEnhet.ledere[0]}</b>
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
      )}
    </>
  );
}

const validationSchema = yup.object().shape({
  orgEnhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup
    .string()
    .required("Obligatorisk")
    .test({
      name: "gyldigFom-datovalidering",
      test: function (value, context) {
        const sektor = this?.options?.context?.sektor;
        const tilknytningtype = this?.options?.context?.tilknytningtype;

        return validateFormDate(value, context, sektor, tilknytningtype, true);
      },
    }),
  gyldigTom: yup
    .string()
    .nullable()
    .test({
      name: "gyldigTom-datovalidering",
      test: function (value, context) {
        const sektor = this?.options?.context?.sektor;
        const tilknytningtype = this?.options?.context?.tilknytningtype;

        return validateFormDate(value, context, sektor, tilknytningtype, false);
      },
    })
    .test({
      name: "gyldigTom-ikke-før-fom",
      message: "Kan ikke være før startdato",
      test: function (value, context) {
        const gyldigFom = context.parent.gyldigFom;

        return !gyldigFom || nomDateIsSameOrAfter(value, gyldigFom);
      },
    }),
  fysiskBrukerstedNr: yup.string().required("Obligatorisk"),
});
