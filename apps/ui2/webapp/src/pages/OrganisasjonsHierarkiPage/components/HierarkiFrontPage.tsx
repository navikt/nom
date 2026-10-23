import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import React, { Fragment } from "react";

import { direktoratOrgEnhetId } from "~/customUtils/environmentUtils";
import { OrgEnhetView } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { UnderEnheterGraphic } from "~/pages/OrganisasjonsHierarkiPage/components/UnderEnheterGraphic";
import { ValgtOrgEnhet } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import type { OrgEnhet } from "~/types/api-models";

const direktoratId = direktoratOrgEnhetId();

export function HierarkiFrontPage({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const underEnheterOrgEnhetIds = [
    direktoratId,
    ...orgEnhet.underordnedeOrgEnhetId.filter((id) => id !== direktoratId),
  ];

  return (
    <>
      <ValgtOrgEnhet hideRessurser orgEnhet={orgEnhet} />
      <UnderEnheterGraphic valgtEnhet={orgEnhet} />
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
          margin-left: var(--a-spacing-24);
          margin-right: var(--a-spacing-12);
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
