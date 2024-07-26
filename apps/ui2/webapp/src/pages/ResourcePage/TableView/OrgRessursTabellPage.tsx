import { css } from "@emotion/react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { Alert, Checkbox, Heading, Link, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  getAlleUnderliggendeOrgEnheter,
  getOrgEnhet,
  isOrgEnhetArbeidsomraade,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import { OrgRessursTabellPageInner } from "~/pages/ResourcePage/TableView/OrgRessursTabellPageInner";
import type { OrgEnhet } from "~/types/api-models";

export const OrgRessursTabellPage = () => {
  const { id } = useParams({ from: "/org/table/$id" });

  const isOrgEnhetArbeidsomraadeQuery = useQuery({
    queryKey: isOrgEnhetArbeidsomraade.queryKey(id as string),
    queryFn: () => isOrgEnhetArbeidsomraade.queryFn(id as string),
    enabled: !!id,
  });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id as string),
    queryFn: () => getOrgEnhet.queryFn(id as string),
    enabled: !!id,
  });

  const underliggendeOrgEnhetQuery = useQuery({
    queryKey: getAlleUnderliggendeOrgEnheter.queryKey(id as string),
    queryFn: () => getAlleUnderliggendeOrgEnheter.queryFn(id as string),
    enabled: !!id,
  });

  const [includeUnderliggende, setIncludeUnderliggende] = useState(true);

  let kombinertOrgEnheter: (OrgEnhet | undefined)[] = [];

  if (includeUnderliggende && underliggendeOrgEnhetQuery.data) {
    kombinertOrgEnheter = [
      orgEnhetQuery.data,
      ...underliggendeOrgEnhetQuery.data,
    ];
  } else if (orgEnhetQuery.data) {
    kombinertOrgEnheter = [orgEnhetQuery.data];
  }

  const filteredOrgEnheter: OrgEnhet[] = kombinertOrgEnheter as OrgEnhet[];

  if (orgEnhetQuery.isLoading || underliggendeOrgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          align-items: center;
          justify-content: center;
        `}
      >
        <Loader size="3xlarge" variant="neutral" />
      </div>
    );
  }

  if (isOrgEnhetArbeidsomraadeQuery.data?.valueOf() === false) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Denne organisasjonsenheten tilhører ikke et arbeidsområde
        </Heading>
      </Alert>
    );
  }

  if (orgEnhetQuery.isError) {
    return (
      <Alert variant="error">
        <Heading level="3" size="small">
          Kunne ikke hente organisajoner
        </Heading>
      </Alert>
    );
  }

  if (
    isOrgEnhetArbeidsomraadeQuery.data?.valueOf() === true &&
    orgEnhetQuery.data &&
    underliggendeOrgEnhetQuery.data
  ) {
    return (
      <>
        <PageBanner text={"Medarbeidere i " + orgEnhetQuery.data?.navn || ""} />
        <ContentWrapper>
          <Link
            css={css`
              color: #000;
              font-size: 1.2rem;
              margin-bottom: 20px;
            `}
            href={`/org/${id}`}
          >
            <ChevronLeftIcon fontSize="1.8rem" />
            <b>Tilbake til {orgEnhetQuery.data?.navn}</b>
          </Link>
          <Checkbox
            checked={includeUnderliggende}
            css={css`
              color: #000;
              font-size: 1.2rem;
              margin-bottom: 20px;
            `}
            onChange={() => {
              setIncludeUnderliggende(!includeUnderliggende);
            }}
            value="underliggende-organisasjonsenheter"
          >
            Vis medarbeidere fra underliggende organisasjonsenheter
          </Checkbox>
          <OrgRessursTabellPageInner orgEnheter={filteredOrgEnheter} />
        </ContentWrapper>
      </>
    );
  }
};
