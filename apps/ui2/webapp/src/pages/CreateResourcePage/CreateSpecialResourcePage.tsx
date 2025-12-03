import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import {
  Alert,
  BodyLong,
  BodyShort,
  Button,
  DatePicker,
  HelpText,
  HGrid,
  Label,
  Loader,
  Modal,
  Search,
  Select,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import React, { useState } from "react";
import {
  FormProvider,
  useController,
  useForm,
  useFormContext,
} from "react-hook-form";
import type { InferType } from "yup";
import * as yup from "yup";

import {
  createRessurs,
  getOrgEnhet,
  getOrgEnheterSomKanHaRessurser,
  getPerson,
} from "~/api/react-query-endpoints";
import {
  useHasAdminAccess,
  useHasRobotAccess,
  useHasUtenlandskKonsulentAccess,
  useHasVikafossenAccess,
} from "~/api/useGetSession";
import { ApiResponseError } from "~/components/ApiResponseError";
import ContentWrapper from "~/components/ContentWrapper";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { LabelWithValue } from "~/components/LabelWithValue";
import ModalComponent from "~/components/modalProvider/ModalComponent";
import {
  ModalProvider,
  useModal,
} from "~/components/modalProvider/ModalProvider";
import { PageBanner } from "~/components/PageBanner";
import { OrgEnhetSelectFormPart } from "~/components/select/OrgEnhetSelect";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  SektorNameMapping,
  TilknytningtypeNameMapping,
} from "~/pages/CreateResourcePage/SektorStep";
import { nomDateIsSameOrAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { fullwidthRow } from "~/resources/NomStyles";
import opprettRessursIconRoundedBackground from "~/resources/opprettRessursIconRoundedBakground.svg";
import type {
  GetPersonRequest,
  OpprettRessursRequest,
  OpprettRessursResponse,
} from "~/types/api-models";
import { Tilknytningtype } from "~/types/api-models";
import { AnsettelsesType, Sektor } from "~/types/api-models";
import { IdentType, type Person } from "~/types/api-models";
import { validateFormDate } from "~/types/yupUtility";
import {
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
  isInDevelopment,
} from "~/utils";

export function CreateSpecialResourcePage() {
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();

  if (
    !hasAdminAccess &&
    !hasRobotAccess &&
    !hasUtenlandskKonsulentAccess &&
    !hasVikafossenAccess
  ) {
    return;
  }

  return (
    <>
      <PageBanner text="Opprett annen medarbeider" />

      <ContentWrapper>
        <ContainerWithHighlightedTop icon={opprettRessursIconRoundedBackground}>
          <ModalProvider>
            <CreateSpecialResourceForm />

            <ModalComponent />
          </ModalProvider>
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
}

function CreateSpecialResourceForm() {
  const navigate = useNavigate();

  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();

  const IdentTypeAccessMap = {
    [IdentType.ROBOT]: hasRobotAccess || hasAdminAccess,
    [IdentType.UTENLANDSK_KONSULENT]:
      hasUtenlandskKonsulentAccess || hasAdminAccess,
    [IdentType.VIKAFOSSEN]: hasVikafossenAccess || hasAdminAccess,
    [IdentType.STANDARD]: false,
    [IdentType.UKJENT]: false,
  };

  const tillatteIdentTyper = spesielleIdentTyper.filter(
    (type) => IdentTypeAccessMap[type],
  );

  const methods = useForm({
    defaultValues: {
      identType: tillatteIdentTyper[0],
      identifikasjonsnummer: undefined,
      visningsfornavn: "",
      visningsmellomnavn: "",
      visningsetternavn: "",
      orgEnhetId: "",
      startDato: "",
      sluttDato: "",
    },
    resolver: yupResolver(opprettSpesiellValidationSchema),
  });

  const { handleSubmit, watch } = methods;

  const { showModal } = useModal();

  const opprettRessursMutation = useMutation<
    AxiosResponse<OpprettRessursResponse>,
    AxiosError<Error>,
    OpprettRessursRequest
  >({
    mutationFn: async (request) => {
      return createRessurs(request);
    },

    onSuccess: (opprettRessursResponse) => {
      navigate({
        to: "/ressurs/specialreceipt",
        search: () => ({
          navident: opprettRessursResponse.data.navident,
        }),
        replace: true,
      }).then();
    },
  });

  const onValid = async (
    values: InferType<typeof opprettSpesiellValidationSchema>,
  ) => {
    const request = lagRessursRequest(values);

    await showModal(<CreateSpecialConfirmModal request={request} />).then(
      () => {
        opprettRessursMutation.mutate(request);
      },
    );
  };

  const identType = watch("identType");

  const [identifikasjonsnummer, setIdentifikasjonsnummer] = useState("");

  if (opprettRessursMutation.isPending) {
    return (
      <Loader
        css={css`
          align-self: center;
        `}
        size="3xlarge"
      />
    );
  }

  return (
    <FormProvider {...methods}>
      <form onSubmit={handleSubmit(onValid)}>
        <VStack gap="8">
          <div
            css={css`
              display: flex;
              justify-content: space-between;
            `}
          >
            <div
              css={css`
                width: 40%;
              `}
            >
              <IdentTypeFormPart
                name="identType"
                tillatteIdentTyper={tillatteIdentTyper}
              />

              <IdentifikasjonsnummerSearchFormPart
                checkedIdentifikasjonsnummer={identifikasjonsnummer}
                name={"identifikasjonsnummer"}
                onChecked={(value) => {
                  setIdentifikasjonsnummer(value);
                }}
              />
            </div>

            <BodyLong
              css={css`
                width: 30%;
                background-color: var(--nom-secondary-hover);
                border-radius: 8px;
                padding: var(--a-spacing-4);
                height: fit-content;
                align-self: center;
              `}
            >
              Velg identtype og skriv inn ønsket identifikator. NOM vil
              verifisere at identifikatoren ikke er i bruk.
            </BodyLong>
          </div>

          <CreateSpecialResourceFormInner
            identType={identType as IdentType}
            identifikasjonsnummer={identifikasjonsnummer}
          />

          <ApiResponseError error={opprettRessursMutation.error} />
        </VStack>
      </form>
    </FormProvider>
  );
}

function CreateSpecialResourceFormInner({
  identType,
  identifikasjonsnummer,
}: {
  identType: IdentType;
  identifikasjonsnummer: string;
}) {
  const navigate = useNavigate();

  const personRequest: GetPersonRequest = {
    identType: identType,
    personident: identifikasjonsnummer,
  };

  const personQuery = useQuery<Person, AxiosError<Error>>({
    queryKey: getPerson.queryKey(personRequest),
    queryFn: () => getPerson.queryFn(personRequest),
    enabled: !!identifikasjonsnummer && identifikasjonsnummer !== "",
    retry: false,
  });

  const ressurs = personQuery.data;

  const erBrukt = !!ressurs;

  if (!identifikasjonsnummer) {
    return (
      <div
        css={css`
          margin-top: var(--a-spacing-4);
          display: flex;
          justify-content: end;
        `}
      >
        <Button
          css={css`
            width: 130px;
          `}
          onClick={() => navigate({ to: "/" })}
          size="small"
          type="button"
          variant="secondary"
        >
          Avbryt
        </Button>
      </div>
    );
  }

  if (personQuery.isLoading) {
    return (
      <>
        <Loader
          css={css`
            align-self: center;
          `}
          size="3xlarge"
          title={`Søker etter ${identifikasjonsnummer}`}
        />

        <div
          css={css`
            margin-top: var(--a-spacing-4);
            display: flex;
            justify-content: end;
          `}
        >
          <Button
            css={css`
              width: 130px;
            `}
            onClick={() => navigate({ to: "/" })}
            size="small"
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
        </div>
      </>
    );
  }

  if (personQuery.error && personQuery.error.status !== 404) {
    return (
      <>
        <Alert variant="error">En teknisk feil forhindret søket</Alert>

        <div
          css={css`
            margin-top: var(--a-spacing-4);
            display: flex;
            justify-content: end;
          `}
        >
          <Button
            css={css`
              width: 130px;
            `}
            onClick={() => navigate({ to: "/" })}
            size="small"
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
        </div>
      </>
    );
  }

  if (erBrukt) {
    return (
      <>
        <Alert variant="error">
          Denne identifikatoren er allerede i bruk for ressurs med Nav-ident{" "}
          <b>{ressurs?.navident}</b>. Rediger identifikatoren hvis du skal
          opprette en ny ident. Du kan se mer informasjon på{" "}
          <Link params={{ id: ressurs?.navident ?? "" }} to="/ressurs/$id">
            ressursens profilside
          </Link>
          .
        </Alert>

        <div
          css={css`
            margin-top: var(--a-spacing-4);
            display: flex;
            justify-content: end;
          `}
        >
          <Button
            css={css`
              width: 130px;
            `}
            onClick={() => navigate({ to: "/" })}
            size="small"
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
        </div>
      </>
    );
  }

  return (
    <>
      <Alert variant="success">Identifikatoren er ikke i bruk</Alert>

      {identType === IdentType.ROBOT && <RobotFormSection />}

      {identType === IdentType.UTENLANDSK_KONSULENT && (
        <UtenlandskKonsulentFormSection />
      )}

      {identType === IdentType.VIKAFOSSEN && <VikafossenFormSection />}

      <div
        css={css`
          margin-top: var(--a-spacing-4);
          display: flex;
          justify-content: end;
        `}
      >
        <Button
          css={css`
            width: 130px;
          `}
          onClick={() => navigate({ to: "/" })}
          size="small"
          type="button"
          variant="secondary"
        >
          Avbryt
        </Button>

        <Button
          css={css`
            margin-left: var(--a-spacing-6);
            width: 130px;
          `}
          size="small"
          type="submit"
        >
          Neste
        </Button>
      </div>
    </>
  );
}

function CreateSpecialConfirmModal({
  onCancel,
  onConfirm,
  request,
}: {
  onCancel?: () => void;
  onConfirm?: () => void;
  request: OpprettRessursRequest;
}) {
  const {
    personident,
    visningsfornavn,
    visningsetternavn,
    visningsmellomnavn,
    sektor,
    startDato,
    sluttDato,
    orgEnhetId,
    fysiskBrukerstedNr,
    tilknytningtype,
  } = request;

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId as string),
    enabled: !!orgEnhetId,
  });

  const selectOrgEnhet = orgEnhetQuery.data;
  const lederNavn = formatVisningsnavn(selectOrgEnhet?.ledere[0]?.visningsnavn);

  const selectedFysiskBrukersted = useFysiskBrukersted(
    fysiskBrukerstedNr ?? "",
  );

  if (orgEnhetQuery.isLoading) {
    return (
      <Modal
        css={css`
          width: 600px;
          padding: var(--a-spacing-4);
        `}
        header={{ heading: "Bekreft registrering", closeButton: false }}
        onClose={onCancel}
        open
      >
        <Modal.Body
          css={css`
            display: flex;
            flex-direction: column;
            gap: var(--a-spacing-9);
          `}
        >
          <Loader
            css={css`
              align-self: center;
            `}
            size="3xlarge"
          />
        </Modal.Body>
      </Modal>
    );
  }

  return (
    <Modal
      css={css`
        padding: var(--a-spacing-4);
      `}
      header={{ heading: "Bekreft registrering", closeButton: false }}
      onClose={onCancel}
      open
    >
      <Modal.Body
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-9);
        `}
      >
        <span
          css={css`
            max-width: 400px;
          `}
        >
          Ved å bekrefte disse detaljene vil ressursen bli registrert som
          medarbeider i NOM.
        </span>

        <HGrid columns="max-content max-content" gap="2 2">
          <LabelWithValue label="Navn" split value={formattedName} />
          <LabelWithValue
            label="Identifikasjonsnummer"
            split
            value={personident}
          />
          <LabelWithValue
            label="Sektor"
            split
            value={sektor && SektorNameMapping[sektor]}
          />
          <LabelWithValue
            label="Tilknytningstype"
            split
            value={
              tilknytningtype &&
              (tilknytningtype === Tilknytningtype.INGEN
                ? undefined
                : TilknytningtypeNameMapping[tilknytningtype])
            }
          />
          <LabelWithValue
            label="Org.enhet"
            split
            value={selectOrgEnhet?.navn}
          />
          <LabelWithValue label="Leder" split value={lederNavn} />
          <LabelWithValue
            label="Fysisk brukersted"
            split
            value={
              selectedFysiskBrukersted &&
              `${selectedFysiskBrukersted.brukerstedNavn} (${selectedFysiskBrukersted.brukerstedNr})`
            }
          />
          <LabelWithValue
            label="Remedy-enhet"
            split
            value={selectOrgEnhet?.remedyEnhetId}
          />
          <LabelWithValue
            label="Startdato (første arbeidsdag)"
            split
            value={startDato && formatDisplayDate(startDato)}
          />
          <LabelWithValue
            label="Sluttdato"
            split
            value={sluttDato && formatDisplayDate(sluttDato)}
          />
        </HGrid>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={onConfirm} type="button">
          Opprett medarbeider
        </Button>
        <Button onClick={onCancel} type="button" variant="secondary">
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

function IdentTypeFormPart({
  name,
  tillatteIdentTyper,
}: {
  name: string;
  tillatteIdentTyper: IdentType[];
}) {
  const { field, fieldState } = useController({ name });
  const { clearErrors } = useFormContext();

  return (
    <Select
      error={fieldState.error?.message}
      label="Identtype"
      onChange={(event) => {
        field.onChange(event.target.value);
        clearErrors();
      }}
      ref={field.ref}
      value={field.value}
    >
      {Object.values(tillatteIdentTyper).map((type) => (
        <option key={type} value={type}>
          {IdentTypeNameMapping[type]}
        </option>
      ))}
    </Select>
  );
}

function IdentifikasjonsnummerSearchFormPart({
  name,
  onChecked,
  checkedIdentifikasjonsnummer,
}: {
  name: string;
  onChecked: (identifikasjonsnummer: string) => void;
  checkedIdentifikasjonsnummer: string;
}) {
  const { trigger, getValues, reset, clearErrors } = useFormContext();
  const { field, fieldState } = useController({ name });

  const handleCheckClick = async () => {
    const isValid = await trigger(name);

    if (isValid) {
      onChecked(getValues(name));
    }
  };

  const handleChange = async (value: string) => {
    field.onChange(value);

    if (checkedIdentifikasjonsnummer) {
      onChecked("");

      clearErrors();
    }
  };

  return (
    <div
      css={css`
        margin-top: var(--a-spacing-8);
      `}
    >
      <Search
        autoComplete="off"
        description="Elleve tegn bestående av tall og bokstaver"
        error={fieldState.error?.message}
        hideLabel={false}
        label="Identifikator"
        onChange={handleChange}
        onClear={() => {
          reset({ identifikasjonsnummer: "" });
        }}
        variant="secondary"
      >
        <Search.Button
          iconPosition="right"
          onClick={handleCheckClick}
          type="button"
        >
          Verifisér
        </Search.Button>
      </Search>
    </div>
  );
}

function RobotFormSection() {
  return (
    <>
      <VisningsnavnFormSection identType={IdentType.ROBOT} />

      <OrgEnhetSelectFormSection />

      <DateFormSection />
    </>
  );
}

function UtenlandskKonsulentFormSection() {
  return (
    <>
      <VisningsnavnFormSection identType={IdentType.UTENLANDSK_KONSULENT} />

      <OrgEnhetSelectFormSection />

      <DateFormSection />
    </>
  );
}

function VikafossenFormSection() {
  return (
    <>
      <VikafossenVisningsnavnFormSection />

      <div>
        <Label>Organisasjonsenhet</Label>
        <BodyShort>
          Medarbeideren blir plassert i organisasjonsenheten Vikafossen.
        </BodyShort>
      </div>

      <DateFormSection />
    </>
  );
}

function VisningsnavnFormSection({ identType }: { identType: IdentType }) {
  const { register } = useFormContext();

  const visningsfornavnFieldState = useController({
    name: "visningsfornavn",
  }).fieldState;

  const visningsmellomnavnFieldState = useController({
    name: "visningsmellomnavn",
  }).fieldState;

  const visningsetternavnFieldState = useController({
    name: "visningsetternavn",
  }).fieldState;

  return (
    <div>
      <b>Visningsnavn</b>
      <BodyShort spacing>
        Visningsnavnet vil bli brukt i alle Navs applikasjoner, og fornavn og
        etternavn er obligatorisk. Visningsnavnet blir benyttet til å generere
        e-postadresse.
      </BodyShort>
      <div css={fullwidthRow}>
        <TextField
          autoComplete="off"
          error={visningsfornavnFieldState?.error?.message}
          label="Fornavn"
          type="text"
          {...register("visningsfornavn")}
        />
        <TextField
          autoComplete="off"
          error={visningsmellomnavnFieldState?.error?.message}
          label="Mellomnavn"
          type="text"
          {...register("visningsmellomnavn")}
        />
        {identType !== IdentType.ROBOT ? (
          <TextField
            autoComplete="off"
            error={visningsetternavnFieldState?.error?.message}
            label="Etternavn"
            type="text"
            {...register("visningsetternavn")}
          />
        ) : (
          <div>
            <Label>Etternavn</Label>

            <BodyShort
              css={css`
                margin-top: var(--a-spacing-5);
                margin-left: var(--a-spacing-2);
              `}
            >
              RPARobot
            </BodyShort>
          </div>
        )}
      </div>
    </div>
  );
}

function VikafossenVisningsnavnFormSection() {
  const { field, fieldState } = useController({ name: "visningsetternavn" });

  return (
    <Select
      css={css`
        width: 40%;
      `}
      error={fieldState.error?.message}
      label="Visningsnavn"
      onChange={(event) => {
        field.onChange(event.target.value);
      }}
      ref={field.ref}
      value={field.value}
    >
      <option value={""}></option>
      <option value={"Anonym"}>Nav Anonym</option>
      <option value={"Ukjent"}>Nav Ukjent</option>
      <option value={"Fiktiv"}>Nav Fiktiv</option>
      <option value={"Skjult"}>Nav Skjult</option>
    </Select>
  );
}

function OrgEnhetSelectFormSection() {
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
        name={"orgEnhetId"}
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

function DateFormSection() {
  return (
    <div
      css={css`
        display: flex;
        gap: var(--a-spacing-6);
      `}
    >
      <FullWidthDatePickerWrapper>
        <FormDateWrapper
          label="Startdato (første arbeidsdag)"
          name="startDato"
        />
      </FullWidthDatePickerWrapper>

      <FullWidthDatePickerWrapper>
        <FormDateWrapper label="Sluttdato" name="sluttDato" />
      </FullWidthDatePickerWrapper>
    </div>
  );
}

function FormDateWrapper({ name, label }: { name: string; label: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
    },
    defaultSelected: field.value ? new Date(field.value) : undefined,
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label={label}
        size="medium"
      />
    </DatePicker>
  );
}

function lagRessursRequest(values: {
  orgEnhetId?: string | undefined;
  visningsfornavn?: string | undefined;
  visningsmellomnavn?: string | undefined;
  visningsetternavn?: string | undefined;
  sluttDato?: string | null | undefined;
  identType: NonNullable<IdentType | undefined>;
  identifikasjonsnummer: string;
  startDato: string;
}): OpprettRessursRequest {
  const getVisningsEtternavn = (identType: IdentType, etternavn?: string) => {
    if (identType === IdentType.ROBOT) return "RPARobot";

    return etternavn ?? "";
  };

  const inDevelopment = isInDevelopment();

  const vikafossenOrgEnhetId = inDevelopment ? "ru151s" : "ru664y";

  return {
    identType: values.identType,
    personident: values.identifikasjonsnummer,
    orgEnhetId:
      values.identType === IdentType.VIKAFOSSEN
        ? vikafossenOrgEnhetId
        : (values.orgEnhetId ?? ""),
    startDato: values.startDato,
    sluttDato: values.sluttDato ?? "",
    visningsfornavn:
      values.identType === IdentType.VIKAFOSSEN
        ? "Nav"
        : (values.visningsfornavn ?? ""),
    visningsmellomnavn:
      values.identType === IdentType.VIKAFOSSEN
        ? ""
        : (values.visningsmellomnavn ?? ""),
    visningsetternavn: getVisningsEtternavn(
      values.identType,
      values.visningsetternavn,
    ),
    ansettelsesType: AnsettelsesType.FAST,
    fysiskBrukerstedNr: "2700",
    sektor: identTypeSektorMapping(values.identType),
    tilknytningtype: identTypeTilknytningtypeMapping(values.identType),
  };
}

const opprettSpesiellValidationSchema = yup.object().shape({
  identType: yup
    .mixed<IdentType>()
    .required("Obligatorisk")
    .oneOf(
      [IdentType.ROBOT, IdentType.VIKAFOSSEN, IdentType.UTENLANDSK_KONSULENT],
      "Ugyldig type",
    ),
  identifikasjonsnummer: yup
    .string()
    .required("Obligatorisk")
    .length(11, "Identifikasjonsnummer må bestå av 11 tegn"),
  visningsfornavn: yup.string().test({
    name: "fornavn-obligatorisk-ikke-vikafossen",
    message: "Obligatorisk",
    test: function (value, context) {
      const ressursIdentType = context?.parent?.identType;

      const required = ressursIdentType !== IdentType.VIKAFOSSEN;

      return required ? !!value : true;
    },
  }),
  visningsmellomnavn: yup.string().optional(),
  visningsetternavn: yup
    .string()
    .test({
      name: "etternavn-obligatorisk-ikke-robot",
      message: "Obligatorisk",
      test: function (value, context) {
        const ressursIdentType = context?.parent?.identType;

        const required = ressursIdentType !== IdentType.ROBOT;

        return required ? !!value : true;
      },
    })
    .test({
      name: "etternavn-ingen-mellomrom",
      message: "Etternavn kan ikke inneholde mellomrom",
      test: function (value) {
        return !value || !/\s/.test(value);
      },
    }),
  orgEnhetId: yup.string().test({
    name: "orgenhet-obligatorisk-ikke-vikafossen",
    message: "Obligatorisk",
    test: function (value, context) {
      const ressursIdentType = context?.parent?.identType;

      const required = ressursIdentType !== IdentType.VIKAFOSSEN;

      return required ? !!value : true;
    },
  }),
  startDato: yup
    .string()
    .required("Obligatorisk")
    .test({
      name: "startdato-datovalidering",
      test: function (value, context) {
        const ressursIdentType = context?.parent?.identType;

        const ressursSektor = identTypeSektorMapping(ressursIdentType);

        const ressursTilknytningtype =
          identTypeTilknytningtypeMapping(ressursIdentType);

        return validateFormDate(
          value,
          context,
          ressursSektor,
          ressursTilknytningtype,
          true,
        );
      },
    }),
  sluttDato: yup
    .string()
    .nullable()
    .test({
      name: "sluttdato-datovalidering",
      test: function (value, context) {
        const ressursIdentType = context?.parent?.identType;

        const ressursSektor = identTypeSektorMapping(ressursIdentType);

        const ressursTilknytningtype =
          identTypeTilknytningtypeMapping(ressursIdentType);

        return validateFormDate(
          value,
          context,
          ressursSektor,
          ressursTilknytningtype,
          false,
        );
      },
    })
    .test({
      name: "sluttdato-ikke-før-startdato",
      message: "Kan ikke være før startdato",
      test: function (value, context) {
        const startdato = context.parent.startDato;

        return !startdato || nomDateIsSameOrAfter(value, startdato);
      },
    }),
});

function identTypeSektorMapping(identType: IdentType): Sektor {
  return identType === IdentType.UTENLANDSK_KONSULENT ||
    identType === IdentType.VIKAFOSSEN
    ? Sektor.EKSTERN
    : Sektor.NAV_STATLIG;
}

function identTypeTilknytningtypeMapping(
  identType: IdentType,
): Tilknytningtype {
  return identType === IdentType.UTENLANDSK_KONSULENT ||
    identType === IdentType.VIKAFOSSEN
    ? Tilknytningtype.KONSULENT
    : Tilknytningtype.FAST;
}

const spesielleIdentTyper = [
  IdentType.ROBOT,
  IdentType.UTENLANDSK_KONSULENT,
  IdentType.VIKAFOSSEN,
];

const IdentTypeNameMapping = {
  [IdentType.ROBOT]: "Robot",
  [IdentType.STANDARD]: "Standard",
  [IdentType.VIKAFOSSEN]: "Vikafossen",
  [IdentType.UTENLANDSK_KONSULENT]: "Utenlandsk konsulent",
  [IdentType.UKJENT]: "Ukjent",
};
