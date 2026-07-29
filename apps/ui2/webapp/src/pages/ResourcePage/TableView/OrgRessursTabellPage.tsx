import { css } from "@emotion/react";
import { ChevronLeftIcon, FileExcelIcon } from "@navikt/aksel-icons";
import {
  Alert,
  BodyShort,
  Button,
  Checkbox,
  Heading,
  Loader,
} from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React, { useState } from "react";

import {
  downloadExcelByListNavident,
  getAlleUnderliggendeOrgEnheter,
  getOrgEnhet,
  isOrgEnhetDriftsenhet,
} from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { isPeriodActiveOrFuture } from "~/pages/ResourcePage/RessursComponents/dolUtils";
import { OrgRessursTabellPageInner } from "~/pages/ResourcePage/TableView/OrgRessursTabellPageInner";
import type { ExcelExportRequest, OrgEnhet } from "~/types/api-models";
import { isEksternSamarbeidspartnerOrgEnhet } from "~/utils";

export const OrgRessursTabellPage = () => {
  const { id } = useParams({ from: "/org/resources-table/$id" });

  const isOrgEnhetDriftsenhetQuery = useQuery({
    queryKey: isOrgEnhetDriftsenhet.queryKey(id),
    queryFn: () => isOrgEnhetDriftsenhet.queryFn(id),
    enabled: !!id,
  });

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(id),
    queryFn: () => getOrgEnhet.queryFn(id),
    enabled: !!id && isOrgEnhetDriftsenhetQuery.isSuccess,
  });

  const underliggendeOrgEnhetQuery = useQuery({
    queryKey: getAlleUnderliggendeOrgEnheter.queryKey(id),
    queryFn: () => getAlleUnderliggendeOrgEnheter.queryFn(id),
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

  const eksternSamarbeidspartner = isEksternSamarbeidspartnerOrgEnhet(id);

  const [isDownloading, setIsDownloading] = useState(false);

  const excelFilNavn = () => {
    return includeUnderliggende &&
      underliggendeOrgEnhetQuery.isSuccess &&
      underliggendeOrgEnhetQuery.data.length > 0
      ? "Medarbeidere i " + orgEnhetQuery.data?.navn + " og underliggende.xlsx"
      : "Medarbeidere i " + orgEnhetQuery.data?.navn + ".xlsx";
  };

  const handleDownloadExcel = async () => {
    const excelExportRequest = {
      navidenter: ressurserNavidents,
      topOrgEnhetId: orgEnhetQuery.data?.id,
    };
    setIsDownloading(true);

    await downloadExcelByListNavident(
      excelExportRequest as ExcelExportRequest,
      excelFilNavn(),
    );

    setIsDownloading(false);
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
        <Loader size="3xlarge" />
      </div>
    );
  }

  if (
    isOrgEnhetDriftsenhetQuery.data?.valueOf() === false &&
    !eksternSamarbeidspartner
  ) {
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
    (isOrgEnhetDriftsenhetQuery.data?.valueOf() === true ||
      eksternSamarbeidspartner) &&
    orgEnhetQuery.data &&
    underliggendeOrgEnhetQuery.data
  ) {
    return (
      <>
        <PageBanner text={"Medarbeidere i " + orgEnhetQuery.data?.navn || id} />

        <ContentWrapper limitWidth={false}>
          <SmartLink
            css={css`
              display: flex;
              width: fit-content;
              font-size: 1.2rem;
              color: #000;
            `}
            href={`/org/${id}`}
          >
            <span
              css={css`
                display: flex;
                align-items: center;
                font-size: 1.2rem;
              `}
            >
              <ChevronLeftIcon fontSize="1.8rem" />
              <b>Tilbake til {orgEnhetQuery.data?.navn}</b>
            </span>
          </SmartLink>

          <div
            css={css`
              display: flex;
              justify-content: space-between;
              align-items: flex-start;
              margin-top: var(--a-spacing-1);
            `}
          >
            <div>
              <Checkbox
                checked={includeUnderliggende}
                css={css`
                  color: #000;
                  font-size: 1.2rem;
                `}
                onChange={(event) => {
                  setIncludeUnderliggende(event.target.checked);
                }}
                value="underliggende-organisasjonsenheter"
              >
                Vis medarbeidere fra underliggende organisasjonsenheter
              </Checkbox>

              <BodyShort
                css={css`
                  margin-top: var(--a-spacing-2);
                `}
              >
                Antall medarbeidere: <b>{ressurserNavidents.length}</b>
              </BodyShort>
            </div>

            <Button
              css={css`
                align-self: end;
              `}
              icon={<FileExcelIcon />}
              loading={isDownloading}
              onClick={handleDownloadExcel}
              variant="secondary"
            >
              Eksportér til Excel
            </Button>
          </div>

          <OrgRessursTabellPageInner
            orgEnheter={filteredOrgEnheter}
            ressurserNavidents={ressurserNavidents}
            toppOrgEnhetId={orgEnhetQuery.data?.id}
          />
        </ContentWrapper>
      </>
    );
  }
};
