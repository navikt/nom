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
  Select,
  Tag,
  useDatepicker,
} from "@navikt/ds-react";
import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import { addYears, subMonths } from "date-fns";
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
  getOrgEnhet,
  getOrgEnheterSomKanHaRessurser,
  getOrgTilknytninger,
  oppdaterOrgtilknytningerForRessurs,
  ressurserKeys,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasAdminOrWriteAccess,
} from "~/api/useGetSession";
import { ApiResponseError } from "~/components/ApiResponseError";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import { useModal } from "~/components/modalProvider/ModalProvider";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { showToast } from "~/hooks/showToast";
import {
  SektorNameMapping,
  tilknytningstyperEksterne,
  tilknytningstyperStatlige,
  TilknytningtypeNameMapping,
} from "~/pages/CreateResourcePage/SektorStep";
import {
  dolValidering,
  otCustomStortByDate,
  setRessursStartdato,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { RemedyEndringModal } from "~/pages/ResourcePage/RessursComponents/Modal/RemedyEndringModal";
import { TomDatoModal } from "~/pages/ResourcePage/RessursComponents/Modal/TomDatoModal";
import { OrgtilknytningTimeline } from "~/pages/ResourcePage/RessursComponents/OrgtilknytningTimeline";
import infoikonNOM from "~/resources/infoikonNOM.svg";
import type {
  OppdaterOrgtilknytningerForRessursRequest,
  OrgEnhet,
  OrgtilknytningState,
  Ressurs,
} from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { Sektor, Tilknytningtype } from "~/types/api-models";
import {
  dateWithoutTimezone,
  earliestDateInList,
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
  isAfterToday,
  isBeforeDate,
  isBeforeToday,
  isGyldighetsPeriodeActive,
  isSameDateOrAfter,
  isSameDateOrBefore,
  todayWithoutTimezone,
} from "~/utils";

export function EditOrgtilknytninger({ ressurs }: { ressurs: Ressurs }) {
  const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();

  if (!hasAdminOrWriteAccess) {
    return;
  }

  const ressursOrgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const hentedeOrgtilknytninger = ressursOrgtilknytningerQuery.data ?? [];

  const orgtilknytninger = hentedeOrgtilknytninger.sort((a, b) =>
    otCustomStortByDate(a, b),
  );

  const orgEnhetIdListe = [
    ...new Set(
      orgtilknytninger.flatMap(
        (orgtilknytning) => orgtilknytning.orgEnhetId || [],
      ),
    ),
  ];

  const orgEnheter = useQueries({
    queries: orgEnhetIdListe.map((id) => ({
      queryKey: getOrgEnhet.queryKey(id),
      queryFn: () => getOrgEnhet.queryFn(id),
    })),
    combine: (results) => results.flatMap((r) => r.data ?? []),
  });

  const dolOrgtilknytninger = orgtilknytninger.filter((orgtilknytning) => {
    return orgtilknytning.erDagligOppfolging;
  });

  const dagensDolOrgtilknytning = dolOrgtilknytninger.find((tilknytning) => {
    if (
      !!tilknytning.gyldigFom &&
      isSameDateOrBefore(
        new Date(tilknytning.gyldigFom),
        todayWithoutTimezone(),
      ) &&
      (!tilknytning.gyldigTom ||
        isSameDateOrAfter(
          new Date(tilknytning.gyldigTom),
          todayWithoutTimezone(),
        ))
    ) {
      return true;
    }
  });

  const dagensDolOrgEnhet = orgEnheter.find(
    (orgEnhet) => orgEnhet.id === dagensDolOrgtilknytning?.orgEnhetId,
  );

  const dagensRemedyEnhetId = dagensDolOrgEnhet?.remedyEnhetId;

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
        dagensDolOrgEnhet={dagensDolOrgEnhet}
        dagensRemedyEnhetId={dagensRemedyEnhetId}
        orgtilknytninger={orgtilknytninger}
        ressurs={ressurs}
      />

      <ModalComponent />
    </div>
  );
}

function defaultNyOrgtilknytning(sektor: Sektor) {
  return {
    erDagligOppfolging: false,
    orgEnhetId: "",
    gyldigFom: "",
    gyldigTom: "",
    sektor: sektor,
    orgtilknytningType:
      sektor === Sektor.NAV_KOMMUNAL
        ? Tilknytningtype.INGEN
        : (sektor === Sektor.NAV_STATLIG
            ? tilknytningstyperStatlige
            : tilknytningstyperEksterne)[0],
  };
}

function OrgtilknytningForm({
  orgtilknytninger,
  ressurs,
  dagensRemedyEnhetId,
  dagensDolOrgEnhet,
}: {
  orgtilknytninger: Partial<OrgtilknytningState>[];
  ressurs: Ressurs;
  dagensRemedyEnhetId: string | undefined;
  dagensDolOrgEnhet: OrgEnhet | undefined;
}) {
  const hasAdminOrWriteAccess = useHasAdminOrWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const navigate = useNavigate();

  const nomSharepointLenke: string =
    "https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Organisasjonsmaster-(NOM).aspx";

  const lonnsskjemaLenke: string =
    "https://navno.sharepoint.com/sites/intranett-lonnsadministrasjon/SitePages/L%C3%B8nnsskjema.aspx";

  const tidligsteOrgtilknytningStartdato = earliestDateInList(
    orgtilknytninger
      .filter((orgtilknytning) => orgtilknytning.gyldigFom)
      .map((orgtilknytning) => new Date(orgtilknytning.gyldigFom as string)),
  );

  const tidligsteOrgDatoPre = tidligsteOrgtilknytningStartdato
    ? formatDateWithoutTimezone(tidligsteOrgtilknytningStartdato)
    : undefined;

  const hasOtTomChanged = (orgtilknytning: Partial<OrgtilknytningState>) => {
    const originalOt = orgtilknytninger.find(
      (tilknytning) => tilknytning.fid === orgtilknytning.fid,
    );

    return !(orgtilknytning.gyldigTom === originalOt?.gyldigTom);
  };

  // Hvis det ikke finnes noen orgtilknytninger, legger vi automatisk til en ny
  const defaultOrgtilknytninger =
    orgtilknytninger.length > 0
      ? orgtilknytninger
      : [defaultNyOrgtilknytning(ressurs.sektor)];

  const methods = useForm<{
    mappedeOrgtilknytninger: Partial<OrgtilknytningState>[];
  }>({
    defaultValues: {
      mappedeOrgtilknytninger: defaultOrgtilknytninger,
    },
    // @ts-expect-error -- Type inferred fra resolver er typen etter submit der flere felter er required, men under utfylling av formet vil flere verdier kunne være undefined. Derfor bruker vi heller eksplisitt type til UseForm
    resolver: yupResolver(formValuesSchema),
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

  const queryClient = useQueryClient();

  const oppdaterOrgtilknytningerMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OppdaterOrgtilknytningerForRessursRequest
  >({
    mutationFn: async (request) => {
      return oppdaterOrgtilknytningerForRessurs(ressurs.navident, request);
    },
    onSuccess: async () => {
      showToast({ text: "Lagret" });

      await queryClient.invalidateQueries({
        queryKey: ressurserKeys.id(ressurs.navident),
      });

      navigate({
        params: { id: ressurs.navident },
        to: "/ressurs/$id",
      }).then();
    },
  });

  const { showModal } = useModal();

  const visBekreftelsesmodaler = async (
    orgtilknytninger: Partial<OrgtilknytningState>[],
  ) => {
    await showModal(
      <RemedyEndringModal
        dagensDolOrgEnhet={dagensDolOrgEnhet}
        dagensRemedyEnhetId={dagensRemedyEnhetId}
        navn={formatVisningsnavn(ressurs.visningsnavn)}
        orgtilknytninger={orgtilknytninger}
      />,
    );

    await showModal(
      <TomDatoModal
        navn={formatVisningsnavn(ressurs.visningsnavn)}
        orgtilknytninger={orgtilknytninger}
      />,
    );
  };

  const onValid: SubmitHandler<{
    mappedeOrgtilknytninger: Partial<OrgtilknytningState>[];
  }> = async (values) => {
    const validation = dolValidering(
      values.mappedeOrgtilknytninger as OrgtilknytningEtterValidering[],
    );

    if (
      validation.dolGaps.length === 0 &&
      validation.dolOverlaps.length === 0
    ) {
      await visBekreftelsesmodaler(values.mappedeOrgtilknytninger).then(() => {
        oppdaterOrgtilknytningerMutation.mutate(
          lagOppdaterOrgtilknytningerRequest(
            values.mappedeOrgtilknytninger as OrgtilknytningEtterValidering[],
            setRessursStartdato(
              ressurs,
              values.mappedeOrgtilknytninger,
              tidligsteOrgDatoPre,
            ),
            ressurs.sektor,
          ),
        );
      });
    }
  };

  return (
    <FormProvider {...methods}>
      <Informasjonsboks ressursSektor={ressurs.sektor} />
      <OrgtilknytningTimeline
        orgtilknytninger={watch("mappedeOrgtilknytninger")}
      />

      <form autoComplete="false" onSubmit={handleSubmit(onValid)}>
        {hasAdminOrWriteAccess && (
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
                prepend(defaultNyOrgtilknytning(ressurs.sektor), {
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
          const gyldigTom = watch(`mappedeOrgtilknytninger.${index}.gyldigTom`);
          const gyldigFom = watch(`mappedeOrgtilknytninger.${index}.gyldigFom`);

          const existingOt = !!field.fid;

          const avsluttet =
            !!gyldigTom &&
            isBeforeToday(dateWithoutTimezone(new Date(gyldigTom)));

          const overToMndGammel =
            !!field.gyldigTom &&
            isBeforeDate(
              dateWithoutTimezone(new Date(field.gyldigTom)),
              dateWithoutTimezone(subMonths(todayWithoutTimezone(), 2)),
            );

          const fremtidig = !!gyldigFom && isAfterToday(gyldigFom);

          const editOtDisabled =
            existingOt &&
            !hasOtTomChanged(field) &&
            overToMndGammel &&
            !hasAdminAccess;

          return (
            <div
              css={css`
                background: ${avsluttet ? "var(--a-bg-subtle)" : "#fff"};
                margin-top: var(--a-spacing-6);
                padding: var(--a-spacing-6);
                border-radius: 8px;
                border: 1px solid var(--a-border-subtle);
              `}
              key={field.id}
            >
              {existingOt ? (
                <EditOrgilkntyningFormPart
                  avsluttet={avsluttet}
                  fremtidig={fremtidig}
                  index={index}
                />
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
                  disabled={editOtDisabled}
                  error={
                    errors?.mappedeOrgtilknytninger?.[index]?.gyldigFom
                      ?.message ?? ""
                  }
                  label="Fra og med"
                  name={`mappedeOrgtilknytninger.${index}.gyldigFom`}
                  triggerOthers={[`mappedeOrgtilknytninger.${index}.gyldigTom`]}
                />

                <FormDateWrapper
                  defaultDate={field.gyldigTom ?? undefined}
                  disabled={editOtDisabled}
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
                  triggerOthers={[
                    `mappedeOrgtilknytninger.${index}.sektor`,
                    `mappedeOrgtilknytninger.${index}.orgtilknytningType`,
                  ]}
                />
              </HStack>

              <LederLabel index={index} />

              <Checkbox
                css={css`
                  display: flex;
                  align-items: center;
                `}
                disabled={editOtDisabled}
                {...register(
                  `mappedeOrgtilknytninger.${index}.erDagligOppfolging`,
                )}
              >
                <BodyShort>
                  Denne lederen har ansvar for den daglige oppfølgingen av
                  medarbeideren
                </BodyShort>
              </Checkbox>

              <div
                css={css`
                  display: flex;
                  margin-top: var(--a-spacing-4);
                `}
              >
                <FormSektorSelectWrapper
                  disabled={editOtDisabled}
                  error={
                    errors?.mappedeOrgtilknytninger?.[index]?.sektor?.message ??
                    ""
                  }
                  name={`mappedeOrgtilknytninger.${index}.sektor`}
                  tilknytningstypeField={`mappedeOrgtilknytninger.${index}.orgtilknytningType`}
                  triggerOthers={[`mappedeOrgtilknytninger.${index}.gyldigTom`]}
                />

                <FormTilknytningstypeSelectWrapper
                  disabled={editOtDisabled}
                  error={
                    errors?.mappedeOrgtilknytninger?.[index]?.orgtilknytningType
                      ?.message ?? ""
                  }
                  name={`mappedeOrgtilknytninger.${index}.orgtilknytningType`}
                  selectedSektor={watch(
                    `mappedeOrgtilknytninger.${index}.sektor`,
                  )}
                />
              </div>
            </div>
          );
        })}

        {ressurs.sektor === Sektor.NAV_KOMMUNAL && (
          <Alert
            css={css`
              margin-top: var(--a-spacing-8);
            `}
            variant="info"
          >
            Du er i ferd med å redigere en kommunal medarbeider. Hvis
            medarbeideren er{" "}
            <Link to={nomSharepointLenke}>
              leder eller har utvidede tilganger i Unit4
            </Link>
            , må du også melde inn de samme endringene{" "}
            <Link to={lonnsskjemaLenke}>
              via eksisterende rutiner til Unit4
            </Link>
            .
          </Alert>
        )}

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
            data-cy="lagre-button"
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

        <ModalComponent />
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
        <Heading data-cy="ny-orgtilknytning-header" size="medium">
          Legg til ny organisasjonstilknytning
        </Heading>

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

function EditOrgilkntyningFormPart({
  index,
  avsluttet,
  fremtidig,
}: {
  index: number;
  avsluttet: boolean;
  fremtidig: boolean;
}) {
  const { watch } = useFormContext();
  const field = watch(`mappedeOrgtilknytninger.${index}`);

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(field.orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(field.orgEnhetId as string),
    enabled: !!field.orgEnhetId,
  });

  return (
    <div>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <Heading size="medium">{orgEnhetQuery.data?.navn}</Heading>

        {avsluttet && (
          <Tag size="medium" variant="warning">
            Avsluttet
          </Tag>
        )}

        {fremtidig && (
          <Tag size="medium" variant="success">
            Fremtidig
          </Tag>
        )}
      </div>

      <BodyLong>
        Enhet i Remedy: <b>{orgEnhetQuery.data?.remedyEnhetId}</b>
      </BodyLong>
    </div>
  );
}

function FormOrgEnhetSelectWrapper({ name }: { name: string }) {
  const orgEnheterSomKanHaRessurser =
    useQuery(getOrgEnheterSomKanHaRessurser).data || [];
  return (
    <OrgEnhetSelectFormPart
      name={name}
      orgEnheter={orgEnheterSomKanHaRessurser}
    />
  );
}

function FormDateWrapper({
  name,
  defaultDate,
  label,
  error,
  triggerOthers,
  labelHelpText,
  disabled,
}: {
  name: string;
  defaultDate?: string;
  label: string;
  error?: string;
  triggerOthers?: string[];
  labelHelpText?: string | undefined;
  disabled?: boolean;
}) {
  const { trigger } = useFormContext();
  const { field } = useController({
    name,
  });
  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);

      await trigger(name);

      if (triggerOthers && triggerOthers.length > 0) {
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
        data-cy={label.replaceAll(" ", "-")}
        disabled={disabled}
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

function FormSektorSelectWrapper({
  name,
  error,
  disabled,
  triggerOthers,
  tilknytningstypeField,
}: {
  name: string;
  error: string;
  disabled?: boolean;
  triggerOthers?: string[];
  tilknytningstypeField: string;
}) {
  const { trigger, setValue } = useFormContext();
  const { field } = useController({
    name,
  });

  if (disabled && !field.value) {
    return;
  }

  return (
    <div
      css={css`
        display: flex;
      `}
    >
      <Label
        css={css`
          margin-top: 6px;
        `}
        size="small"
      >
        Sektor:{disabled ? ` ${SektorNameMapping[field.value as Sektor]}` : ""}
      </Label>

      {!disabled && (
        <Select
          css={css`
            margin-left: var(--a-spacing-1);
            height: fit-content;
          `}
          error={error}
          hideLabel
          label="Sektor"
          onChange={async (value) => {
            field.onChange(value);

            await trigger(name);

            if (triggerOthers && triggerOthers.length > 0) {
              triggerOthers.map((otherField) => {
                trigger(otherField);
              });
            }

            const newValue = value.target.value;

            if (newValue === Sektor.NAV_KOMMUNAL) {
              setValue(tilknytningstypeField, Tilknytningtype.INGEN);
            } else {
              setValue(
                tilknytningstypeField,
                (newValue === Sektor.NAV_STATLIG
                  ? tilknytningstyperStatlige
                  : tilknytningstyperEksterne)[0],
              );
            }
          }}
          ref={field.ref}
          size="small"
          value={field.value}
        >
          {!field.value && <option value="" />}
          {Object.values(Sektor).map((sektor) => {
            return (
              <option key={sektor} value={sektor}>
                {SektorNameMapping[sektor]}
              </option>
            );
          })}
        </Select>
      )}
    </div>
  );
}

function FormTilknytningstypeSelectWrapper({
  name,
  error,
  disabled,
  triggerOthers,
  selectedSektor,
}: {
  name: string;
  error: string;
  disabled?: boolean;
  triggerOthers?: string[];
  selectedSektor: Sektor | undefined;
}) {
  const { trigger } = useFormContext();

  const { field } = useController({
    name,
  });

  const hasAdminAccess = useHasAdminAccess();

  const navident = useGetSession().data?.navident;

  // TODO: Forbedre logikk; sjekke om navident har redigeringstilgang til enhet i direktoratet
  const dinOrgenhetId = useQuery({
    queryKey: getOrgTilknytninger.queryKey(navident as string),
    queryFn: () => getOrgTilknytninger.queryFn(navident as string),
    select: (organiseringer) =>
      organiseringer
        .filter((organisering) =>
          isGyldighetsPeriodeActive(
            organisering.gyldigFom,
            organisering.gyldigTom,
          ),
        )
        .find((organisering) => organisering.erDagligOppfolging)?.orgEnhetId,
  }).data;

  const brukerJobberIDirektoratet = useQuery({
    queryKey: getOrgEnhet.queryKey(dinOrgenhetId as string),
    queryFn: () => getOrgEnhet.queryFn(dinOrgenhetId as string),
    enabled: !!dinOrgenhetId,
    select: (orgEnhet) => orgEnhet.orgEnhetsType === OrgEnhetsType.DIREKTORAT,
  }).data;

  if (
    (disabled && !field.value) ||
    !selectedSektor ||
    selectedSektor === Sektor.NAV_KOMMUNAL ||
    (selectedSektor === Sektor.NAV_STATLIG &&
      field.value === Tilknytningtype.INGEN)
  ) {
    return;
  }

  return (
    <div
      css={css`
        display: flex;
        margin-left: var(--a-spacing-8);
      `}
    >
      <Label
        css={css`
          margin-top: 6px;
        `}
        size="small"
      >
        Tilknytningstype:
        {disabled
          ? ` ${TilknytningtypeNameMapping[field.value as Tilknytningtype]}`
          : ""}
      </Label>

      {!disabled && (
        <Select
          css={css`
            margin-left: var(--a-spacing-1);
            height: fit-content;
          `}
          error={error}
          hideLabel
          label={selectedSektor}
          onChange={async (value) => {
            field.onChange(value);

            await trigger(name);

            if (triggerOthers && triggerOthers.length > 0) {
              triggerOthers.map((otherField) => {
                trigger(otherField);
              });
            }
          }}
          ref={field.ref}
          size="small"
          value={field.value ?? ""}
        >
          {(!field.value ||
            (selectedSektor === Sektor.NAV_STATLIG &&
              field.value === Tilknytningtype.INGEN)) && <option value="" />}

          {(selectedSektor === Sektor.EKSTERN
            ? tilknytningstyperEksterne
            : tilknytningstyperStatlige
          ).map((tilknytningstype) => {
            if (
              tilknytningstype === Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER &&
              !brukerJobberIDirektoratet &&
              !hasAdminAccess
            ) {
              return;
            }

            return (
              <option key={tilknytningstype} value={tilknytningstype}>
                {TilknytningtypeNameMapping[tilknytningstype]}
              </option>
            );
          })}
        </Select>
      )}
    </div>
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

function IllegalDolCombinationAlert() {
  const { watch } = useFormContext();

  const skalValideres = formValuesSchema.isValidSync(watch());

  if (!skalValideres) {
    return <></>;
  }

  const orgtilknytninger = watch("mappedeOrgtilknytninger");

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
        margin-bottom: var(--a-spacing-9);
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
  setRessursStartdato: boolean,
  ressursSektor: Sektor,
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
        tilknytningtype:
          orgtilknytning.sektor === Sektor.NAV_KOMMUNAL
            ? Tilknytningtype.INGEN
            : orgtilknytning.orgtilknytningType,
        sektor: orgtilknytning.sektor,
      }),
    ),
    nyeOrgtilknytninger: nyeOrgtilknytninger.map((orgtilknytning) => ({
      erDagligOppfolging: orgtilknytning.erDagligOppfolging,
      gyldigFom: orgtilknytning.gyldigFom,
      gyldigTom: orgtilknytning.gyldigTom,
      orgEnhetId: orgtilknytning.orgEnhetId,
      tilknytningtype:
        orgtilknytning.sektor === Sektor.NAV_KOMMUNAL
          ? Tilknytningtype.INGEN
          : orgtilknytning.orgtilknytningType,
      sektor: orgtilknytning.sektor ?? ressursSektor,
    })),
    setRessursSluttdato: true,
    setRessursStartdato: setRessursStartdato,
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
    .nullable()
    .test({
      name: "gyldigTom-required",
      message: "Obligatorisk for eksterne",
      test: function (value, context) {
        const otSektor = context?.parent?.sektor;

        return otSektor !== Sektor.EKSTERN || !!value;
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
  sektor: yup.mixed().test({
    name: "sektor-obligatorisk-fremtid-aktiv",
    message: "Obligatorisk",
    test: function (value, context) {
      const otTom = context?.parent?.gyldigTom ?? undefined;

      const required = otTom ? !isBeforeToday(otTom) : true;

      return required ? !!value : true;
    },
  }),
  orgtilknytningType: yup
    .mixed()
    .nullable()
    .test({
      name: "tilknytningstype-obligatorisk-fremtid-aktiv",
      message: "Obligatorisk",
      test: function (value, context) {
        const otSektor = context?.parent?.sektor ?? undefined;

        if (otSektor === Sektor.NAV_KOMMUNAL) {
          return true;
        }

        const otTom = context?.parent?.gyldigTom ?? undefined;

        const required = (otTom ? !isBeforeToday(otTom) : true) && !!otSektor;

        return required ? !!value : true;
      },
    }),
});

const formValuesSchema = yup.object().shape({
  mappedeOrgtilknytninger: yup.array().of(orgtilknytningSchema).required(),
});

const validateMaxTime = (
  value: string | undefined | null,
  context: yup.TestContext<yup.AnyObject>,
) => {
  const otSektor = context?.parent?.sektor;

  const maxDate = dateWithoutTimezone(
    addYears(todayWithoutTimezone(), otSektor === Sektor.EKSTERN ? 1 : 5),
  );

  if (
    (otSektor === Sektor.EKSTERN && !value) ||
    (value && isSameDateOrAfter(dateWithoutTimezone(value), maxDate))
  ) {
    return context.createError({
      message: `Må være under ${otSektor === Sektor.EKSTERN ? "ett" : "fem"} år frem i tid fra dagens dato${otSektor === Sektor.EKSTERN ? " for eksterne medarbeidere" : ", eller ikke satt"}`,
    });
  }

  return true;
};
