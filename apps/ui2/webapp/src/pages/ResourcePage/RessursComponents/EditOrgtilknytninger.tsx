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
  Heading,
  HelpText,
  HStack,
  Label,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQueries, useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { addYears, isBefore } from "date-fns";
import partition from "lodash/partition";
import React from "react";
import {
  FormProvider,
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
import { etEllerAnnet } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { OrgtilknytningTimeline } from "~/pages/ResourcePage/RessursComponents/OrgtilknytningTimeline";
import type {
  OppdaterOrgtilknytningerForRessursRequest,
  OrgEnhet,
  OrgtilknytningState,
  Ressurs,
} from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import {
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
} from "~/utils";

export function EditOrgtilknytninger({ ressurs }: { ressurs: Ressurs }) {
  const ressursOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = ressursOrgtilknytningerQuery.data ?? [];

  const orgenhetIdListe = [
    ...new Set(orgtilknytninger.map((item) => item.orgEnhetId)),
  ];

  const hentedeOrgenheter = useQueries({
    queries: orgenhetIdListe.map((id) => ({
      queryKey: getOrgEnhet.queryKey(id),
      queryFn: () => getOrgEnhet.queryFn(id),
    })),
    combine: (results) => results.flatMap((r) => r.data ?? []),
  });

  if (orgtilknytninger.length === 0) {
    return <></>;
  }

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
        hentedeOrgenheter={hentedeOrgenheter}
        orgtilknytninger={orgtilknytninger}
        ressurs={ressurs}
      />
    </div>
  );
}

function OrgtilknytningForm({
  orgtilknytninger,
  hentedeOrgenheter,
  ressurs,
}: {
  orgtilknytninger: Partial<OrgtilknytningState>[];
  hentedeOrgenheter: OrgEnhet[];
  ressurs: Ressurs;
}) {
  const navigate = useNavigate();
  const hasWriteAccess = useHasWriteAccess();

  const methods = useForm<{
    mappedeOrgtilknytninger: Partial<OrgtilknytningState>[];
  }>({
    defaultValues: {
      mappedeOrgtilknytninger: orgtilknytninger,
    },
    // @ts-expect-error -- Type inferred fra resolver er typen etter submit der flere felter er required, men under utfylling av formet vil flere verdier kunne være undefined. Derfor bruker vi heller eksplisitt type til UseForm
    resolver: yupResolver(formValuesSchema),
    context: { ressursSektor: ressurs.sektor },
  });

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

  return (
    <FormProvider {...methods}>
      <OrgtilknytningTimeline
        orgtilknytninger={watch("mappedeOrgtilknytninger")}
      />
      <form
        autoComplete="false"
        onSubmit={handleSubmit((data) => {
          const validation = etEllerAnnet(
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
        })}
      >
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
                prepend({
                  erDagligOppfolging: false,
                  orgEnhetId: "",
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

              <BodyLong
                css={css`
                  margin-top: var(--a-spacing-6);
                `}
              >
                Leder:{" "}
                <b>
                  {formatVisningsnavn(
                    hentedeOrgenheter?.find(
                      (orgenhet) => orgenhet?.id === field?.orgEnhetId,
                    )?.ledere[0]?.visningsnavn,
                  ) ?? ""}
                </b>
              </BodyLong>

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
  const { setValue, trigger } = useFormContext();

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      setValue(name, date ? formatDateWithoutTimezone(date) : undefined);
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
  const { dolGaps, dolOverlaps } = etEllerAnnet(orgtilknytninger ?? []);

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

const orgtilknytningSchema = yup.object().shape({
  fid: yup.string(),
  erDagligOppfolging: yup.boolean().required(),
  gyldigFom: yup.string().required("Obligatorisk"),
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
      name: "gyldigTom-required",
      message: "Må være under ett år frem i tid fra dagens dato",
      test: function (value, context) {
        const ressursSektor = context?.options?.context?.ressursSektor;

        return (
          ressursSektor !== Sektor.EKSTERN ||
          isBefore(new Date(value ?? ""), addYears(new Date(), 1))
        );
      },
    })
    .test({
      name: "gyldigTom-after-fom",
      message: "Må være etter fra og med-dato",
      test: function (value, context) {
        const gyldigFom = context.parent.gyldigFom;

        return !gyldigFom || !value || new Date(value) >= new Date(gyldigFom);
      },
    }),
  orgEnhetId: yup.string().required("Obligatorisk"),
});

const formValuesSchema = yup.object().shape({
  mappedeOrgtilknytninger: yup.array().of(orgtilknytningSchema).required(),
});

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
