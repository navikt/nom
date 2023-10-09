import { css } from "@emotion/react";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { useParams } from "@tanstack/react-router";
import sortBy from "lodash/sortBy";
import uniqBy from "lodash/uniqBy";
import { Fragment } from "react";
import React from "react";

import type { HentHierarkiQuery } from "~/api/generated/graphql_generated";
import { useHentHierarkiQuery } from "~/api/generated/graphql_generated";
import { PageBanner } from "~/components/PageBanner";
import { HierarkiFrontPage } from "~/pages/OrganisasjonsHierarkiPage/components/HierarkiFrontPage";
import { orgRoute } from "~/reactRouterRoutes";
import { isGyldighetsPeriodeActive } from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";
import { OrgEnhetView, OverEnheterDivider } from "./components/CardViews";
import { ValgtOrgEnhet } from "./components/ValgtOrgEnhet";

type OverEnhet = NonNullable<
  NonNullable<HentHierarkiQuery["enhetsKoblinger"]>["overenheter"]
>[0]["organisasjonsenhet"];

export function getOverEnheterBackToRootRecursively(
  orgEnhet: OverEnhet,
): OverEnhet[] {
  if (!orgEnhet || !orgEnhet.organiseringer) {
    return [];
  }

  const firstOrganisering = orgEnhet.organiseringer[0]?.organisasjonsenhet;

  return firstOrganisering
    ? [
        // eslint-disable-next-line @typescript-eslint/no-explicit-any -- for some reason recursion does not agree with tsc
        ...getOverEnheterBackToRootRecursively(firstOrganisering as any),
        orgEnhet,
      ]
    : [orgEnhet];
}

function OrganisasjonHierarkiPageInner({
  hierarchyEntry,
}: {
  hierarchyEntry: HentHierarkiQuery;
}) {
  const firstOverEnhet = (
    hierarchyEntry.enhetsKoblinger?.overenheter ?? []
  ).map(({ organisasjonsenhet }) => organisasjonsenhet)[0];

  const overEnheter = firstOverEnhet
    ? getOverEnheterBackToRootRecursively(firstOverEnhet)
    : [];
  const underEnheter = (
    hierarchyEntry?.enhetsKoblinger?.underenheter ?? []
  ).map(({ organisasjonsenhet }) => organisasjonsenhet);

  const valgtEnhet = hierarchyEntry.enhet;

  const ressurser = (hierarchyEntry.ressursKoblinger?.koblinger ?? [])
    .filter((kobling) =>
      isGyldighetsPeriodeActive(kobling.gyldigFom, kobling.gyldigTom),
    )
    .map(({ ressurs }) => ressurs);

  const sortedRessurser = sortBy(
    uniqBy(ressurser, (ressurs) => ressurs.navident),
    "visningsnavn",
  );

  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
      `}
    >
      {overEnheter.map((overEnhet) => (
        <Fragment key={overEnhet.id}>
          <OrgEnhetView orgEnhet={overEnhet} />
          <OverEnheterDivider />
        </Fragment>
      ))}

      {valgtEnhet && (
        <>
          <ValgtOrgEnhet ressurser={sortedRessurser} valgtEnhet={valgtEnhet} />
          {underEnheter.length > 0 && (
            <UnderEnheterGraphic valgtEnhet={valgtEnhet} />
          )}
        </>
      )}

      <div
        css={css`
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: var(--a-spacing-4);
          margin-left: var(--a-spacing-24);
        `}
      >
        {underEnheter.map((underEnhet, index) => (
          <OrgEnhetView
            autoFocus={index === 0}
            key={underEnhet.id}
            orgEnhet={underEnhet}
          />
        ))}
      </div>
    </div>
  );
}

export function UnderEnheterGraphic({
  valgtEnhet,
}: {
  valgtEnhet: NonNullable<HentHierarkiQuery["enhet"]>;
}) {
  return (
    <div
      css={css`
        display: flex;
        align-items: flex-end;
        gap: var(--a-spacing-2);
        margin-left: 65px;
        font-weight: var(--a-font-weight-bold);
        margin-bottom: var(--a-spacing-2);
      `}
    >
      <div
        css={css`
          width: 24px;
          height: 40px;
          margin-bottom: 10px;
          border-left: 6px solid var(--nom-primary);
          border-bottom: 6px solid var(--nom-primary);
        `}
      />
      Organisasjonsenheter under {valgtEnhet.navn}
    </div>
  );
}

export function OrganisasjonHierarkiPage() {
  const { id } = useParams({ from: orgRoute.id });

  const hentHierarkiQuery = useHentHierarkiQuery({
    variables: {
      id: id,
    },
    fetchPolicy: "cache-first",
    returnPartialData: true,
  });

  let innerContent;
  if (hentHierarkiQuery.data) {
    innerContent =
      hentHierarkiQuery.data.enhet?.navn === "NAV" ? (
        <HierarkiFrontPage hierarchyEntry={hentHierarkiQuery.data} />
      ) : (
        <OrganisasjonHierarkiPageInner
          hierarchyEntry={hentHierarkiQuery.data}
        />
      );
  } else if (hentHierarkiQuery.loading) {
    innerContent = (
      <div
        css={css`
          justify-self: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  } else if (hentHierarkiQuery.error) {
    innerContent = (
      <Alert variant="error">
        <Heading level="3" size="small">
          Kunne ikke hente hierarki
        </Heading>
      </Alert>
    );
  }

  return (
    <>
      <PageBanner text="NAV organisasjon" />
      <ContentWrapper>
        <div
          css={css`
            display: grid;
          `}
        >
          {innerContent}
        </div>
      </ContentWrapper>
    </>
  );
}
