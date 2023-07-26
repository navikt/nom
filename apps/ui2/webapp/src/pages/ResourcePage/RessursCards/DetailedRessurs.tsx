import { css } from "@emotion/react";
import {
  Buldings3Icon,
  PersonIcon,
  PersonSuitIcon,
  ReceptionIcon,
} from "@navikt/aksel-icons";
import {
  Button,
  ExpansionCard,
  Heading,
  HelpText,
  Label,
  Link,
  Tag,
} from "@navikt/ds-react";
import axios from "axios";
import { isAfter, isBefore } from "date-fns";
import partition from "lodash/partition";
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";
import { useQuery } from "react-query";
import { Link as ReactRouterLink } from "react-router-dom";

import type {
  Ressurs,
  RessursOrgTilknytning,
} from "~/api/generated/graphql_generated";
import * as Gq from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/components/CreateResourceForm";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { formatDisplayDate, formatTlf, isInDevelopment } from "~/utils";

export function DetailedRessurs({ ressurs }: { ressurs: Ressurs }) {
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
      </div>
    </div>
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
  if (ressurs.orgTilknytning.length === 0) {
    return <></>;
  }

  const [expiredOrgTilknytninger, activeOrgtilknytninger] = partition(
    ressurs.orgTilknytning,
    (orgtilknytning) =>
      orgtilknytning.gyldigTom &&
      isBefore(new Date(orgtilknytning.gyldigTom), new Date()),
  );

  return (
    <>
      <Divider />
      <Heading level="2" size="small">
        <Buldings3Icon />
        Organisatorisk tilhørighet
      </Heading>
      {activeOrgtilknytninger.map((orgTilknytning) => (
        <OrganisationHierarchyTree
          key={orgTilknytning.orgEnhet.id}
          orgTilknytning={orgTilknytning}
          ressurs={ressurs}
        />
      ))}
      <ExpiredOrgTilknytninger orgTilknytninger={expiredOrgTilknytninger} />
    </>
  );
}

function ExpiredOrgTilknytninger({
  orgTilknytninger,
}: {
  orgTilknytninger: RessursOrgTilknytning[];
}) {
  const [showAll, setShowAll] = useState(false);
  if (orgTilknytninger.length === 0) {
    return <></>;
  }

  const tilknyttingerToShow = orgTilknytninger.slice(
    0,
    showAll ? orgTilknytninger.length : 3,
  );

  const shouldDisplayShowAllButton = orgTilknytninger.length > 3 && !showAll;

  return (
    <>
      <Heading level="2" size="xsmall">
        Avsluttede org.tilknytninger
      </Heading>
      {tilknyttingerToShow.map((orgTilknytning) => (
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
          key={orgTilknytning.orgEnhet.id}
        >
          <Heading level="3" size="medium">
            {orgTilknytning.orgEnhet.navn}
          </Heading>
          <span>
            {formatDisplayDate(orgTilknytning.gyldigFom)} -{" "}
            {formatDisplayDate(orgTilknytning.gyldigTom)}
          </span>
        </div>
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

function OrganisationHierarchyTree({
  ressurs,
  orgTilknytning,
}: {
  orgTilknytning: RessursOrgTilknytning;
  ressurs: Ressurs;
}) {
  const hentHierarkiQuery = Gq.useHentHierarkiQuery({
    variables: {
      id: orgTilknytning.orgEnhet.id ?? "",
    },
  });

  const orgHierarki = hentHierarkiQuery.data;

  if (!orgHierarki) {
    return <></>;
  }

  const orgHierarkiLeder = orgHierarki.enhet?.leder[0]?.ressurs;

  const { gyldigTom, gyldigFom, erDagligOppfolging } = orgTilknytning;

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
    >
      <ExpansionCard.Header>
        <ExpansionCard.Title>
          <span>{orgHierarki.enhet?.navn}</span>
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
          Leder:{" "}
          <Link
            as={ReactRouterLink}
            to={`/ressurs/${orgHierarkiLeder?.navident}`}
          >
            {orgHierarkiLeder?.visningsnavn}
          </Link>
          {erDagligOppfolging ? (
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
        <CompactOrganisasjonHierarki
          orgHierarki={orgHierarki}
          ressurs={ressurs}
        />
      </ExpansionCard.Content>
    </ExpansionCard>
  );
}

function LeaderForSection({ ressurs }: { ressurs: Ressurs }) {
  if (ressurs.lederFor.length === 0) {
    return <></>;
  }

  /*
   Leder høyt oppe i hierarkiet er ofte leder i flere "like" orgEnheter.
   Her antas det at første forekomst av navn for en orgEnhet er den høyest i hierarkiet.
   Se https://jira.adeo.no/browse/ORG-1437 for flere detaljer.
  */
  const uniqLederFor = uniqBy(
    ressurs.lederFor,
    ({ orgEnhet }) => orgEnhet.navn,
  );

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
        {uniqLederFor.map(({ orgEnhet }) => (
          <Link
            as={ReactRouterLink}
            key={orgEnhet.id}
            to={`/org/${orgEnhet.id}`}
          >
            {orgEnhet.navn}
          </Link>
        ))}
      </div>
    </>
  );
}

function SummarySection({ ressurs }: { ressurs: Ressurs }) {
  const dagligLeder = ressurs.ledere.find((leder) => leder.erDagligOppfolging);

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
        <Heading
          css={css`
            margin-bottom: var(--a-spacing-8);
          `}
          level="1"
          size="large"
        >
          {ressurs.visningsnavn}
        </Heading>
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
            column-gap: 1rem;
            row-gap: 0.5rem;
          `}
        >
          <RessursInformationDetail
            label="NAV-ident"
            value={ressurs.navident}
          />
          <RessursInformationDetail
            label="Sektor"
            value={ressurs.sektor
              .map((sektor) => SektorNameMapping[sektor])
              .join(", ")}
          />
          <RessursInformationDetail label="E-post" value={ressurs.epost} />
          <RessursInformationDetail
            label="Telefon"
            value={ressurs.telefon
              .map(({ nummer }) => formatTlf(nummer))
              .join(", ")}
          />
          {dagligLeder ? (
            <>
              <span>Leder:</span>
              <div
                css={css`
                  display: flex;
                  gap: 0.5rem;
                `}
              >
                <Link
                  as={ReactRouterLink}
                  to={`/ressurs/${dagligLeder.ressurs.navident}`}
                >
                  <Label as="span">{dagligLeder.ressurs.visningsnavn}</Label>
                </Link>
                {dagligLeder && (
                  <>
                    <span>-</span>
                    <span>Daglig oppfølging</span>
                    <HelpText title="Hva er daglig leder">
                      Denne lederen har det daglige ansvaret for
                      sykefraværsoppfølging, godkjenning i MinWinTid, og har
                      tilgang til den ansatte i Læringsportalen
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
        navn={ressurs.visningsnavn ?? ""}
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
  const { data } = useQuery(["memberships", navident], () =>
    axios.get(`/teamcat/member/membership/${navident}`),
  );
  const memberships = data?.data;
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
