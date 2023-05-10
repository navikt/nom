import { css } from "@emotion/react";
import {
  Buldings3Icon,
  PersonIcon,
  PersonSuitIcon,
  ReceptionIcon,
} from "@navikt/aksel-icons";
import { ExpansionCard, Heading, Label, Link } from "@navikt/ds-react";
import axios from "axios";
import React from "react";
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
import { formatTlf, isInDevelopment } from "~/utils";

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

function Divider() {
  return (
    <div
      css={css`
        height: 1px;
        margin: var(--a-spacing-8) 0;
        background-color: var(--a-gray-400);
      `}
    />
  );
}

function OrgTilknytningSection({ ressurs }: { ressurs: Ressurs }) {
  if (ressurs.orgTilknytning.length === 0) {
    return <></>;
  }

  return (
    <>
      <Divider />
      <Heading level="2" size="small">
        <Buldings3Icon />
        Organisatorisk tilhørighet
      </Heading>
      {ressurs.orgTilknytning.map((orgTilknytning) => (
        <OrganisationHierarchyTree
          key={orgTilknytning.orgEnhet.id}
          orgTilknytning={orgTilknytning}
          ressurs={ressurs}
        />
      ))}
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

  return (
    <ExpansionCard
      aria-label="organisasjons-hierarki"
      css={css`
        margin-bottom: 1rem;
      `}
    >
      <ExpansionCard.Header>
        <ExpansionCard.Title>Data</ExpansionCard.Title>
        <ExpansionCard.Description>
          Leder:{" "}
          <Link
            as={ReactRouterLink}
            to={`/ressurs/${orgHierarkiLeder?.navident}`}
          >
            {orgHierarkiLeder?.visningsnavn}
          </Link>
        </ExpansionCard.Description>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
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
        {ressurs.lederFor.map(({ orgEnhet }) => (
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
    axios.get(`/teamcat/member/membership/${navident}`)
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
          Se mer i teamkatalogen
        </Link>
      </div>
    </>
  );
}
