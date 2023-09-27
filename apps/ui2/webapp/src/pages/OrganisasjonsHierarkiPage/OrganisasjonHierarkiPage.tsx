import { css } from "@emotion/react";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { useParams } from "@tanstack/react-router";
import partition from "lodash/partition";
import sortBy from "lodash/sortBy";
import uniqBy from "lodash/uniqBy";
import { Fragment } from "react";
import React from "react";

import type { HentHierarkiQuery } from "~/api/generated/graphql_generated";
import { useHentHierarkiQuery } from "~/api/generated/graphql_generated";
import { PageBanner } from "~/components/PageBanner";
import { orgRoute } from "~/reactRouterRoutes";
import { isGyldighetsPeriodeActive } from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";
import {
  DynamicColumnContainer,
  OrgEnhetView,
  OverEnheterDivider,
  ResourceLink,
} from "./components/CardViews";
import { ValgtEnhet } from "./components/ValgtEnhet";

function sortUnderenheterForHierarchyFrontPage(
  orgHierarchy: HentHierarkiQuery,
) {
  const underEnheter = (orgHierarchy?.enhetsKoblinger?.underenheter ?? []).map(
    ({ organisasjonsenhet }) => organisasjonsenhet,
  );

  const collator = new Intl.Collator("no", { sensitivity: "base" });
  underEnheter.sort((a, b) => collator.compare(a.navn, b.navn));

  if (orgHierarchy?.enhet?.navn !== "NAV") {
    return underEnheter;
  }

  const [direktoratet, linjene] = partition(
    underEnheter,
    (enhet) => enhet?.navn === "Arbeids- og velferdsdirektoratet",
  );

  return [...direktoratet, ...linjene];
}

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
  hierarchyEntry: NonNullable<HentHierarkiQuery>;
}) {
  const firstOverEnhet = (
    hierarchyEntry.enhetsKoblinger?.overenheter ?? []
  ).map(({ organisasjonsenhet }) => organisasjonsenhet)[0];
  const overEnheter = firstOverEnhet
    ? getOverEnheterBackToRootRecursively(firstOverEnhet)
    : [];
  const underEnheter = sortUnderenheterForHierarchyFrontPage(hierarchyEntry);

  const valgtEnhet = hierarchyEntry.enhet;

  const ressurser = (hierarchyEntry.ressursKoblinger?.koblinger ?? [])
    .filter((kobling) =>
      isGyldighetsPeriodeActive(kobling.gyldigFom, kobling.gyldigTom),
    )
    .map(({ ressurs }) => ressurs);

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

      <>
        {valgtEnhet && <ValgtEnhet valgtEnhet={valgtEnhet} />}
        <DynamicColumnContainer
          shouldUseDynamicSecondColumn={underEnheter.length > 5}
        >
          {underEnheter.map((underEnhet, index) => (
            <Fragment key={underEnhet.id}>
              {valgtEnhet?.navn === "NAV" && index === 0 && (
                <Heading size="small">Direktoratet</Heading>
              )}
              {valgtEnhet?.navn === "NAV" && index === 1 && (
                <Heading size="small">Linjene</Heading>
              )}
              <OrgEnhetView autoFocus={index === 0} orgEnhet={underEnhet} />
            </Fragment>
          ))}
        </DynamicColumnContainer>

        <DynamicColumnContainer
          shouldUseDynamicSecondColumn={ressurser.length > 5}
        >
          {sortBy(
            uniqBy(ressurser, (ressurs) => ressurs.navident),
            "visningsnavn",
          ).map((ressurs, index) => (
            <ResourceLink
              autoFocus={index === 0}
              key={ressurs.navident}
              ressurs={ressurs}
            />
          ))}
        </DynamicColumnContainer>
      </>
    </div>
  );
}

export function OrganisasjonHierarkiPage() {
  const { id } = useParams({ from: orgRoute.id });

  const hentHierarkiQuery = useHentHierarkiQuery({
    variables: {
      id: id ?? "",
    },
  });

  let innerContent;
  if (hentHierarkiQuery.data) {
    innerContent = (
      <OrganisasjonHierarkiPageInner hierarchyEntry={hentHierarkiQuery.data} />
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
