import { css } from "@emotion/react";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { PageBanner } from "~/components/PageBanner";
import { HierarkiFrontPage } from "~/pages/OrganisasjonsHierarkiPage/components/HierarkiFrontPage";
import { orgRoute } from "~/reactRouterRoutes";
import type { OrgEnhet } from "~/types/api-models";
import { isGyldighetsPeriodeActive } from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";
import { OrgEnhetView, OverEnheterDivider } from "./components/CardViews";
import { ValgtOrgEnhet } from "./components/ValgtOrgEnhet";

function OrganisasjonHierarkiPageInner({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const underEnheter = orgEnhet.underordnedeOrgEnhetId;

  const ressurserNavidents = orgEnhet.ressursKoblinger
    .filter((kobling) =>
      isGyldighetsPeriodeActive(kobling.gyldigFom, kobling.gyldigTom),
    )
    .map((tilknytning) => tilknytning.navident)
    .sort();

  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
      `}
    >
      <OverEnheter isRoot orgEnhetId={orgEnhet.id} />

      <ValgtOrgEnhet orgEnhet={orgEnhet} ressurser={ressurserNavidents} />
      {underEnheter.length > 0 && <UnderEnheterGraphic valgtEnhet={orgEnhet} />}

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
            key={underEnhet}
            orgEnhetId={underEnhet}
          />
        ))}
      </div>
    </div>
  );
}

function OverEnheter({
  orgEnhetId,
  isRoot,
}: {
  orgEnhetId: string;
  isRoot: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const overEnhetId = orgEnhetQuery.data?.overordnetEnhetId;

  return (
    <>
      {overEnhetId && <OverEnheter isRoot={false} orgEnhetId={overEnhetId} />}
      {!isRoot && <OrgEnhetView orgEnhetId={orgEnhetId} />}
      <OverEnheterDivider />
    </>
  );
}

export function UnderEnheterGraphic({ valgtEnhet }: { valgtEnhet: OrgEnhet }) {
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

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id as string),
    queryFn: () => getOrgEnhet.queryFn(id as string),
    enabled: !!id,
  });

  let innerContent;
  if (orgEnhetQuery.data) {
    innerContent =
      orgEnhetQuery.data.navn === "NAV" ? (
        <HierarkiFrontPage orgEnhet={orgEnhetQuery.data} />
      ) : (
        <OrganisasjonHierarkiPageInner orgEnhet={orgEnhetQuery.data} />
      );
  } else if (orgEnhetQuery.isLoading) {
    innerContent = (
      <div
        css={css`
          justify-self: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  } else if (orgEnhetQuery.error) {
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
