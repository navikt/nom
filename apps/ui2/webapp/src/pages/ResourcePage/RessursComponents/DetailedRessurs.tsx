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
  HStack,
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
  getOrgEnheterRessursKanRedigere,
  getOrgenhetLederByNavIdent,
  getOrgTilknytninger,
  getTeamcatMemberships,
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
import { EnhetKontekstInfo } from "~/components/EnhetKontekstInfo";
import { ModalProvider } from "~/components/modalProvider/ModalProvider";
import { SmartLink } from "~/components/navigation/SmartLink";
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
  otCustomStortByDate,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { EditOrgtilknytninger } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import { PersonOpplysninger } from "~/pages/ResourcePage/RessursComponents/PersonOpplysninger";
import { blueTopBorder } from "~/resources/NomStyles";
import type { OrgtilknytningState, Ressurs } from "~/types/api-models";
import { IdentType, Tilknytningtype } from "~/types/api-models";
import {
  dateWithoutTimezone,
  formatDisplayDate,
  isAfterToday,
  isBeforeToday,
  isGyldighetsPeriodeActive,
  isInDevelopment,
  isTodayOrAfter,
  isTodayOrBefore,
  skjulteOrgtilknytninger,
  todayWithoutTimezone,
} from "~/utils";

export function DetailedRessurs({ ressurs }: { ressurs: Ressurs }) {
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const { editOrgtilknytninger } = useSearch({ from: "/ressurs/$id" });

  const hasWriteAccessForRessurs =
    useQuery(harLederTilgangForNavidentOptions(ressurs.navident)).data ?? false;

  const harSluttet = nomDateIsBeforeToday(ressurs.sluttdato);

  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = orgtilknytningerQuery.data ?? [];

  const dagensDolOrgtilknytningTilknytningstype = orgtilknytninger.find(
    (tilknytning) =>
      isTodayOrBefore(tilknytning.gyldigFom) &&
      isTodayOrAfter(tilknytning.gyldigTom) &&
      tilknytning.erDagligOppfolging,
  )?.orgtilknytningType;

  const eksternSamarbeidspartner =
    dagensDolOrgtilknytningTilknytningstype === Tilknytningtype.HELFO ||
    dagensDolOrgtilknytningTilknytningstype === Tilknytningtype.SKATTEETATEN ||
    dagensDolOrgtilknytningTilknytningstype ===
      Tilknytningtype.RIKSREVISJONEN ||
    dagensDolOrgtilknytningTilknytningstype ===
      Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER;

  const orgenheterLederKanRedigereQuery = useQuery({
    queryKey: getOrgEnheterRessursKanRedigere.queryKey(),
    queryFn: () => getOrgEnheterRessursKanRedigere.queryFn(),
  });

  const orgenheterLederKanRedigere = orgenheterLederKanRedigereQuery.data ?? [];

  const iderLederKanRedigere = orgenheterLederKanRedigere.map(
    (enhet) => enhet.id,
  );

  const redigerbareOrgEnhetIder = new Set(
    orgtilknytninger
      .filter((tilknytning) =>
        nomDateIsSameOrAfter(
          tilknytning.gyldigTom,
          dateWithoutTimezone(subMonths(todayWithoutTimezone(), 6)),
        ),
      )
      .map((tilknytning) => tilknytning.orgEnhetId),
  );

  const tidligereLeder = iderLederKanRedigere.some((id) =>
    redigerbareOrgEnhetIder.has(id),
  );

  const sluttetOverSeksMndSiden = nomDateIsBefore(
    ressurs.sluttdato,
    dateWithoutTimezone(subMonths(todayWithoutTimezone(), 6)),
  );

  const canEditOrgtilknytninger = (() => {
    switch (ressurs.identType) {
      case IdentType.ROBOT: {
        return (
          (!sluttetOverSeksMndSiden && hasRobotAccess) ||
          (!harSluttet && hasWriteAccess) ||
          tidligereLeder ||
          hasAdminAccess
        );
      }

      case IdentType.VIKAFOSSEN: {
        return !sluttetOverSeksMndSiden && hasVikafossenAccess;
      }

      case IdentType.UTENLANDSK_KONSULENT: {
        return (
          (!sluttetOverSeksMndSiden && hasUtenlandskKonsulentAccess) ||
          (!harSluttet && hasWriteAccess) ||
          tidligereLeder ||
          hasAdminAccess
        );
      }

      default: {
        return (
          (!harSluttet && hasWriteAccess) || tidligereLeder || hasAdminAccess
        );
      }
    }
  })();

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
          <EditOrgtilknytninger
            canEditOrgtilknytninger={canEditOrgtilknytninger}
            ressurs={ressurs}
          />
        </ModalProvider>
      ) : (
        <>
          <SummarySection
            eksternSamarbeidspartner={eksternSamarbeidspartner}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            ressurs={ressurs}
            tidligereLeder={tidligereLeder}
          />

          <LeaderForSection ressurs={ressurs} />

          <OrgTilknytningSection
            canEditOrgtilknytninger={canEditOrgtilknytninger}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            orgtilknytninger={orgtilknytninger}
            ressurs={ressurs}
          />

          <TeamCatSection navident={ressurs.navident} />

          <BottomSection
            harSluttet={harSluttet}
            hasWriteAccessForRessurs={hasWriteAccessForRessurs}
            ressurs={ressurs}
          />

          {ressurs.isNomMastered && (
            <Tag
              css={css`
                width: fit-content;
              `}
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
  canEditOrgtilknytninger,
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  orgtilknytninger: OrgtilknytningState[];
  canEditOrgtilknytninger: boolean;
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

  const harSluttet = nomDateIsBeforeToday(ressurs.sluttdato);

  const hasAdminAccess = useHasAdminAccess();

  const hasSelfServiceAccess =
    useGetSession().data?.navident === ressurs.navident;

  const skjulOrgtilknytninger =
    !hasSelfServiceAccess && !hasWriteAccessForRessurs && !hasAdminAccess;

  const filteredExpiredOrgTilknytninger = skjulOrgtilknytninger
    ? expiredOrgTilknytninger.filter(
        (tilknytning) => !skjulteOrgtilknytninger.includes(tilknytning.fid),
      )
    : expiredOrgTilknytninger;

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
            canEditOrgtilknytninger={canEditOrgtilknytninger}
            harSluttet={harSluttet}
            ressurs={ressurs}
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
          expiredOrgTilknytninger={filteredExpiredOrgTilknytninger}
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
        Fremtidige organisasjonstilknytninger
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
          Avsluttede organisasjonstilknytninger
        </Heading>
        <HelpText
          css={css`
            margin-left: var(--a-spacing-2);
          `}
          title="Avsluttede organisasjonstilknytninger"
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
          Vis alle avsluttede organisasjonstilknytninger
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
        background: var(--a-surface-subtle);
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

  const orgEnhetLeder = orgEnhet.leder;

  const {
    gyldigTom,
    gyldigFom,
    erDagligOppfolging,
    sektor,
    orgtilknytningType,
  } = orgTilknytning;

  const tilknytningTyperVisesUtenSektor = [
    Tilknytningtype.VIKAR,
    Tilknytningtype.UFORUTSETT_BEHOV,
    Tilknytningtype.PRAKSISARBEID,
    Tilknytningtype.UTDANNINGSSTILLINGER,
    Tilknytningtype.HELFO,
    Tilknytningtype.SKATTEETATEN,
    Tilknytningtype.RIKSREVISJONEN,
  ];

  const startsInTheFuture = gyldigFom && isAfterToday(new Date(gyldigFom));

  const kanAlltidSeTilknytningstype =
    hasWriteAccessForRessurs || hasSelfServiceAccess || hasAdminAccess;

  return (
    <ExpansionCard
      aria-label="organisasjons-hierarki"
      css={css`
        .navds-expansioncard--open #title-start-date {
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
            {orgEnhetLeder?.visningsnavn}
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
        <EnhetKontekstInfo
          displayKostnadssted
          displayLeder={false}
          orgEnhet={orgEnhet}
        />

        <Divider size="small" />

        <div
          css={css`
            display: flex;
            justify-content: space-between;
            margin-top: var(--a-spacing-2);
          `}
        >
          <div
            css={css`
              display: flex;
              flex-direction: column;
            `}
          >
            {gyldigFom && (
              <span>
                <b>Fra og med:</b> {formatDisplayDate(gyldigFom)}
              </span>
            )}

            {gyldigTom && (
              <span>
                <b>Til og med:</b> {formatDisplayDate(gyldigTom)}
              </span>
            )}
          </div>

          <div
            css={css`
              display: flex;
              flex-direction: column;
            `}
          >
            {sektor && (
              <span>
                <b>Sektor</b>: {SektorNameMapping[sektor]}
              </span>
            )}

            {orgtilknytningType &&
              (sektor ||
                tilknytningTyperVisesUtenSektor.includes(orgtilknytningType)) &&
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

function LeaderForSection({ ressurs }: { ressurs: Ressurs }) {
  const inDevelopment = isInDevelopment();

  const lederForOrgenheter =
    useQuery({
      queryKey: getOrgenhetLederByNavIdent.queryKey(ressurs.navident),
      queryFn: () => getOrgenhetLederByNavIdent.queryFn(ressurs.navident),
      select: (orgenhetLederList) =>
        orgenhetLederList.filter((orgEnhet) => {
          if (!orgEnhet.sluttdato) {
            return true;
          }
          const isoFormatRoughlyOk = /^\d{4}-\d{2}-\d{2}$/.test(
            orgEnhet.sluttdato,
          );
          return (
            isoFormatRoughlyOk && orgEnhet.sluttdato >= new Date().toISOString()
          );
        }),
    }).data ?? [];

  const skjulteEksterneOrgEnheter = inDevelopment
    ? ["fa520s", "fe857w", "hy255y", "ja464g", "py733e", "ru151s"]
    : ["fe178t", "hu192a", "ne938h", "va111j", "va981f", "ru664y", "ke602d"];

  const lederForOrgenheterFiltrert = lederForOrgenheter.filter(
    (orgEnhet) => !skjulteEksterneOrgEnheter.includes(orgEnhet.orgEnhetId),
  );

  if (lederForOrgenheterFiltrert.length === 0) {
    return;
  }

  /*
     Leder høyt oppe i hierarkiet er ofte leder i flere "like" orgEnheter.
     Her antas det at første forekomst av navn for en orgEnhet er den høyest i hierarkiet.
     Se https://jira.adeo.no/browse/ORG-1437 for flere detaljer.
    */
  const unikeOrgEnheter = uniqBy(
    lederForOrgenheterFiltrert,
    (orgEnhet) => orgEnhet.orgEnhetNavn,
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
        {unikeOrgEnheter.map((orgEnhet) => (
          <ReactRouterLink
            key={orgEnhet.orgEnhetId}
            params={{ id: orgEnhet.orgEnhetId }}
            search={{}}
            to="/org/$id"
          >
            <Link as="span">{orgEnhet.orgEnhetNavn}</Link>
          </ReactRouterLink>
        ))}
      </div>
    </>
  );
}

function SummarySection({
  ressurs,
  hasWriteAccessForRessurs,
  tidligereLeder,
  eksternSamarbeidspartner,
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  tidligereLeder: boolean;
  eksternSamarbeidspartner: boolean;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const hasSelfServiceAccess =
    useGetSession().data?.navident === ressurs.navident;

  const harSluttet = nomDateIsBeforeToday(ressurs.sluttdato);

  const canEditPersonopplysninger = (() => {
    switch (ressurs.identType) {
      case IdentType.ROBOT: {
        return (
          (!harSluttet &&
            (hasRobotAccess ||
              tidligereLeder ||
              (hasWriteAccess && hasWriteAccessForRessurs))) ||
          hasAdminAccess
        );
      }

      case IdentType.VIKAFOSSEN: {
        return !harSluttet && hasVikafossenAccess;
      }

      case IdentType.UTENLANDSK_KONSULENT: {
        return (
          (!harSluttet &&
            (hasUtenlandskKonsulentAccess ||
              tidligereLeder ||
              (hasWriteAccess && hasWriteAccessForRessurs))) ||
          hasAdminAccess
        );
      }

      default: {
        return (
          (!harSluttet &&
            ((hasWriteAccess && hasWriteAccessForRessurs) ||
              tidligereLeder ||
              hasSelfServiceAccess)) ||
          hasAdminAccess
        );
      }
    }
  })();

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
      eksternSamarbeidspartner={eksternSamarbeidspartner}
      ressurs={ressurs}
    />
  );
}

function TeamCatSection({ navident }: { navident: string }) {
  const membershipsQuery = useQuery({
    queryKey: getTeamcatMemberships.queryKey(navident),
    queryFn: () => getTeamcatMemberships.queryFn(navident),
  });

  const memberships = membershipsQuery.data;

  if (!memberships) {
    return;
  }

  const activeMemberships = [
    ...memberships.teams,
    ...memberships.clusters,
    ...memberships.productAreas,
  ].filter((membership) => membership.status === "ACTIVE");

  if (activeMemberships.length === 0) {
    return;
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
        <SmartLink externalIcon href={`${teamcatBaseUrl}/resource/${navident}`}>
          Se mer i Teamkatalogen
        </SmartLink>
      </div>
    </>
  );
}

function EditOrgtilknytningerButton({
  harSluttet,
  canEditOrgtilknytninger,
  ressurs,
}: {
  harSluttet: boolean;
  canEditOrgtilknytninger: boolean;
  ressurs: Ressurs;
}) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const hasMissingFysiskBrukersted = !ressurs?.primaryFysiskBrukersted;

  const navigate = useNavigate();

  const kanGjenopprette = (() => {
    switch (ressurs.identType) {
      case IdentType.ROBOT: {
        return hasRobotAccess || hasAdminAccess;
      }

      case IdentType.VIKAFOSSEN: {
        return hasVikafossenAccess;
      }

      case IdentType.UTENLANDSK_KONSULENT: {
        return hasUtenlandskKonsulentAccess || hasAdminAccess;
      }

      default: {
        return hasWriteAccess || hasAdminAccess;
      }
    }
  })();

  if (!editPersonopplysninger && canEditOrgtilknytninger) {
    return (
      <HStack align="center" gap="space-8">
        {hasMissingFysiskBrukersted && (
          <HelpText title="Knappen er deaktivert">
            Denne knappen er deaktivert fordi ressursen mangler fysisk
            brukersted
          </HelpText>
        )}
        <Button
          /*disabled={hasMissingFysiskBrukersted}*/
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
      </HStack>
    );
  }

  if (!editPersonopplysninger && harSluttet && kanGjenopprette) {
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
}: {
  ressurs: Ressurs;
  hasWriteAccessForRessurs: boolean;
  harSluttet: boolean;
}) {
  const hasWriteAccess = useHasWriteAccess();
  const hasAdminAccess = useHasAdminAccess();

  const hasRobotAccess = useHasRobotAccess();
  const hasVikafossenAccess = useHasVikafossenAccess();
  const hasUtenlandskKonsulentAccess = useHasUtenlandskKonsulentAccess();

  const canTerminateRessurs = (() => {
    switch (ressurs.identType) {
      case IdentType.ROBOT: {
        return (
          hasRobotAccess ||
          (hasWriteAccess && hasWriteAccessForRessurs) ||
          hasAdminAccess
        );
      }

      case IdentType.VIKAFOSSEN: {
        return hasVikafossenAccess;
      }

      case IdentType.UTENLANDSK_KONSULENT: {
        return (
          hasUtenlandskKonsulentAccess ||
          (hasWriteAccess && hasWriteAccessForRessurs) ||
          hasAdminAccess
        );
      }

      default: {
        return (hasWriteAccess && hasWriteAccessForRessurs) || hasAdminAccess;
      }
    }
  })();

  if (canTerminateRessurs && !harSluttet) {
    return (
      <>
        <Divider noMargins />

        <TerminateButton ressursNavIdent={ressurs.navident} />
      </>
    );
  }

  return;
}

function TerminateButton({ ressursNavIdent }: { ressursNavIdent: string }) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  if (!editPersonopplysninger) {
    return (
      <ReactRouterLink
        css={css`
          align-self: flex-end;
        `}
        params={{ id: ressursNavIdent }}
        search={{}}
        to="/ressurs-terminate/$id"
      >
        <Button
          icon={<PencilIcon aria-hidden />}
          size="small"
          type="button"
          variant="secondary"
        >
          Medarbeider slutter i Nav
        </Button>
      </ReactRouterLink>
    );
  }

  return;
}
