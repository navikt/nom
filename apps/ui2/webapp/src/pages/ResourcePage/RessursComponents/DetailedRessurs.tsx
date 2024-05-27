import { css } from "@emotion/react";
import {
  Buldings3Icon,
  PencilIcon,
  PersonSuitIcon,
  ReceptionIcon,
  XMarkIcon,
} from "@navikt/aksel-icons";
import {
  BodyLong,
  Button,
  ExpansionCard,
  Heading,
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
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";

import {
  getOrgEnhet,
  getOrgEnheterLedetAvOptions,
  getOrgTilknytninger,
  getTeamcatMemberships,
} from "~/api/react-query-endpoints";
import { Divider } from "~/components/Divider";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { EditButtonWrapper } from "~/pages/ResourcePage/EditForms/EditButtonWrapper";
import { EditPersonOpplysninger } from "~/pages/ResourcePage/EditForms/EditPersonOpplysninger";
import { EditOrgtilknytninger } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import { PersonOpplysninger } from "~/pages/ResourcePage/RessursComponents/PersonOpplysninger";
import { blueTopBorder } from "~/resources/NomStyles";
import type { OrgtilknytningState, Ressurs } from "~/types/api-models";
import {
  formatDisplayDate,
  formatVisningsnavn,
  isAfterPresent,
  isBeforePresent,
  isGyldighetsPeriodeActive,
  isInDevelopment,
} from "~/utils";

export function DetailedRessurs({ ressurs }: { ressurs: Ressurs }) {
  const { editOrgtilknytninger } = useSearch({ from: "/ressurs/$id" });

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
        <EditOrgtilknytninger ressurs={ressurs} />
      ) : (
        <>
          <SummarySection ressurs={ressurs} />
          <LeaderForSection ressurs={ressurs} />
          <OrgTilknytningSection ressurs={ressurs} />
          <TeamCatSection navident={ressurs.navident} />
          <TerminateButton ressurs={ressurs} />
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

function OrgTilknytningSection({ ressurs }: { ressurs: Ressurs }) {
  const orgtilknytningerQuery = useQuery({
    queryKey: getOrgTilknytninger.queryKey(ressurs.navident),
    queryFn: () => getOrgTilknytninger.queryFn(ressurs.navident),
  });

  const orgtilknytninger = orgtilknytningerQuery.data ?? [];

  const navigate = useNavigate();

  const activeOrgtilknytninger = orgtilknytninger.filter((orgTilknytning) =>
    isGyldighetsPeriodeActive(
      orgTilknytning.gyldigFom,
      orgTilknytning.gyldigTom,
    ),
  );

  const expiredOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigTom &&
      isBeforePresent(new Date(orgTilknytning.gyldigTom)),
  );

  const futureOrgTilknytninger = orgtilknytninger.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigFom &&
      isAfterPresent(new Date(orgTilknytning.gyldigFom)),
  );

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
            <Buldings3Icon />
            Organisatorisk tilhørighet
          </Heading>
          <EditButtonWrapper ressurs={ressurs}>
            <Button
              data-cy="endre-orgtilknytninger-button"
              icon={<PencilIcon aria-hidden />}
              onClick={() =>
                navigate({
                  search: { editOrgtilknytninger: true },
                  params: (previous) => previous,
                })
              }
              size="small"
              variant="secondary"
            >
              Endre
            </Button>
          </EditButtonWrapper>
        </div>

        {orgtilknytninger.length === 0 ? (
          <BodyLong>
            Medarbeideren er ikke registrert på en organisasjonsenhet. Klikk
            endre for å legge til.
          </BodyLong>
        ) : undefined}

        {activeOrgtilknytninger.map((orgTilknytning) => (
          <OrganisationHierarchyTree
            key={orgTilknytning.fid}
            orgTilknytning={orgTilknytning}
            ressurs={ressurs}
          />
        ))}
        <FutureOrgTilknytninger
          futureOrgTilknytninger={futureOrgTilknytninger}
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
}: {
  futureOrgTilknytninger: OrgtilknytningState[];
  ressurs: Ressurs;
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
        gap: var(--a-spacing-4);
        align-items: center;
        background: white;
        padding: var(--a-spacing-6);
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

  // const navigate = useNavigate();

  if (!orgEnhet) {
    return <></>;
  }

  const orgEnhetLeder = orgEnhet.ledere[0];

  const { gyldigTom, gyldigFom, erDagligOppfolging } = orgTilknytning;

  const startsInTheFuture = gyldigFom && isAfterPresent(new Date(gyldigFom));

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
        {gyldigFom && <span>Fra og med: {formatDisplayDate(gyldigFom)}</span>}
        <br />
        {gyldigTom && <span>Til og med: {formatDisplayDate(gyldigTom)}</span>}
        <Divider size="small" />
        <CompactOrganisasjonHierarki orgEnhet={orgEnhet} ressurs={ressurs} />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function LeaderForSection({ ressurs }: { ressurs: Ressurs }) {
  const lederForOrgenheter =
    useQuery(getOrgEnheterLedetAvOptions(ressurs.navident)).data ?? [];

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
            to={"/org/$id"}
          >
            <Link as="span">{orgEnhet.navn}</Link>
          </ReactRouterLink>
        ))}
      </div>
    </>
  );
}

function SummarySection({ ressurs }: { ressurs: Ressurs }) {
  const { editPersonopplysninger } = useSearch({
    from: "/ressurs/$id",
  });

  if (editPersonopplysninger) {
    return <EditPersonOpplysninger ressurs={ressurs} />;
  }

  return <PersonOpplysninger ressurs={ressurs} />;
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

function TerminateButton({ ressurs }: { ressurs: Ressurs }) {
  return (
    <EditButtonWrapper ressurs={ressurs}>
      <Divider noMargins />
      <ReactRouterLink
        css={css`
          align-self: flex-end;
        `}
        params={{ id: ressurs.navident }}
        search={{}}
        to="/ressurs-terminate/$id"
      >
        <Button
          css={css`
            --ac-button-secondary-border: var(--a-red-500);
            --ac-button-secondary-text: var(--a-red-500);
            --ac-button-secondary-hover-text: var(--a-red-500);
            --ac-button-secondary-hover-bg: var(--a-red-50);

            :active {
              --ac-button-secondary-active-bg: var(--a-red-500);
            }
          `}
          icon={<XMarkIcon aria-hidden />}
          size="small"
          type="button"
          variant="secondary"
        >
          Medarbeider slutter
        </Button>
      </ReactRouterLink>
    </EditButtonWrapper>
  );
}
