import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import partition from "lodash/partition";
import React, { Fragment } from "react";

import type { HentHierarkiQuery } from "~/api/generated/graphql_generated";
import { OrgEnhetView } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ValgtOrgEnhet } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { UnderEnheterGraphic } from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";

export function HierarkiFrontPage({
  hierarchyEntry,
}: {
  hierarchyEntry: HentHierarkiQuery;
}) {
  const underEnheter = sortUnderenheterForHierarchyFrontPage(hierarchyEntry);
  const valgtEnhet = hierarchyEntry.enhet;

  if (!valgtEnhet) {
    return <></>;
  }

  return (
    <>
      <ValgtOrgEnhet ressurser={[]} valgtEnhet={valgtEnhet} />
      <UnderEnheterGraphic valgtEnhet={valgtEnhet} />
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
          margin-left: var(--a-spacing-24);
          margin-right: var(--a-spacing-12);

          h1 {
            margin-bottom: -0.5rem;
          }
        `}
      >
        {underEnheter.map((underEnhet, index) => (
          <Fragment key={underEnhet.id}>
            {index === 0 && <Heading size="small">Direktoratet</Heading>}
            {index === 1 && <Heading size="small">Linjene</Heading>}
            <OrgEnhetView autoFocus={index === 0} orgEnhet={underEnhet} />
          </Fragment>
        ))}
      </div>
    </>
  );
}

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
