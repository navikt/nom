import { css } from "@emotion/react";
import {
  Buldings3Icon,
  FloppydiskIcon,
  PencilIcon,
  PersonIcon,
  PersonSuitIcon,
  ReceptionIcon,
} from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  DatePicker,
  ExpansionCard,
  Heading,
  HelpText,
  Label,
  Link,
  Tag,
  useDatepicker,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import {
  Link as ReactRouterLink,
  useNavigate,
  useSearch,
} from "@tanstack/react-router";
import { isAfter, isBefore } from "date-fns";
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";
import { useForm } from "react-hook-form";

import {
  getLedereForRessurs,
  getOrgEnhet,
  getOrgEnheterLedetAv,
  getOrgTilknytninger,
  getTeamcatMemberships,
  updateOrgtilknytningDato,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess, useHasWriteAccess } from "~/api/useGetSession";
import { FullWidthDatePickerWrapper } from "~/components/FullWidthDatePickerWrapper";
import { UserImage } from "~/components/UserImage";
import { showToast } from "~/hooks/showToast";
import { rowStyles } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { EditNameForm } from "~/pages/ResourcePage/EditForms/EditNameForm";
import { EditPhoneNumber } from "~/pages/ResourcePage/EditForms/EditPhoneNumber";
import { orgRoute, ressursRoute } from "~/reactRouterRoutes";
import type {
  EndreOrgtilknytningDatoRequest,
  OrgtilknytningState,
  Ressurs,
} from "~/types/api-models";
import { Sektor } from "~/types/api-models";
import {
  formatDateWithoutTimezone,
  formatDisplayDate,
  formatTlf,
  formatVisningsnavn,
  isGyldighetsPeriodeActive,
  isInDevelopment,
} from "~/utils";

export function DetailedRessurs({ ressurs }: { ressurs: Ressurs }) {
  const hasAdminAccess = useHasAdminAccess();

  return (
    <div
      css={css`
        border-radius: 5px;
        background-color: var(--nom-secondary-hover);
        border: 1px solid var(--a-gray-400);
      `}
    >
      <HorizontalEdge />
      <div
        css={css`
          padding: 2rem;

          h1,
          h2 {
            display: flex;
            gap: 0.5rem;
            margin-bottom: var(--a-spacing-6);

            svg {
              width: 24px;
              height: 24px;
            }
          }
        `}
      >
        <SummarySection ressurs={ressurs} />
        <LeaderForSection ressurs={ressurs} />
        <OrgTilknytningSection ressurs={ressurs} />
        <TeamCatSection navident={ressurs.navident} />
        {hasAdminAccess && <ToggleEditButton />}
      </div>
    </div>
  );
}

function ToggleEditButton() {
  const { edit } = useSearch({ from: ressursRoute.id });
  const navigate = useNavigate();
  if (edit) {
    return (
      <Button
        onClick={() => navigate({ search: { edit: false } })}
        size="small"
      >
        Ferdig
      </Button>
    );
  }
  return (
    <Button onClick={() => navigate({ search: { edit: true } })} size="small">
      Endre
    </Button>
  );
}

function Divider({ size = "large" }: { size?: "large" | "small" }) {
  return (
    <div
      css={[
        css`
          height: 1px;
          margin: var(--a-spacing-8) 0;
          background-color: var(--a-gray-400);
        `,
        size === "large" &&
          css`
            margin: var(--a-spacing-8) 0;
          `,
        size === "small" &&
          css`
            margin: var(--a-spacing-6) 0;
          `,
      ]}
    />
  );
}

function OrgTilknytningSection({ ressurs }: { ressurs: Ressurs }) {
  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = orgtilknytningerQuery.data ?? [];

  const { editOrgtilknytning } = useSearch({ from: ressursRoute.id });

  if (orgtilknytninger.length === 0) {
    return <></>;
  }

  const activeOrgtilknytninger = orgtilknytninger.filter((orgTilknytning) =>
    isGyldighetsPeriodeActive(
      orgTilknytning.gyldigFom,
      orgTilknytning.gyldigTom,
    ),
  );

  const expiredOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigTom &&
      isBefore(new Date(orgTilknytning.gyldigTom), new Date()),
  );

  const futureOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigFom &&
      isAfter(new Date(orgTilknytning.gyldigFom), new Date()),
  );

  return (
    <>
      <Divider />
      <Heading level="2" size="small">
        <Buldings3Icon />
        Organisatorisk tilhørighet
      </Heading>
      {activeOrgtilknytninger.map((orgTilknytning) =>
        editOrgtilknytning === orgTilknytning.fid ? (
          <EditOrgtilknytning
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
          />
        ) : (
          <OrganisationHierarchyTree
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
            ressurs={ressurs}
          />
        ),
      )}
      <FutureOrgTilknytninger
        futureOrgTilknytninger={futureOrgTilknytninger}
        ressurs={ressurs}
      />
      <ExpiredOrgTilknytninger
        expiredOrgTilknytninger={expiredOrgTilknytninger}
      />
    </>
  );
}

function FutureOrgTilknytninger({
  futureOrgTilknytninger,
  ressurs,
}: {
  futureOrgTilknytninger: OrgtilknytningState[];
  ressurs: Ressurs;
}) {
  const { editOrgtilknytning } = useSearch({ from: ressursRoute.id });

  if (futureOrgTilknytninger.length === 0) {
    return <></>;
  }

  return (
    <>
      <Heading level="2" size="xsmall">
        Fremtidige org.tilknytninger
      </Heading>
      {futureOrgTilknytninger.map((orgTilknytning) =>
        editOrgtilknytning === orgTilknytning.fid ? (
          <EditOrgtilknytning
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
          />
        ) : (
          <OrganisationHierarchyTree
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
            ressurs={ressurs}
          />
        ),
      )}
    </>
  );
}

function ExpiredOrgTilknytninger({
  expiredOrgTilknytninger,
}: {
  expiredOrgTilknytninger: OrgtilknytningState[];
}) {
  const [showAll, setShowAll] = useState(false);
  if (expiredOrgTilknytninger.length === 0) {
    return <></>;
  }

  const tilknyttingerToShow = expiredOrgTilknytninger.slice(
    0,
    showAll ? expiredOrgTilknytninger.length : 3,
  );

  const shouldDisplayShowAllButton =
    expiredOrgTilknytninger.length > 3 && !showAll;

  return (
    <>
      <Heading level="2" size="xsmall">
        Avsluttede org.tilknytninger
      </Heading>
      {tilknyttingerToShow.map((orgTilknytning) => (
        <ExpiredOrgTilknytning
          expiredOrgTilknytning={orgTilknytning}
          key={orgTilknytning.fid}
        />
      ))}
      {shouldDisplayShowAllButton && (
        <Button
          onClick={() => setShowAll(true)}
          size="small"
          variant="tertiary"
        >
          Vis alle avsluttede org.tilknytninger
        </Button>
      )}
    </>
  );
}

function ExpiredOrgTilknytning({
  expiredOrgTilknytning,
}: {
  expiredOrgTilknytning: OrgtilknytningState;
}) {
  const orgEnhet = useQuery({
    queryKey: getOrgEnhet.queryKey(expiredOrgTilknytning.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(expiredOrgTilknytning.orgEnhetId),
  }).data;

  return (
    <div
      css={css`
        display: flex;
        gap: 1rem;
        align-items: center;
        background: white;
        padding: 1.5rem;
        border: 1px solid var(--a-border-default);
        border-radius: var(--a-border-radius-large);
      `}
      key={expiredOrgTilknytning.orgEnhetId}
    >
      <Heading level="3" size="medium">
        {orgEnhet?.navn}
      </Heading>
      <span>
        {formatDisplayDate(expiredOrgTilknytning.gyldigFom)} -{" "}
        {formatDisplayDate(expiredOrgTilknytning.gyldigTom)}
      </span>
    </div>
  );
}

function EditOrgtilknytning({
  orgTilknytning,
}: {
  orgTilknytning: OrgtilknytningState;
}) {
  const methods = useForm({
    defaultValues: {
      gyldigFom: orgTilknytning.gyldigFom,
      gyldigTom: orgTilknytning.gyldigTom,
    },
  });

  const { handleSubmit, setValue } = methods;

  const startDatepicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigFom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: new Date(orgTilknytning.gyldigFom),
  });

  const sluttDatepicker = useDatepicker({
    onDateChange: (date) =>
      setValue("gyldigTom", date ? formatDateWithoutTimezone(date) : ""),
    defaultSelected: orgTilknytning.gyldigTom
      ? new Date(orgTilknytning.gyldigTom)
      : undefined,
  });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgTilknytning.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgTilknytning.orgEnhetId),
  });

  const orgtilknytningDatoMutation = useMutation<
    unknown,
    unknown,
    EndreOrgtilknytningDatoRequest
  >({
    mutationFn: async (values) =>
      updateOrgtilknytningDato(orgTilknytning.fid, values),
    onSuccess: () => {
      showToast({ text: "Lagret" });
      // TODO:
      // Sørge for at verdier lastes på nytt ved lagring
      //--- i toppen av komponenten
      // const queryClient = useQueryClient();
      //
      // --- i onSuccess på linja før navigate()
      // queryClient.invalidateQueries({ queryKey: orgtilknytningerKeys.navident(navident)});
      navigate({ search: { editOrgtilknytning: undefined } });
    },
  });

  const orgEnhet = orgEnhetQuery.data;

  const navigate = useNavigate();

  if (!orgEnhet) {
    return <></>;
  }

  const orgEnhetLeder = orgEnhet.ledere[0];

  return (
    <div
      aria-label="datoendring"
      css={css`
        margin-bottom: var(--a-spacing-8);
        background-color: var(--a-surface-default);
        border-radius: var(--a-border-radius-large);
        border: 1px solid var(--a-border-default);
        padding: var(--a-spacing-3) var(--a-spacing-4);
      `}
    >
      <Heading
        css={css`
          margin-bottom: var(--a-spacing-1);
        `}
        level="3"
        size="medium"
      >
        {orgEnhet.navn}
      </Heading>
      <Label size="small">
        <span
          css={css`
            font-weight: var(--a-font-weight-regular);
          `}
        >
          Leder:{" "}
        </span>
        {formatVisningsnavn(orgEnhetLeder?.visningsnavn)}
      </Label>
      {orgEnhet.remedyEnhetId ? (
        <Label
          css={css`
            margin-left: 14px;
          `}
          size="small"
        >
          <span
            css={css`
              font-weight: var(--a-font-weight-regular);
            `}
          >
            Enhet i Remedy:{" "}
          </span>
          {orgEnhet.remedyEnhetId}
        </Label>
      ) : undefined}
      <form
        onSubmit={handleSubmit((values) =>
          orgtilknytningDatoMutation.mutate(values),
        )}
      >
        <div
          css={css`
            display: flex;
            margin-top: var(--a-spacing-6);
            margin-bottom: var(--a-spacing-4);
            ${rowStyles}
          `}
        >
          <FullWidthDatePickerWrapper>
            <DatePicker {...startDatepicker.datepickerProps}>
              <DatePicker.Input
                {...startDatepicker.inputProps}
                label="Første arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
          <FullWidthDatePickerWrapper>
            <DatePicker {...sluttDatepicker.datepickerProps}>
              <DatePicker.Input
                {...sluttDatepicker.inputProps}
                label="Siste arbeidsdag i enheten"
              />
            </DatePicker>
          </FullWidthDatePickerWrapper>
        </div>
        {orgtilknytningDatoMutation.isError ? (
          <Alert variant="error">Noe gikk galt</Alert>
        ) : undefined}
        <span>
          Sluttdato på dagens dato eller tidligere vil avslutte tilknytningen
          med én gang.
        </span>
        <div
          css={css`
            display: flex;
            justify-content: flex-end;
            margin-top: var(--a-spacing-4);
          `}
        >
          <Button
            css={css`
              margin-right: var(--a-spacing-6);
            `}
            onClick={() =>
              navigate({ search: { editOrgtilknytning: undefined } })
            }
            size="small"
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
          <Button
            icon={<FloppydiskIcon aria-hidden />}
            size="small"
            type="submit"
            variant="primary"
          >
            Lagre
          </Button>
        </div>
      </form>
    </div>
  );
}

function OrganisationHierarchyTree({
  ressurs,
  orgTilknytning,
}: {
  orgTilknytning: OrgtilknytningState;
  ressurs: Ressurs;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgTilknytning.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgTilknytning.orgEnhetId),
  });

  const orgEnhet = orgEnhetQuery.data;

  const navigate = useNavigate();

  const { editOrgtilknytning } = useSearch({ from: ressursRoute.id });

  const hasWriteAccess = useHasWriteAccess();

  if (!orgEnhet) {
    return <></>;
  }

  const orgEnhetLeder = orgEnhet.ledere[0];

  const { gyldigTom, gyldigFom, erDagligOppfolging, fid } = orgTilknytning;

  const startsInTheFuture =
    gyldigFom && isAfter(new Date(gyldigFom), new Date());

  return (
    <ExpansionCard
      aria-label="organisasjons-hierarki"
      css={css`
        margin-bottom: 2rem;

        &.navds-expansioncard--open #title-start-date {
          color: blue;
          visibility: hidden;
        }
      `}
      size="small"
    >
      <ExpansionCard.Header>
        <ExpansionCard.Title>
          <span>{orgEnhet.navn}</span>
          {startsInTheFuture && (
            <span
              css={css`
                font-size: var(--a-font-size-large);
                font-weight: var(--a-font-weight-regular);
                margin-left: var(--a-spacing-4);
              `}
              id="title-start-date"
            >
              Starter {formatDisplayDate(gyldigFom)}
            </span>
          )}
        </ExpansionCard.Title>
        <ExpansionCard.Description>
          <Label size="small">
            <span
              css={css`
                font-weight: var(--a-font-weight-regular);
              `}
            >
              Leder:{" "}
            </span>
            {formatVisningsnavn(orgEnhetLeder?.visningsnavn)}
          </Label>
          {erDagligOppfolging && !startsInTheFuture ? (
            <Tag
              css={css`
                margin-left: 1rem;
              `}
              size="xsmall"
              variant="info"
            >
              Daglig oppfølging
            </Tag>
          ) : undefined}
        </ExpansionCard.Description>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        {gyldigFom && <span>Startdato: {formatDisplayDate(gyldigFom)}</span>}
        <br />
        {gyldigTom && <span>Sluttdato: {formatDisplayDate(gyldigTom)}</span>}
        <Divider size="small" />
        <CompactOrganisasjonHierarki orgEnhet={orgEnhet} ressurs={ressurs} />
        <div
          css={css`
            display: flex;
            justify-content: flex-end;
            margin-top: var(--a-spacing-6);
          `}
        >
          {hasWriteAccess &&
            ressurs.sektor === Sektor.EKSTERN &&
            editOrgtilknytning === undefined && (
              <Button
                icon={<PencilIcon aria-hidden />}
                onClick={() =>
                  navigate({ search: { editOrgtilknytning: fid } })
                }
                size="small"
                variant="secondary"
              >
                Endre
              </Button>
            )}
        </div>
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function LeaderForSection({ ressurs }: { ressurs: Ressurs }) {
  const lederForOrgenheter =
    useQuery({
      queryKey: getOrgEnheterLedetAv.queryKey(ressurs.navident),
      queryFn: () => getOrgEnheterLedetAv.queryFn(ressurs.navident),
    }).data ?? [];

  if (lederForOrgenheter.length === 0) {
    return <></>;
  }

  /*
   Leder høyt oppe i hierarkiet er ofte leder i flere "like" orgEnheter.
   Her antas det at første forekomst av navn for en orgEnhet er den høyest i hierarkiet.
   Se https://jira.adeo.no/browse/ORG-1437 for flere detaljer.
  */
  const uniqLederFor = uniqBy(lederForOrgenheter, (orgEnhet) => orgEnhet.navn);

  return (
    <>
      <Divider />
      <Heading level="2" size="small">
        <PersonSuitIcon />
        Leder for
      </Heading>
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: 0.5rem;
        `}
      >
        {uniqLederFor.map((orgEnhet) => (
          <ReactRouterLink
            key={orgEnhet.id}
            params={{ id: orgEnhet.id }}
            to={orgRoute.to}
          >
            <Link as="span">{orgEnhet.navn}</Link>
          </ReactRouterLink>
        ))}
      </div>
    </>
  );
}

function Name({ ressurs }: { ressurs: Ressurs }) {
  const { edit } = useSearch({ from: ressursRoute.id });

  if (edit) {
    return <EditNameForm ressurs={ressurs} />;
  }
  return (
    <Heading
      css={css`
        margin-bottom: var(--a-spacing-8);
      `}
      level="1"
      size="large"
    >
      {formatVisningsnavn(ressurs.visningsnavn)}
    </Heading>
  );
}

function Telephone({ ressurs }: { ressurs: Ressurs }) {
  const { edit } = useSearch({ from: ressursRoute.id });

  if (edit) {
    return (
      <>
        <span>Telefon:</span>

        <EditPhoneNumber ressurs={ressurs} />
      </>
    );
  }
  return (
    <RessursInformationDetail
      label="Telefon"
      value={ressurs.telefoner
        .map(({ nummer }) => formatTlf(nummer))
        .join(", ")}
    />
  );
}

function SummarySection({ ressurs }: { ressurs: Ressurs }) {
  const dagligLeder = useQuery({
    queryKey: getLedereForRessurs.queryKey(ressurs.navident),
    queryFn: () => getLedereForRessurs.queryFn(ressurs.navident),
    select: (ledere) => ledere.find((leder) => leder.erDagligOppfolging)?.leder,
  }).data;

  return (
    <div
      css={css`
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        flex-wrap: wrap-reverse;
        gap: var(--a-spacing-4);
      `}
    >
      <div>
        <Name ressurs={ressurs} />
        <Heading
          css={css`
            margin-bottom: var(--a-spacing-6);
          `}
          level="2"
          size="small"
        >
          <PersonIcon />
          <span>Kort fortalt</span>
        </Heading>
        <div
          css={css`
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: 0.5rem 1rem;
          `}
        >
          <RessursInformationDetail
            label="NAV-ident"
            value={ressurs.navident}
          />
          <RessursInformationDetail
            label="Startdato"
            value={formatDisplayDate(ressurs.startdato)}
          />
          <RessursInformationDetail
            label="Sektor"
            value={SektorNameMapping[ressurs.sektor]}
          />
          <RessursInformationDetail label="E-post" value={ressurs.epost} />
          <Telephone ressurs={ressurs} />
          {dagligLeder ? (
            <>
              <span>Leder:</span>
              <div
                css={css`
                  display: flex;
                  gap: 0.5rem;
                `}
              >
                <ReactRouterLink
                  params={{ id: dagligLeder.navident }}
                  to={ressursRoute.to}
                >
                  <Link as="span">
                    {formatVisningsnavn(dagligLeder.visningsnavn)}
                  </Link>
                </ReactRouterLink>

                {dagligLeder && (
                  <>
                    <span>-</span>
                    <span>Daglig oppfølging</span>
                    <HelpText title="Hva er daglig leder">
                      Denne lederen har det daglige ansvaret for
                      sykefraværsoppfølging, godkjenning i MinWinTid, og har
                      tilgang til den ansatte i Læringsportalen.
                    </HelpText>
                  </>
                )}
              </div>
            </>
          ) : undefined}
        </div>
      </div>
      <UserImage
        navident={ressurs.navident}
        navn={formatVisningsnavn(ressurs.visningsnavn)}
        size="126px"
      />
    </div>
  );
}

function RessursInformationDetail({
  label,
  value,
}: {
  label: string;
  value: string | null | undefined;
}) {
  if (!value) {
    return <></>;
  }

  return (
    <>
      <span>{label}:</span>
      <Label as="span">{value}</Label>
    </>
  );
}

function TeamCatSection({ navident }: { navident: string }) {
  const memberships = useQuery({
    queryKey: getTeamcatMemberships.queryKey(navident),
    queryFn: () => getTeamcatMemberships.queryFn(navident),
  }).data;

  if (!memberships) {
    return <></>;
  }

  const flattenedMemberships = [
    ...memberships.teams,
    ...memberships.clusters,
    ...memberships.productAreas,
  ];

  if (flattenedMemberships.length === 0) {
    return <></>;
  }

  const teamcatBaseUrl = isInDevelopment()
    ? "https://teamkatalog.ekstern.dev.nav.no"
    : "https://teamkatalog.nav.no";

  return (
    <>
      <Divider />
      <Heading level="2" size="small">
        <ReceptionIcon />
        Tilhørighet til team, område og klynge
      </Heading>

      <div>
        <Link href={`${teamcatBaseUrl}/resource/${navident}`}>
          Se mer i Teamkatalogen
        </Link>
      </div>
    </>
  );
}
