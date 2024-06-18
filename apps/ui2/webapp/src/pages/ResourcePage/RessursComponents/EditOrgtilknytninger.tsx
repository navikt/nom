import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Buildings3Icon,
  FloppydiskIcon,
  MinusCircleIcon,
  PlusCircleIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  Checkbox,
  DatePicker,
  ExpansionCard,
  Heading,
  HelpText,
  HStack,
  Label,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { addYears } from "date-fns";
import partition from "lodash/partition";
import React, { useEffect } from "react";
import type { SubmitHandler } from "react-hook-form";
import {
  FormProvider,
  useController,
  useFieldArray,
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
import {
  getOrgEnhet,
  getOrgTilknytninger,
  oppdaterOrgtilknytningerForRessurs,
} from "~/api/react-query-endpoints";
import { useHasWriteAccess } from "~/api/useGetSession";
import { ApiResponseError } from "~/components/ApiResponseError";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { showToast } from "~/hooks/showToast";
import { dolValidering } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { OrgtilknytningTimeline } from "~/pages/ResourcePage/RessursComponents/OrgtilknytningTimeline";
import infoikonNOM from "~/resources/infoikonNOM.svg";
import type {
  OppdaterOrgtilknytningerForRessursRequest,
  OrgtilknytningState,
  Ressurs,
} from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import {
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
  isSameDateOrAfter,
} from "~/utils";

export function EditOrgtilknytninger({ ressurs }: { ressurs: Ressurs }) {
  const ressursOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = ressursOrgtilknytningerQuery.data ?? [];

  return (
    <div>
      <Heading
        css={css`
          display: flex;
          align-items: center;
        `}
        size="large"
      >
        {formatVisningsnavn(ressurs.visningsnavn)}
      </Heading>
      <Label
        css={css`
          font-weight: var(--a-font-weight-regular);
        `}
        size="small"
      >
        NAV-ident: <b>{ressurs.navident}</b>
      </Label>

      <Heading
        css={css`
          display: flex;
          align-items: center;
          margin-top: var(--a-spacing-8);
        `}
        size="small"
        spacing
      >
        <Buildings3Icon title="a11y-title" />
        Organisatorisk tilhørighet
      </Heading>

      <OrgtilknytningForm
        orgtilknytninger={orgtilknytninger}
        ressurs={ressurs}
      />
    </div>
  );
}

const defaultNyOrgtilknytning = {
  erDagligOppfolging: false,
  orgEnhetId: "",
  gyldigFom: "",
  gyldigTom: "",
};

function OrgtilknytningForm({
  orgtilknytninger,
  ressurs,
}: {
  orgtilknytninger: Partial<OrgtilknytningState>[];
  ressurs: Ressurs;
}) {
  const navigate = useNavigate();
  const hasWriteAccess = useHasWriteAccess();

  // Hvis det ikke finnes noen orgtilknytninger, legger vi til en ny en by default.
  const defaultOrgtilknytninger =
    orgtilknytninger.length > 0 ? orgtilknytninger : [defaultNyOrgtilknytning];

  const methods = useForm<{
    mappedeOrgtilknytninger: Partial<OrgtilknytningState>[];
  }>({
    defaultValues: {
      mappedeOrgtilknytninger: defaultOrgtilknytninger,
    },
    // @ts-expect-error -- Type inferred fra resolver er typen etter submit der flere felter er required, men under utfylling av formet vil flere verdier kunne være undefined. Derfor bruker vi heller eksplisitt type til UseForm
    resolver: yupResolver(formValuesSchema),
    context: { ressursSektor: ressurs.sektor },
  });

  useEffect(() => {
    methods.reset({ mappedeOrgtilknytninger: orgtilknytninger });
  }, [orgtilknytninger.length]);

  const {
    register,
    formState: { errors },
    handleSubmit,
    control,
    watch,
  } = methods;

  const { fields, prepend, remove } = useFieldArray({
    control,
    name: "mappedeOrgtilknytninger",
  });

  const oppdaterOrgtilknytningerMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OppdaterOrgtilknytningerForRessursRequest
  >({
    mutationFn: async (request) => {
      return oppdaterOrgtilknytningerForRessurs(ressurs.navident, request);
    },
    onSuccess: () => {
      showToast({ text: "Lagret" });
      navigate({
        params: { id: ressurs.navident },
        to: "/ressurs/$id",
      });
    },
  });

  const onValid: SubmitHandler<{
    mappedeOrgtilknytninger: Partial<OrgtilknytningState>[];
  }> = (data) => {
    const validation = dolValidering(
      data.mappedeOrgtilknytninger as OrgtilknytningEtterValidering[],
    );

    if (
      validation.dolOverlaps.length === 0 &&
      validation.dolGaps.length === 0
    ) {
      oppdaterOrgtilknytningerMutation.mutate(
        lagOppdaterOrgtilknytningerRequest(
          data.mappedeOrgtilknytninger as OrgtilknytningEtterValidering[],
        ),
      );
    }
  };

  return (
    <FormProvider {...methods}>
      <OrgtilknytningTimeline
        orgtilknytninger={watch("mappedeOrgtilknytninger")}
      />

      <Informasjonsboks ressursSektor={ressurs.sektor} />

      <form autoComplete="false" onSubmit={handleSubmit(onValid)}>
        {hasWriteAccess && (
          <div
            css={css`
              margin-top: var(--a-spacing-9);
              display: flex;
              justify-content: end;
            `}
          >
            <Button
              icon={<PlusCircleIcon />}
              onClick={() => {
                prepend(defaultNyOrgtilknytning, {
                  focusIndex: 0,
                });
              }}
              size="small"
              type="button"
              variant="secondary"
            >
              Legg til ny
            </Button>
          </div>
        )}

        {fields.map((field, index) => {
          return (
            <div
              css={css`
                background: #fff;
                margin-top: var(--a-spacing-6);
                padding: var(--a-spacing-6);
                border-radius: 8px;
                border: 1px solid var(--a-border-subtle);
              `}
              key={field.id}
            >
              {field.fid ? (
                <EditOrgilkntyningFormPart index={index} />
              ) : (
                <NewOrgtilknytningFormPart
                  index={index}
                  key={field.id}
                  onRemove={() => remove(index)}
                />
              )}
              <HStack gap="8">
                <FormDateWrapper
                  defaultDate={field.gyldigFom ?? undefined}
                  error={
                    errors?.mappedeOrgtilknytninger?.[index]?.gyldigFom
                      ?.message ?? ""
                  }
                  label="Fra og med"
                  name={`mappedeOrgtilknytninger.${index}.gyldigFom`}
                />

                <FormDateWrapper
                  defaultDate={field.gyldigTom ?? undefined}
                  error={
                    errors?.mappedeOrgtilknytninger?.[index]?.gyldigTom
                      ?.message ?? ""
                  }
                  label="Til og med"
                  labelHelpText={
                    ressurs.sektor === Sektor.EKSTERN
                      ? "Bruk datoen for normal avvikling av avtalen. Opsjoner skal ikke beregnes med her. Datoen må være under ett år frem i tid fra dagens dato."
                      : undefined
                  }
                  name={`mappedeOrgtilknytninger.${index}.gyldigTom`}
                />
              </HStack>

              <LederLabel index={index} />

              <Checkbox
                css={css`
                  display: flex;
                  align-items: center;
                  margin-top: var(--a-spacing-2);
                `}
                {...register(
                  `mappedeOrgtilknytninger.${index}.erDagligOppfolging`,
                )}
              >
                <BodyShort>
                  Denne lederen har ansvar for den daglige oppfølgingen av
                  medarbeideren
                </BodyShort>
              </Checkbox>
            </div>
          );
        })}

        <div
          css={css`
            display: flex;
            justify-content: end;
            margin-top: var(--a-spacing-8);
            gap: var(--a-spacing-4);
          `}
        >
          <Button
            onClick={() =>
              navigate({
                search: { editOrgtilknytninger: undefined },
                params: (previous) => previous,
              })
            }
            size="small"
            variant="tertiary"
          >
            Avbryt
          </Button>

          <Button
            icon={<FloppydiskIcon aria-hidden />}
            size="small"
            type="submit"
          >
            Lagre
          </Button>
        </div>

        <div
          css={css`
            margin-top: var(--a-spacing-6);
          `}
        >
          <IllegalDolCombinationAlert />

          <ApiResponseError error={oppdaterOrgtilknytningerMutation.error} />
        </div>
      </form>
    </FormProvider>
  );
}

function NewOrgtilknytningFormPart({
  index,
  onRemove,
}: {
  index: number;
  onRemove: () => void;
}) {
  const { watch } = useFormContext();
  const field = watch(`mappedeOrgtilknytninger.${index}`);
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(field.orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(field.orgEnhetId as string),
    enabled: !!field.orgEnhetId,
  });

  return (
    <>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <Heading size="medium">Legg til ny organisasjonstilknytning</Heading>

        <Button
          icon={<MinusCircleIcon />}
          onClick={onRemove}
          size="small"
          type="button"
          variant="secondary"
        >
          Forkast
        </Button>
      </div>
      <div
        css={css`
          font-size: var(--a-font-size-large);
          font-weight: 100;
          width: 100%;
          margin-top: var(--a-spacing-4);
        `}
      >
        <FormOrgEnhetSelectWrapper
          name={`mappedeOrgtilknytninger.${index}.orgEnhetId`}
        />
      </div>

      <BodyLong
        css={css`
          margin-top: var(--a-spacing-2);
        `}
      >
        Enhet i Remedy: <b>{orgEnhetQuery.data?.remedyEnhetId}</b>
      </BodyLong>
    </>
  );
}

function EditOrgilkntyningFormPart({ index }: { index: number }) {
  const { watch } = useFormContext();
  const field = watch(`mappedeOrgtilknytninger.${index}`);

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(field.orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(field.orgEnhetId as string),
    enabled: !!field.orgEnhetId,
  });

  return (
    <>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <Heading size="medium">{orgEnhetQuery.data?.navn}</Heading>
      </div>
    </>
  );
}

function LederLabel({ index }: { index: number }) {
  const { watch } = useFormContext();
  const field = watch(`mappedeOrgtilknytninger.${index}`);

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(field.orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(field.orgEnhetId as string),
    enabled: !!field.orgEnhetId,
  });

  const lederNavn = orgEnhetQuery.data?.ledere[0]?.visningsnavn;

  return (
    <BodyLong
      css={css`
        margin-top: var(--a-spacing-6);
      `}
    >
      Leder: <b>{lederNavn ? formatVisningsnavn(lederNavn) : ""}</b>
    </BodyLong>
  );
}

function FormDateWrapper({
  name,
  defaultDate,
  label,
  error,
  triggerOthers,
  labelHelpText,
}: {
  name: string;
  defaultDate?: string;
  label: string;
  error?: string;
  triggerOthers?: string[];
  labelHelpText?: string | undefined;
}) {
  const { trigger } = useFormContext();
  const { field } = useController({
    name,
  });
  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
      await trigger(name);
      if (triggerOthers) {
        triggerOthers.map((otherField) => {
          trigger(otherField);
        });
      }
    },
    defaultSelected: defaultDate ? new Date(defaultDate) : undefined,
  });
  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={error}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label={
          <div
            css={css`
              display: flex;
              margin-top: var(--a-spacing-4);
            `}
          >
            <span
              css={css`
                display: flex;
                height: 24px;
                align-items: center;
              `}
            >
              {label}
            </span>

            {labelHelpText && (
              <HelpText
                css={css`
                  margin-left: var(--a-spacing-2);
                `}
              >
                {labelHelpText}
              </HelpText>
            )}
          </div>
        }
        size="small"
      />
    </DatePicker>
  );
}

function FormOrgEnhetSelectWrapper({ name }: { name: string }) {
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

  return <OrgEnhetSelectFormPart name={name} orgEnheter={orgEnheter} />;
}

function IllegalDolCombinationAlert() {
  const { watch } = useFormContext();

  const skalValideres = formValuesSchema.isValidSync(watch());

  if (!skalValideres) {
    return <></>;
  }

  const orgtilknytninger = watch(`mappedeOrgtilknytninger`);
  const { dolGaps, dolOverlaps } = dolValidering(orgtilknytninger ?? []);

  if (dolGaps.length === 0 && dolOverlaps.length === 0) {
    return <></>;
  }

  return (
    <Alert variant="error">
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
        `}
      >
        {dolGaps.length > 0 && (
          <div
            css={css`
              display: flex;
              flex-direction: column;
            `}
          >
            <label>
              <b>Perioder uten daglig oppfølging-leder:</b>
            </label>
            {dolGaps.map((gap) => {
              return (
                <span key={formatDisplayDate(gap.fom)}>
                  Fra <b>{formatDisplayDate(gap.fom)}</b>{" "}
                  {!!gap.tom && (
                    <>
                      til <b>{formatDisplayDate(gap.tom)}</b>
                    </>
                  )}
                </span>
              );
            })}
          </div>
        )}

        {dolOverlaps.length > 0 && (
          <div
            css={css`
              display: flex;
              flex-direction: column;
            `}
          >
            <label>
              <b>Perioder med mer enn én daglig oppfølging-leder:</b>
            </label>
            {dolOverlaps.map((overlap) => {
              return (
                <span key={formatDisplayDate(overlap.fom)}>
                  Fra <b>{formatDisplayDate(overlap.fom)}</b>{" "}
                  {!!overlap.tom && (
                    <>
                      til <b>{formatDisplayDate(overlap.tom)}</b>
                    </>
                  )}
                </span>
              );
            })}
          </div>
        )}
      </div>
    </Alert>
  );
}

function Informasjonsboks({ ressursSektor }: { ressursSektor: Sektor }) {
  return (
    <ExpansionCard
      aria-label="Veiledning for utfylling"
      css={css`
        margin-top: var(--a-spacing-9);
      `}
      defaultOpen
    >
      <ExpansionCard.Header>
        <HStack align="center" gap="4" wrap={false}>
          <div
            css={css`
              display: flex;
              justify-content: center;
            `}
          >
            <img alt="informasjonsboks" height="48px" src={infoikonNOM} />
          </div>
          <div
            css={css`
              .navds-expansioncard__title--medium {
                margin-top: unset;
              }
            `}
          >
            <ExpansionCard.Title>Veiledning for utfylling</ExpansionCard.Title>
          </div>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <InnholdInformasjonsboks ressursSektor={ressursSektor} />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function InnholdInformasjonsboks({ ressursSektor }: { ressursSektor: Sektor }) {
  return (
    <ul>
      <li>
        {ressursSektor === Sektor.EKSTERN
          ? "Du må fylle inn sluttdato, men sluttdato må være mindre enn ett år frem i tid fra dagens dato."
          : "Du trenger ikke fylle inn sluttdato på fast ansatte."}
      </li>
      <li>
        Medarbeideren må ha en organisasjonstilknytning og en leder med daglig
        oppfølging for å være registrert som medarbeider i NAV. Perioder med
        tilknytning og daglig oppfølging må derfor ligge inntil hverandre uten
        avbrekk i tid.
      </li>
      <li>
        Leder med ansvar for daglig oppfølging kan ikke ha overlappende
        tidsperioder.
      </li>
    </ul>
  );
}

function lagOppdaterOrgtilknytningerRequest(
  orgtilknytninger: OrgtilknytningEtterValidering[],
): OppdaterOrgtilknytningerForRessursRequest {
  const [eksisterendeOrgtilknytninger, nyeOrgtilknytninger] = partition(
    orgtilknytninger,
    (orgtilknytning) => orgtilknytning.fid,
  );

  return {
    eksisterendeOrgtilknytninger: eksisterendeOrgtilknytninger.map(
      (orgtilknytning) => ({
        erDagligOppfolging: orgtilknytning.erDagligOppfolging,
        fid: orgtilknytning.fid ?? "",
        gyldigFom: orgtilknytning.gyldigFom,
        gyldigTom: orgtilknytning.gyldigTom,
        tilknytningtype: orgtilknytning.orgtilknytningType,
      }),
    ),
    nyeOrgtilknytninger: nyeOrgtilknytninger.map((orgtilknytning) => ({
      erDagligOppfolging: orgtilknytning.erDagligOppfolging,
      gyldigFom: orgtilknytning.gyldigFom,
      gyldigTom: orgtilknytning.gyldigTom,
      orgEnhetId: orgtilknytning.orgEnhetId,
    })),
    setRessursSluttdato: true,
    setRessursStartdato: false,
  };
}

export type OrgtilknytningEtterValidering = yup.InferType<
  typeof orgtilknytningSchema
> &
  Partial<OrgtilknytningState>;

const orgtilknytningSchema = yup.object().shape({
  fid: yup.string(),
  erDagligOppfolging: yup.boolean().required(),
  orgEnhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup
    .string()
    .required("Obligatorisk")
    .test({
      name: "gyldigFom-maks-tid",
      test: function (value, context) {
        return validateMaxTime(value, context);
      },
    }),
  gyldigTom: yup
    .string()
    .test({
      name: "gyldigTom-required",
      message: "Obligatorisk",
      test: function (value, context) {
        const ressursSektor = context?.options?.context?.ressursSektor;

        return ressursSektor !== Sektor.EKSTERN || !!value;
      },
    })
    .test({
      name: "gyldigTom-maks-tid",
      test: function (value, context) {
        return validateMaxTime(value, context);
      },
    })
    .test({
      name: "gyldigTom-etter-fom",
      message: "Må være etter fra og med-dato",
      test: function (value, context) {
        const gyldigFom = context.parent.gyldigFom;

        return !gyldigFom || !value || new Date(value) >= new Date(gyldigFom);
      },
    }),
});

const formValuesSchema = yup.object().shape({
  mappedeOrgtilknytninger: yup.array().of(orgtilknytningSchema).required(),
});

const validateMaxTime = (
  value: string | undefined,
  context: yup.TestContext<yup.AnyObject>,
) => {
  const ressursSektor = context?.options?.context?.ressursSektor;

  const maxDate = addYears(
    new Date(),
    ressursSektor === Sektor.EKSTERN ? 1 : 5,
  );

  if (isSameDateOrAfter(new Date(value ?? ""), maxDate)) {
    return context.createError({
      message: `Må være under ${ressursSektor === Sektor.EKSTERN ? "ett" : "fem"} år frem i tid fra dagens dato`,
    });
  }

  return true;
};
