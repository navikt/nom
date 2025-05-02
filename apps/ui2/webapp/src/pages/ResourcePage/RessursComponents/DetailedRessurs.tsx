import { css } from "@emotion/react";
import {
  Buildings3Icon,
  PencilIcon,
  PersonSuitIcon,
  ReceptionIcon,
} from "@navikt/aksel-icons";
import {
  BodyLong,
  Button,
  ExpansionCard,
  Heading,
  HelpText,
  Label,
  Link,
  Tag,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import {
  Link as ReactRouterLink,
  useNavigate,
  useSearch,
} from "@tanstack/react-router";
import { subMonths } from "date-fns";
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";

import {
  getOrgEnhet,
  getOrgEnheterLederKanRedigere,
  getOrgEnheterLedetAvOptions,
  getOrgEnhetList,
  getOrgTilknytninger,
  getTeamcatMemberships,
  getUnleashEnabled,
  harLederTilgangForNavidentOptions,
} from "~/api/react-query-endpoints";
import {
  useGetSession,
  useHasAdminAccess,
  useHasRobotAccess,
  useHasUtenlandskKonsulentAccess,
  useHasVikafossenAccess,
  useHasWriteAccess,
} from "~/api/useGetSession";
import { Divider } from "~/components/Divider";
import { ModalProvider } from "~/components/modalProvider/ModalProvider";
import { spesielleIdentTyper } from "~/pages/CreateResourcePage/CreateSpecialResourcePage";
import {
  SektorNameMapping,
  TilknytningtypeNameMapping,
} from "~/pages/CreateResourcePage/SektorStep";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { EditPersonOpplysninger } from "~/pages/ResourcePage/EditForms/EditPersonOpplysninger";
import {
  nomDateIsBefore,
  nomDateIsBeforeToday,
  nomDateIsSameOrAfter,
  nomDateIsTodayOrAfter,
  otCustomStortByDate,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { EditOrgtilknytninger } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import { PersonOpplysninger } from "~/pages/ResourcePage/RessursComponents/PersonOpplysninger";
import { blueTopBorder } from "~/resources/NomStyles";
import type { OrgtilknytningState, Ressurs } from "~/types/api-models";
import { IdentType } from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { Tilknytningtype } from "~/types/api-models";
import {
  dateWithoutTimezone,
  formatDisplayDate,
  formatVisningsnavn,
  isAfterToday,
  isBeforeToday,
  isGyldighetsPeriodeActive,
  isInDevelopment,
  todayWithoutTimezone,
} from "~/utils";

export function DetailedRessurs({ ressurs }: { ressurs: Ressurs }) {
  const { editOrgtilknytninger } = useSearch({ from: "/ressurs/$id" });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const harSluttet = nomDateIsBeforeToday(ressurs.sluttdato);

  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = orgtilknytningerQuery.data ?? [];

  const orgEnhetIder = orgtilknytninger.map(
    (tilknytning) => tilknytning.orgEnhetId,
  );

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(orgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(orgEnhetIder),
    enabled: orgEnhetIder.length > 0,
  });

  const orgEnheter = orgEnhetQuery.data;

  const jobberIDirektoratet =
    orgtilknytninger
      ?.filter((orgtilknytning) =>
        nomDateIsTodayOrAfter(orgtilknytning.gyldigTom),
      )
      .some((orgtilknytning) =>
        orgEnheter?.some(
          (orgEnhet) =>
            orgEnhet.id === orgtilknytning.orgEnhetId &&
            orgEnhet.orgEnhetsType === OrgEnhetsType.DIREKTORAT,
        ),
      ) ?? false;

  const orgenheterLederKanRedigereQuery = useQuery({
    queryKey: getOrgEnheterLederKanRedigere.queryKey(),
    queryFn: () => getOrgEnheterLederKanRedigere.queryFn(),
  });

  const orgenheterLederKanRedigere = orgenheterLederKanRedigereQuery.data ?? [];

  const iderLederKanRedigere = orgenheterLederKanRedigere.map(
    (enhet) => enhet.id,
  );

  const redigerbareOrgEnhetIder = new Set(
    new Set(
      orgtilknytninger
        .filter((tilknytning) =>
          nomDateIsSameOrAfter(
            tilknytning.gyldigTom,
            dateWithoutTimezone(subMonths(todayWithoutTimezone(), 2)),
          ),
        )
        .map((tilknytning) => tilknytning.orgEnhetId),
    ),
  );

  const lederKanRedigere = iderLederKanRedigere.some((id) =>
    redigerbareOrgEnhetIder.has(id),
  );

  const sluttetOverToMndSiden = nomDateIsBefore(
    ressurs.sluttdato,
    dateWithoutTimezone(subMonths(todayWithoutTimezone(), 2)),
  );

  const canEditRessursIdentType = (() => {
    switch (ressurs.identType) {
      case IdentType.ROBOT: {
        return (!sluttetOverToMndSiden && hasRobotAccess) || hasAdminAccess;
      }

      case IdentType.VIKAFOSSEN: {
        return (
          (!sluttetOverToMndSiden && hasVikafossenAccess) || hasAdminAccess
        );
      }

      case IdentType.UTENLANDSK_KONSULENT: {
        return (
          (!sluttetOverToMndSiden && hasUtenlandskKonsulentAccess) ||
          hasAdminAccess
        );
      }

      // Hvis identTypen til ressursen ikke er en av de spesielle, behandler vi
      // det som STANDARD hvis den ikke er satt, er STANDARD eller er UKJENT
      default: {
        return (
          (hasWriteAccess && !harSluttet) || lederKanRedigere || hasAdminAccess
        );
      }
    }
  })();

  const ouBegrensningerUnleashQuery = useQuery({
    queryKey: ["unleashToggle", "nom.sperring-endringer-ou"],
    queryFn: () => getUnleashEnabled("nom.sperring-endringer-ou"),
  });

  const ouBegrensningerUnleashAktiv =
    ouBegrensningerUnleashQuery.data?.isUnleashEnabled ?? false;

  const ouBegrensningerAktiv =
    jobberIDirektoratet && ouBegrensningerUnleashAktiv;

  return (
    <div
      css={[
        css`
          padding: var(--a-spacing-12);
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-9);

          h1,
          h2 {
            display: flex;
            gap: var(--a-spacing-2);

            svg {
              width: 24px;
              height: 24px;
            }
          }
        `,
        blueTopBorder,
      ]}
    >
      {editOrgtilknytninger ? (
        <ModalProvider>
          <EditOrgtilknytninger ressurs={ressurs} />
        </ModalProvider>
      ) : (
        <>
          <SummarySection
            canEditRessursIdentType={canEditRessursIdentType}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            ouBegrensningerAktiv={ouBegrensningerAktiv}
            ressurs={ressurs}
          />

          <LeaderForSection ressurs={ressurs} />

          <OrgTilknytningSection
            canEditRessursIdentType={canEditRessursIdentType}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            orgtilknytninger={orgtilknytninger}
            ouBegrensningerAktiv={ouBegrensningerAktiv}
            ressurs={ressurs}
          />

          <TeamCatSection navident={ressurs.navident} />

          <BottomSection
            canEditRessursIdentType={canEditRessursIdentType}
            harSluttet={harSluttet}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            ouBegrensningerAktiv={ouBegrensningerAktiv}
            ressurs={ressurs}
          />

          {ressurs.isNomMastered && (
            <Tag
              css={css`
                width: fit-content;
              `}
              data-cy="nom-mastered-tag"
              size="small"
              variant="alt1"
            >
              Opprettet/endret i NOM
            </Tag>
          )}
        </>
      )}
    </div>
  );
}

function OrgTilknytningSection({
  ressurs,
  hasWriteAccessForRessurs,
  orgtilknytninger,
  ouBegrensningerAktiv,
  canEditRessursIdentType,
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  orgtilknytninger: OrgtilknytningState[];
  ouBegrensningerAktiv: boolean;
  canEditRessursIdentType: boolean;
}) {
  const activeOrgtilknytninger = orgtilknytninger.filter((orgTilknytning) =>
    isGyldighetsPeriodeActive(
      orgTilknytning.gyldigFom,
      orgTilknytning.gyldigTom,
    ),
  );

  const expiredOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigTom &&
      isBeforeToday(new Date(orgTilknytning.gyldigTom)),
  );

  const futureOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigFom &&
      isAfterToday(new Date(orgTilknytning.gyldigFom)),
  );

  const harSluttet = !!ressurs.sluttdato && isBeforeToday(ressurs.sluttdato);

  return (
    <>
      <Divider noMargins />

      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
        `}
      >
        <div
          css={css`
            display: flex;
            justify-content: space-between;
            margin-bottom: var(--a-spacing-4);
          `}
        >
          <Heading level="2" size="small">
            <Buildings3Icon />
            Organisasjonstilknytninger
          </Heading>

          <EditOrgtilknytningerButton
            canEditRessursIdentType={canEditRessursIdentType}
            harSluttet={harSluttet}
            ouBegrensningerAktiv={ouBegrensningerAktiv}
          />
        </div>

        {orgtilknytninger.length === 0 && (
          <BodyLong>
            Medarbeideren er ikke registrert på en organisasjonsenhet.
          </BodyLong>
        )}

        {activeOrgtilknytninger.map((orgTilknytning) => (
          <OrganisationHierarchyTree
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
            ressurs={ressurs}
          />
        ))}
        <FutureOrgTilknytninger
          futureOrgTilknytninger={futureOrgTilknytninger}
          hasWriteAccessForRessurs={hasWriteAccessForRessurs}
          ressurs={ressurs}
        />
        <ExpiredOrgTilknytninger
          expiredOrgTilknytninger={expiredOrgTilknytninger}
        />
      </div>
    </>
  );
}

function FutureOrgTilknytninger({
  futureOrgTilknytninger,
  ressurs,
  hasWriteAccessForRessurs,
}: {
  futureOrgTilknytninger: OrgtilknytningState[];
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
}) {
  if (futureOrgTilknytninger.length === 0) {
    return <></>;
  }

  return (
    <>
      <Heading level="2" size="xsmall">
        Fremtidige org.tilknytninger
      </Heading>
      {futureOrgTilknytninger.map((orgTilknytning) => (
        <OrganisationHierarchyTree
          hasWriteAccessForRessurs={hasWriteAccessForRessurs}
          key={orgTilknytning.fid}
          orgTilknytning={orgTilknytning}
          ressurs={ressurs}
        />
      ))}
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

  expiredOrgTilknytninger.sort((a, b) => otCustomStortByDate(a, b));

  const tilknyttingerToShow = expiredOrgTilknytninger.slice(
    0,
    showAll ? expiredOrgTilknytninger.length : 3,
  );

  const shouldDisplayShowAllButton =
    expiredOrgTilknytninger.length > 3 && !showAll;

  return (
    <>
      <div
        css={css`
          display: flex;
          align-content: center;
        `}
      >
        <Heading level="2" size="xsmall">
          Avsluttede org.tilknytninger
        </Heading>
        <HelpText
          css={css`
            margin-left: var(--a-spacing-2);
          `}
          title="Hvilke org.-tilknytninger vises her?"
        >
          NOM har kun avsluttede organisasjonstilknytninger fra og med 2021.
        </HelpText>
      </div>
      {tilknyttingerToShow.map((orgtilknytning) => (
        <ExpiredOrgTilknytning
          expiredOrgTilknytning={orgtilknytning}
          key={orgtilknytning.fid}
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
        gap: var(--a-spacing-4);
        align-items: center;
        background: var(--a-bg-subtle);
        padding: var(--a-spacing-6);
        border: 1px solid var(--a-border-default);
        border-radius: var(--a-border-radius-large);
      `}
      key={expiredOrgTilknytning.orgEnhetId}
    >
      <Heading level="3" size="medium">
        {orgEnhet?.navn}
      </Heading>
      <span
        css={css`
          margin-left: auto;
        `}
      >
        {formatDisplayDate(expiredOrgTilknytning.gyldigFom)} -{" "}
        {formatDisplayDate(expiredOrgTilknytning.gyldigTom)}
      </span>

      <Tag size="medium" variant="warning">
        Avsluttet
      </Tag>
    </div>
  );
}

function OrganisationHierarchyTree({
  ressurs,
  orgTilknytning,
  hasWriteAccessForRessurs,
}: {
  orgTilknytning: OrgtilknytningState;
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgTilknytning.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgTilknytning.orgEnhetId),
  });

  const hasAdminAccess = useHasAdminAccess();

  const hasSelfServiceAccess =
    useGetSession().data?.navident === ressurs.navident;

  const orgEnhet = orgEnhetQuery.data;

  if (!orgEnhet) {
    return <></>;
  }

  const orgEnhetLeder = orgEnhet.ledere[0];

  const {
    gyldigTom,
    gyldigFom,
    erDagligOppfolging,
    sektor,
    orgtilknytningType,
  } = orgTilknytning;

  const nyeTilknytningtyper = [
    Tilknytningtype.VIKAR,
    Tilknytningtype.UFORUTSETT_BEHOV,
    Tilknytningtype.PRAKSISARBEID,
    Tilknytningtype.UTDANNINGSSTILLINGER,
  ];

  const startsInTheFuture = gyldigFom && isAfterToday(new Date(gyldigFom));

  const kanAlltidSeTilknytningstype =
    hasWriteAccessForRessurs || hasSelfServiceAccess || hasAdminAccess;

  return (
    <ExpansionCard
      aria-label="organisasjons-hierarki"
      css={css`
        &.navds-expansioncard--open #title-start-date {
          color: blue;
          visibility: hidden;
        }
      `}
      size="small"
    >
      <ExpansionCard.Header
        css={css`
          .navds-expansioncard__header-content {
            width: 100%;
          }
        `}
      >
        <ExpansionCard.Title
          css={css`
            display: flex;
          `}
        >
          <span>{orgEnhet.navn}</span>
          {startsInTheFuture && (
            <>
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

              <Tag
                css={css`
                  margin-left: auto;
                `}
                size="medium"
                variant="success"
              >
                Fremtidig
              </Tag>
            </>
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
                margin-left: var(--a-spacing-4);
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
        <div
          css={css`
            display: flex;
            justify-content: space-between;
          `}
        >
          <div>
            {gyldigFom && (
              <span>
                <b>Fra og med:</b> {formatDisplayDate(gyldigFom)}
              </span>
            )}
            <br />
            {gyldigTom && (
              <span>
                <b>Til og med:</b> {formatDisplayDate(gyldigTom)}
              </span>
            )}
          </div>

          <div>
            {sektor && (
              <span>
                <b>Sektor</b>: {SektorNameMapping[sektor]}
              </span>
            )}
            <br />
            {orgtilknytningType &&
              (sektor ||
                nyeTilknytningtyper.find(
                  (value) => value === orgtilknytningType,
                )) &&
              orgtilknytningType !== Tilknytningtype.INGEN &&
              (orgtilknytningType !== Tilknytningtype.ARBEIDSTRENING ||
                kanAlltidSeTilknytningstype) && (
                <span>
                  <b>Tilknytningstype:</b>{" "}
                  {TilknytningtypeNameMapping[orgtilknytningType]}
                </span>
              )}
          </div>
        </div>
        <Divider size="small" />
        <CompactOrganisasjonHierarki orgEnhet={orgEnhet} ressurs={ressurs} />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

const nomIdVikafossen = new Set(["ru664y", "fe178t"]);

function LeaderForSection({ ressurs }: { ressurs: Ressurs }) {
  const hasAdminAccess = useHasAdminAccess();
  const lederForOrgenheter =
    useQuery(getOrgEnheterLedetAvOptions(ressurs.navident)).data ?? [];

  const lederForOrgenheterFiltrert =
    hasAdminAccess || isInDevelopment()
      ? lederForOrgenheter
      : lederForOrgenheter.filter((enh) => !nomIdVikafossen.has(enh.id));

  if (lederForOrgenheterFiltrert.length === 0) {
    return <></>;
  }

  /*
     Leder høyt oppe i hierarkiet er ofte leder i flere "like" orgEnheter.
     Her antas det at første forekomst av navn for en orgEnhet er den høyest i hierarkiet.
     Se https://jira.adeo.no/browse/ORG-1437 for flere detaljer.
    */
  const uniqLederFor = uniqBy(
    lederForOrgenheterFiltrert,
    (orgEnhet) => orgEnhet.navn,
  );

  return (
    <>
      <Divider noMargins />
      <Heading level="2" size="small">
        <PersonSuitIcon />
        Leder for
      </Heading>

      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-2);
        `}
      >
        {uniqLederFor.map((orgEnhet) => (
          <ReactRouterLink
            key={orgEnhet.id}
            params={{ id: orgEnhet.id }}
            search={{}}
            to="/org/$id"
          >
            <Link as="span">{orgEnhet.navn}</Link>
          </ReactRouterLink>
        ))}
      </div>
    </>
  );
}

function SummarySection({
  ressurs,
  hasWriteAccessForRessurs,
  ouBegrensningerAktiv,
  canEditRessursIdentType,
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  ouBegrensningerAktiv: boolean;
  canEditRessursIdentType: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasSelfServiceAccess =
    useGetSession().data?.navident === ressurs.navident;

  const spesiellRessurs = spesielleIdentTyper.includes(ressurs.identType);

  const canEditPersonopplysninger =
    (canEditRessursIdentType &&
      (spesiellRessurs ||
        (hasWriteAccess && hasWriteAccessForRessurs) ||
        hasAdminAccess)) ||
    (!spesiellRessurs && hasSelfServiceAccess);

  if (editPersonopplysninger && canEditPersonopplysninger) {
    return (
      <ModalProvider>
        <EditPersonOpplysninger
          hasSelfServiceAccess={hasSelfServiceAccess}
          ressurs={ressurs}
        />
      </ModalProvider>
    );
  }

  return (
    <PersonOpplysninger
      canEditPersonopplysninger={canEditPersonopplysninger}
      ouBegrensningerAktiv={ouBegrensningerAktiv}
      ressurs={ressurs}
    />
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
      <Divider noMargins />
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

function EditOrgtilknytningerButton({
  harSluttet,
  ouBegrensningerAktiv,
  canEditRessursIdentType,
}: {
  harSluttet: boolean;
  ouBegrensningerAktiv: boolean;
  canEditRessursIdentType: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const navigate = useNavigate();

  const hasAdminAccess = useHasAdminAccess();

  const utilgjengeligOu = ouBegrensningerAktiv && !hasAdminAccess;

  if (!editPersonopplysninger && canEditRessursIdentType) {
    return (
      <div
        css={css`
          display: flex;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        {utilgjengeligOu && (
          <HelpText>
            Flytting av medarbeidere er midlertidig utilgjengelig på grunn av
            omorganisering av direktoratet. Flytting gjøres sentralt av teamet i
            NOM. Meld sak i Porten ved behov.
          </HelpText>
        )}

        <Button
          data-cy="endre-orgtilknytninger-button"
          disabled={utilgjengeligOu}
          icon={<PencilIcon aria-hidden />}
          onClick={() =>
            navigate({
              to: "/ressurs/$id",
              search: { editOrgtilknytninger: true },
              params: (previous) => ({ id: previous.id ?? "" }),
            })
          }
          size="small"
          variant="secondary"
        >
          Endre
        </Button>
      </div>
    );
  }

  if (harSluttet) {
    return (
      <Button
        icon={<PencilIcon fontSize="1.5rem" />}
        onClick={() =>
          navigate({ to: "/ressurs/create/$step", params: { step: "1" } })
        }
        size="small"
        variant="secondary"
      >
        Gjenopprett
      </Button>
    );
  }

  return;
}

function BottomSection({
  ressurs,
  hasWriteAccessForRessurs,
  harSluttet,
  ouBegrensningerAktiv,
  canEditRessursIdentType,
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  harSluttet: boolean;
  ouBegrensningerAktiv: boolean;
  canEditRessursIdentType: boolean;
}) {
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const spesiellRessurs = spesielleIdentTyper.includes(ressurs.identType);

  const canTerminateRessurs =
    canEditRessursIdentType &&
    (spesiellRessurs ||
      (hasWriteAccess && hasWriteAccessForRessurs) ||
      hasAdminAccess);

  if (canTerminateRessurs && !harSluttet) {
    return (
      <>
        <Divider noMargins />

        <TerminateButton
          ouBegrensningerAktiv={ouBegrensningerAktiv}
          ressursNavIdent={ressurs.navident}
        />
      </>
    );
  }

  return;
}

function TerminateButton({
  ressursNavIdent,
  ouBegrensningerAktiv,
}: {
  ressursNavIdent: string;
  ouBegrensningerAktiv: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasAdminAccess = useHasAdminAccess();

  const utilgjengeligOu = ouBegrensningerAktiv && !hasAdminAccess;

  if (!editPersonopplysninger) {
    return (
      <div
        css={css`
          display: flex;
          align-items: center;
          gap: var(--a-spacing-2);
          align-self: end;
        `}
      >
        {utilgjengeligOu && (
          <HelpText>
            Avslutting av medarbeidere er midlertidig utilgjengelig på grunn av
            omorganisering av direktoratet. Meld sak i Porten dersom personen
            slutter før 10. mai.
          </HelpText>
        )}

        <ReactRouterLink
          css={css`
            align-self: flex-end;
          `}
          params={{ id: ressursNavIdent }}
          search={{}}
          to="/ressurs-terminate/$id"
        >
          <Button
            data-cy="avslutt-medarbeider-button"
            disabled={utilgjengeligOu}
            icon={<PencilIcon aria-hidden />}
            size="small"
            type="button"
            variant="secondary"
          >
            Medarbeider slutter i Nav
          </Button>
        </ReactRouterLink>
      </div>
    );
  }

  return;
}
