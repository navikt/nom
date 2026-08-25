import { css } from "@emotion/react";
import { Alert, Heading, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { PageBanner } from "~/components/PageBanner";
import { HierarkiFrontPage } from "~/pages/OrganisasjonsHierarkiPage/components/HierarkiFrontPage";
import { UnderEnheterGraphic } from "~/pages/OrganisasjonsHierarkiPage/components/UnderEnheterGraphic";
import type { OrgEnhet } from "~/types/api-models";

import ContentWrapper from "../../components/ContentWrapper";
import { OrgEnhetView, OverEnheterDivider } from "./components/CardViews";
import { ValgtOrgEnhet } from "./components/ValgtOrgEnhet";

function OrganisasjonHierarkiPageInner({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const underEnheter = orgEnhet.underordnedeOrgEnhetId;

  return (
    <div
      css={css`
        display: flex;
        flex-direction: column;
      `}
    >
      <OverEnheter isRoot orgEnhetId={orgEnhet.id} />

      <ValgtOrgEnhet orgEnhet={orgEnhet} />
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

export function OrganisasjonHierarkiPage() {
  const { id } = useParams({ from: "/org/$id" });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id as string),
    queryFn: () => getOrgEnhet.queryFn(id as string),
    enabled: !!id,
  });

  let innerContent;
  if (orgEnhetQuery.data) {
    innerContent =
      orgEnhetQuery.data.navn.toLowerCase() === "nav" ? (
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
      <PageBanner text="Nav organisasjon" />
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
