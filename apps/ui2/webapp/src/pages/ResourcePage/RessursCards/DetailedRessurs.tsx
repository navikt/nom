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
import { Link as ReactRouterLink } from "@tanstack/react-router";
import axios from "axios";
import { isAfter, isBefore } from "date-fns";
import uniqBy from "lodash/uniqBy";
import React, { useState } from "react";
import { useQuery } from "react-query";

import type { HentRessursMedNavidentQuery } from "~/api/generated/graphql_generated";
import { useHentHierarkiQuery } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { CompactOrganisasjonHierarki } from "~/pages/ResourcePage/CompactOrganisasjonHierarki";
import { orgRoute, ressursRoute } from "~/reactRouterRoutes";
import {
  formatDisplayDate,
  formatTlf,
  isGyldighetsPeriodeActive,
  isInDevelopment,
} from "~/utils";

type Ressurs = NonNullable<HentRessursMedNavidentQuery["ressurs"]>;
type OrgTilknytninger = Ressurs["orgTilknytning"];

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

  const activeOrgtilknytninger = ressurs.orgTilknytning.filter(
    (orgTilknytning) =>
      isGyldighetsPeriodeActive(
        orgTilknytning.gyldigFom,
        orgTilknytning.gyldigTom,
      ),
  );

  const expiredOrgTilknytninger = ressurs.orgTilknytning.filter(
    (orgTilknytning) =>
      orgTilknytning.gyldigTom &&
      isBefore(new Date(orgTilknytning.gyldigTom), new Date()),
  );

  const futureOrgTilknytninger = ressurs.orgTilknytning.filter(
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
      {activeOrgtilknytninger.map((orgTilknytning) => (
        <OrganisationHierarchyTree
          key={orgTilknytning.orgEnhet.id}
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
    </>
  );
}

function FutureOrgTilknytninger({
  futureOrgTilknytninger,
  ressurs,
}: {
  futureOrgTilknytninger: NonNullable<
    HentRessursMedNavidentQuery["ressurs"]
  >["orgTilknytning"];
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
          key={orgTilknytning.orgEnhet.id}
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
  expiredOrgTilknytninger: OrgTilknytninger;
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
  orgTilknytning: OrgTilknytninger[0];
  ressurs: Ressurs;
}) {
  const hentHierarkiQuery = useHentHierarkiQuery({
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
      size="small"
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
          Leder: {orgHierarkiLeder?.visningsnavn}
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
                <ReactRouterLink
                  params={{ id: dagligLeder.ressurs.navident }}
                  to={ressursRoute.to}
                >
                  <Link as="span">{dagligLeder.ressurs.visningsnavn}</Link>
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
