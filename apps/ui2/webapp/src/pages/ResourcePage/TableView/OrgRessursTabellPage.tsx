import { css } from "@emotion/react";
import { ChevronLeftIcon, FileExcelIcon } from "@navikt/aksel-icons";
import {
  Alert,
  Button,
  Checkbox,
  Heading,
  Link,
  Loader,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  downloadExcelByListNavident,
  getAlleUnderliggendeOrgEnheter,
  getOrgEnhet,
  isOrgEnhetArbeidsomraade,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import { OrgRessursTabellPageInner } from "~/pages/ResourcePage/TableView/OrgRessursTabellPageInner";
import type { OrgEnhet } from "~/types/api-models";
import { isPeriodActiveOrFuture } from "~/utils";

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
    enabled: !!id && isOrgEnhetArbeidsomraadeQuery.isSuccess,
  });

  const underliggendeOrgEnhetQuery = useQuery({
    queryKey: getAlleUnderliggendeOrgEnheter.queryKey(id as string),
    queryFn: () => getAlleUnderliggendeOrgEnheter.queryFn(id as string),
    enabled: !!id && orgEnhetQuery.isSuccess,
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

  const allRessursKoblinger = filteredOrgEnheter.flatMap((orgEnhet) =>
    orgEnhet.ressursKoblinger.filter((kobling) =>
      isPeriodActiveOrFuture(kobling.gyldigFom, kobling.gyldigTom),
    ),
  );

  const ressurserNavidents = [
    ...new Set(allRessursKoblinger.map((tilknytning) => tilknytning.navident)),
  ];

  const excelFilNavn = () => {
    return includeUnderliggende &&
      underliggendeOrgEnhetQuery.isSuccess &&
      underliggendeOrgEnhetQuery.data.length > 0
      ? "Medarbeidere i " + orgEnhetQuery.data?.navn + " og underliggende.xlsx"
      : "Medarbeidere i " + orgEnhetQuery.data?.navn + ".xlsx";
  };

  const handleDownloadExcel = async () => {
    await downloadExcelByListNavident(ressurserNavidents, excelFilNavn());
  };

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

        <ContentWrapper limitWidth={false}>
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
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <Checkbox
              checked={includeUnderliggende}
              css={css`
                color: #000;
                font-size: 1.2rem;
                margin-bottom: 20px;
              `}
              onChange={(event) => {
                setIncludeUnderliggende(event.target.checked);
              }}
              value="underliggende-organisasjonsenheter"
            >
              Vis medarbeidere fra underliggende organisasjonsenheter
            </Checkbox>
            <div>
              <Button
                icon={<FileExcelIcon />}
                onClick={handleDownloadExcel}
                variant="secondary"
              >
                Til Excel
              </Button>
            </div>
          </div>
          <OrgRessursTabellPageInner
            orgEnheter={filteredOrgEnheter}
            ressurserNavidents={ressurserNavidents}
          />
        </ContentWrapper>
      </>
    );
  }
};
