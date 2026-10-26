import { css } from "@emotion/react";
import { BodyShort, Loader, Table } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import React from "react";

import { getParentOgUnderliggende } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { navToppnodeOrgEnhetId } from "~/customUtils/environmentUtils";
import { useTableSort } from "~/hooks/useTableSort";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import type { OrgEnhet } from "~/types/api-models";

export const TabellvisningOrgEnheter = () => {
  const { id = navToppnodeOrgEnhetId() } = useSearch({
    from: "/org/table",
  });

  const parentOgUnderenheterQuery = useQuery(getParentOgUnderliggende(id));

  const parentOgUnderenheter = parentOgUnderenheterQuery.data ?? [];

  const parent = parentOgUnderenheter.find((orgEnhet) => orgEnhet.id === id);

  return (
    <>
      <PageBanner
        text={`Organisasjonsenheter under ${parent?.navn ?? "enheten"}`}
      />

      <ContentWrapper limitWidth={false}>
        <OrgEnhetTabell
          loading={parentOgUnderenheterQuery.isLoading}
          orgEnheter={parentOgUnderenheter}
        />
      </ContentWrapper>
    </>
  );
};

function OrgEnhetTabell({
  orgEnheter,
  loading = false,
}: {
  orgEnheter: OrgEnhet[];
  loading?: boolean;
}) {
  const { data, sort, handleSort } = useTableSort(orgEnheter, "navn");

  if (loading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  return (
    <Table
      css={css`
        background-color: #fff;
        padding: 6px;
        border-radius: 5px;

        --ac-table-row-border: #071a3636;
      `}
      onSortChange={(sortKey) => handleSort(sortKey)}
      size="small"
      sort={sort}
      zebraStripes
    >
      <Table.Header
        css={css`
          border-bottom: 2px;
        `}
      >
        <Table.Row>
          <Table.ColumnHeader sortKey="navn" sortable>
            Navn
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="leder.visningsnavn" sortable>
            Leder
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="nomNivaa" sortable>
            NOM-nivå
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="orgEnhetsType" sortable>
            Type
          </Table.ColumnHeader>
          <Table.ColumnHeader sortKey="tilgangsenhetId" sortable>
            Tilgangsenhet
          </Table.ColumnHeader>
        </Table.Row>
      </Table.Header>

      <Table.Body
        css={css`
          height: 48px;
        `}
      >
        {data.map((orgEnhet) => (
          <Table.Row
            css={css`
              height: 48px;
            `}
            key={orgEnhet.id}
            shadeOnHover={false}
          >
            <Table.DataCell>
              <SmartLink href={`/org/${orgEnhet.id}`} target="_blank">
                {`${orgEnhet.navn} (${orgEnhet.id})`}
              </SmartLink>
            </Table.DataCell>
            <Table.DataCell>
              {!!orgEnhet?.leder?.navident ? (
                <SmartLink
                  href={`/ressurs/${orgEnhet.leder.navident}`}
                  target="_blank"
                >
                  {orgEnhet.leder.visningsnavn}
                </SmartLink>
              ) : (
                ""
              )}
            </Table.DataCell>
            <Table.DataCell>{orgEnhet.nomNivaa}</Table.DataCell>
            <Table.DataCell>
              {!!orgEnhet.orgEnhetsType
                ? OrgEnhetsTypeNameMapping[orgEnhet.orgEnhetsType]
                : ""}
            </Table.DataCell>
            <Table.DataCell>{orgEnhet.tilgangsenhetId}</Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  );
}
