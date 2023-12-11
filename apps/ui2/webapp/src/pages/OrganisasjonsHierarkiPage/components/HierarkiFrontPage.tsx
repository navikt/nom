import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import React, { Fragment } from "react";

import { OrgEnhetView } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { ValgtOrgEnhet } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { UnderEnheterGraphic } from "~/pages/OrganisasjonsHierarkiPage/OrganisasjonHierarkiPage";
import type { OrgEnhet } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

const DIREKTORAT_ORGENHET_ID = isInDevelopment() ? "fa965e" : "bu431e";

export function HierarkiFrontPage({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const underEnheterOrgEnhetIds = [
    DIREKTORAT_ORGENHET_ID,
    ...orgEnhet.underordnedeOrgEnhetId.filter(
      (id) => id !== DIREKTORAT_ORGENHET_ID,
    ),
  ];

  return (
    <>
      <ValgtOrgEnhet orgEnhet={orgEnhet} ressurser={[]} />
      <UnderEnheterGraphic valgtEnhet={orgEnhet} />
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
          margin-left: var(--a-spacing-24);
          margin-right: var(--a-spacing-12);

          h1 {
            margin-bottom: --var(--a-spacing-2);
          }
        `}
      >
        {underEnheterOrgEnhetIds.map((underEnhetId, index) => (
          <Fragment key={underEnhetId}>
            {index === 0 && <Heading size="small">Direktoratet</Heading>}
            {index === 1 && <Heading size="small">Linjene</Heading>}
            <OrgEnhetView autoFocus={index === 0} orgEnhetId={underEnhetId} />
          </Fragment>
        ))}
      </div>
    </>
  );
}
