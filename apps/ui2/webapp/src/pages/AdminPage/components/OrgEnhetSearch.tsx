import { css } from "@emotion/react";
import { BodyShort, Checkbox } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import axios from "axios";
import { useState } from "react";
import type { OptionProps, SingleValueProps } from "react-select";
import { components } from "react-select";

import type { OrgEnhetSearchFilter } from "~/api/generated/graphql_generated";
import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";

const SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;
export function OrgEnhetSearch() {
  const search = useSearch({ from: "/admin/ressurser" });
  const [selectedOrgEnhet, setSelectedOrgEnhet] = useState<
    OrgEnhetSearchResult | undefined
  >();
  const navigate = useNavigate({ from: "/admin/ressurser" });

  const includeUnderliggendeOrgEnheter = search.includeUnderliggendeOrgEnheter;

  return (
    <div
      css={css`
        display: flex;
        flex-direction: row;
        gap: var(--a-spacing-4);
        align-items: end;
      `}
    >
      <SelectLayoutWrapper
        htmlFor="Organisasjonsenhet"
        label="Organisasjonsenhet"
      >
        <AsyncSearch
          components={{
            Option: OrgenhetOption,
            SingleValue: OrgenhetOptionSingleValue,
          }}
          css={css`
            min-width: 300px;
          `}
          inputId="Organisasjonsenhet"
          key={selectedOrgEnhet?.id}
          loadOptions={(inputValue) => searchOrgEnheter(inputValue, {})}
          noOptionsMessage={({ inputValue }: { inputValue: string }) =>
            inputValue.length < SEARCH_TERM_LOWER_LENGTH_LIMIT
              ? "Du må skrive minst 3 tegn for å søke"
              : `Fant ingen resultater for "${inputValue}"`
          }
          onChange={(selectedOrgEnhet) => {
            navigate({
              search: (previousSearch) => ({
                ...previousSearch,
                orgEnhet: selectedOrgEnhet?.id,
              }),
            });
            setSelectedOrgEnhet(selectedOrgEnhet ?? undefined);
          }}
          placeholder="Søk etter organisasjonsenheter"
          value={search.orgEnhet ? selectedOrgEnhet : null}
        />
      </SelectLayoutWrapper>
      <Checkbox
        checked={includeUnderliggendeOrgEnheter}
        onChange={() =>
          navigate({
            search: (previousSearch) => ({
              ...previousSearch,
              includeUnderliggendeOrgEnheter: !includeUnderliggendeOrgEnheter,
            }),
          })
        }
      >
        <BodyShort size="small">Inkluder underliggende orgenheter</BodyShort>
      </Checkbox>
    </div>
  );
}

const OrgenhetOptionSingleValue = (
  properties: SingleValueProps<OrgEnhetSearchResult, false>,
) => {
  return (
    <components.SingleValue {...properties}>
      {properties.data.navn}
    </components.SingleValue>
  );
};

const OrgenhetOption = (
  properties: OptionProps<OrgEnhetSearchResult, false>,
) => {
  return (
    <components.Option {...properties}>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
          gap: var(--a-spacing-4);
        `}
      >
        <span>{properties.data.navn}</span>
        <b>{properties.data.id}</b>
        <span>{properties.data.orgNiv}</span>
      </div>
    </components.Option>
  );
};

async function searchOrgEnheter(
  searchString: string,
  filter: OrgEnhetSearchFilter,
): Promise<OrgEnhetSearchResult[]> {
  if (searchString.length < SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  return await performSearch(searchString, filter);
}

async function performSearch(
  searchString: string,
  filter: OrgEnhetSearchFilter,
): Promise<OrgEnhetSearchResult[]> {
  const searchQuery = `
  query search($searchString: String!, $filter: OrgEnhetSearchFilter!) {
    searchOrgEnhet(term: $searchString, filter: $filter) {
      id
      navn
      orgNiv
    }
  }
`;

  const result = await axios.post("/frackend/nom-api/graphql", {
    operationName: "search",
    query: searchQuery,
    variables: {
      searchString: searchString,
      filter: filter,
    },
  });

  return result.data.data.searchOrgEnhet;
}

type OrgEnhetSearchResult = {
  id: string;
  navn: string;
  orgNiv: string;
};
