import { css } from "@emotion/react";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import partition from "lodash/partition";
import sortBy from "lodash/sortBy";
import uniqBy from "lodash/uniqBy";
import { Fragment } from "react";
import React from "react";
import { useParams } from "react-router";

import type { OrgEnhet, Ressurs } from "~/api/generated/graphql_generated";
import type { HentHierarkiQueryHookResult } from "~/api/generated/graphql_generated";
import { PageBanner } from "~/components/PageBanner";

import * as Gq from "../../api/generated/graphql_generated";
import ContentWrapper from "../../components/ContentWrapper";
import {
  DynamicColumnContainer,
  OrgEnhetView,
  OverEnheterDivider,
  ResourceLink,
} from "./components/CardViews";
import { ValgtEnhet } from "./components/ValgtEnhet";

function sortUnderenheterForHierarchyFrontPage(
  orgHierarchy: HentHierarkiQueryHookResult["data"]
) {
  const underEnheter = (orgHierarchy?.enhetsKoblinger?.underenheter ?? []).map(
    ({ organisasjonsenhet }) => organisasjonsenhet
  );

  const sortedUnderEnheter = sortBy(underEnheter, "navn");

  if (orgHierarchy?.enhet?.navn !== "NAV") {
    return sortedUnderEnheter;
  }

  const [direktoratet, linjene] = partition(
    sortedUnderEnheter,
    (enhet) => enhet?.navn === "Arbeids- og velferdsdirektoratet"
  );

  return [...direktoratet, ...linjene];
}

export function getOverEnheterBackToRootRecursively(
  orgEnhet: OrgEnhet
): OrgEnhet[] {
  if (!orgEnhet || !orgEnhet.organiseringer) {
    return [];
  }

  const firstOrganisering = orgEnhet.organiseringer[0]?.organisasjonsenhet;

  return firstOrganisering
    ? [...getOverEnheterBackToRootRecursively(firstOrganisering), orgEnhet]
    : [orgEnhet];
}

export const OrganisasjonHierarkiPageInner = (properties: {
  data: HentHierarkiQueryHookResult["data"];
}) => {
  const firstOverEnhet = (
    properties.data?.enhetsKoblinger?.overenheter ?? []
  ).map(({ organisasjonsenhet }) => organisasjonsenhet)[0];
  const overEnheter = firstOverEnhet
    ? getOverEnheterBackToRootRecursively(firstOverEnhet as OrgEnhet)
    : [];
  const underEnheter = sortUnderenheterForHierarchyFrontPage(properties.data);

  const valgtEnhet = properties.data?.enhet;

  const ressurser = (properties.data?.ressursKoblinger?.koblinger ?? []).map(
    ({ ressurs }) => ressurs
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

      <>
        {valgtEnhet && <ValgtEnhet valgtEnhet={valgtEnhet as OrgEnhet} />}
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
              <OrgEnhetView
                autoFocus={index === 0}
                orgEnhet={underEnhet as OrgEnhet}
              />
            </Fragment>
          ))}
        </DynamicColumnContainer>

        <DynamicColumnContainer
          shouldUseDynamicSecondColumn={ressurser.length > 5}
        >
          {sortBy(
            uniqBy(ressurser, (ressurs) => ressurs.navident),
            "visningsnavn"
          ).map((ressurs, index) => (
            <ResourceLink
              autoFocus={index === 0}
              key={ressurs.navident}
              ressurs={ressurs as Ressurs}
            />
          ))}
        </DynamicColumnContainer>
      </>
    </div>
  );
};

function OrganisasjonsHierarkiPageOuter() {
  const { id } = useParams();

  const hentHierarkiQuery = Gq.useHentHierarkiQuery({
    variables: {
      id: id ?? "",
    },
  });

  let innerContent;
  if (hentHierarkiQuery.data) {
    innerContent = (
      <OrganisasjonHierarkiPageInner data={hentHierarkiQuery.data} />
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

export default OrganisasjonsHierarkiPageOuter;
