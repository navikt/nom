import { css } from "@emotion/react";
import { Checkbox, Tag } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import axios from "axios";
import type { OptionProps } from "react-select";
import { components } from "react-select";

import type { OrgEnhetSearchFilter } from "~/api/generated/graphql_generated";
import { AsyncSearch } from "~/components/select/CustomSelectComponents";
import { ressurserAdminRoute } from "~/reactRouterRoutes";

const SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;
export function OrgEnhetSearch() {
  const search = useSearch({ from: ressurserAdminRoute.id });
  const navigate = useNavigate();

  const includeUnderliggendeOrgEnheter = search.includeUnderliggendeOrgEnheter;

  return (
    <div
      css={css`
        display: flex;
        flex-direction: row;
        gap: var(--a-spacing-4);
      `}
    >
      <AsyncSearch
        components={{ Option: OrgenhetOption }}
        controlShouldRenderValue={false}
        hideSelectedOptions={false}
        loadOptions={(inputValue) => searchOrgEnheter(inputValue, {})}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < SEARCH_TERM_LOWER_LENGTH_LIMIT
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOrgEnhet) =>
          selectedOrgEnhet
            ? navigate({
                search: { ...search, orgEnhet: selectedOrgEnhet.id },
              })
            : undefined
        }
        placeholder="Søk etter organisasjonsenheter"
      />
      <Checkbox
        checked={includeUnderliggendeOrgEnheter}
        onChange={() =>
          navigate({
            search: {
              ...search,
              includeUnderliggendeOrgEnheter: !includeUnderliggendeOrgEnheter,
            },
          })
        }
      >
        Inkluder underliggende orgenheter
      </Checkbox>
    </div>
  );
}

const OrgenhetOption = (
  properties: OptionProps<OrgEnhetSearchResult, false>,
) => {
  return (
    <components.Option {...properties}>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <span>{properties.data.navn}</span>
        <Tag
          css={css`
            background: #ebcbd4;
            border-color: #d4a9b6;
          `}
          size="small"
          variant="info"
        >
          {properties.data.orgNiv}
        </Tag>
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
      gyldigTom
      orgNiv
      organiseringer {
      retning
      orgEnhet {
        id
      }
    }
    }
  }
`;

  const result = await axios.post("/nom-api/graphql", {
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
  gyldigTom: string | null;
  orgNiv: string;
  organiseringer: {
    retning: string;
    orgEnhet: {
      id: string;
    };
  }[];
};
