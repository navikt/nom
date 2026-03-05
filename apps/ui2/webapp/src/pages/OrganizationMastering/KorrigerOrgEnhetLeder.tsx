import { css } from "@emotion/react";
import { BodyShort, Loader } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import axios from "axios";
import React, { useState } from "react";
import { useController } from "react-hook-form";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";

export const KorrigerOrgEnhetLeder = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  const { id } = useSearch({ from: "/korriger-leder-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Endre leder på organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop>
          <KorrigeringOrgEnhetLeder orgEnhetId={id} />
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhetLeder({ orgEnhetId }: { orgEnhetId?: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
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

  if (!orgEnhet) {
    return;
  }

  return (
    <>
      <OrgEnhetInfoWrapper
        heading="Valgt organisasjonsenhet"
        orgEnhetId={orgEnhetId}
      />

      <>{orgEnhet.id}</>
    </>
  );
}

function FormRessursSearchWrapper({
  name,
  orgEnhetLederOption,
}: {
  name: string;
  orgEnhetLederOption: any;
}) {
  const { field, fieldState } = useController({
    name,
  });

  const [lederChanged, setLederChanged] = useState<boolean>(false);

  return (
    <SelectLayoutWrapper
      error={fieldState.error}
      htmlFor="Leder"
      label="Leder for ny organisasjonsenhet"
    >
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < 3
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) => {
          field.onChange(selectedOption?.value ?? "");

          setLederChanged(true);
        }}
        placeholder="Søk etter medarbeider"
        value={lederChanged ? undefined : orgEnhetLederOption}
      />
    </SelectLayoutWrapper>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  if (searchTerm.length < 3) {
    return [];
  }

  const searchQuery = `query ressurs($searchTerm: String!, $filter: RessursSearchFilter) {
        searchRessurs(term: $searchTerm, filter: $filter) {
          navident
          visningsnavn
        }
      }`;

  const filter = { statusSelection: "AKTIV" };

  const resultRaw = await axios.post("/frackend/nom-api/graphql", {
    operationName: "ressurs",
    query: searchQuery,
    variables: {
      searchTerm: searchTerm,
      filter: filter,
    },
  });

  const resultRessurser = resultRaw.data.data.searchRessurs;

  return resultRessurser.map((ressurs: any) => ({
    label: `${ressurs.visningsnavn} (${ressurs.navident})`,
    value: ressurs.navident,
  }));
}
