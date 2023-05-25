import type { SerializedStyles } from "@emotion/react";
import { css } from "@emotion/react";
import { Tag } from "@navikt/ds-react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import type { OptionProps } from "react-select";
import { components } from "react-select";

import type { Ressurs } from "~/api/generated/graphql_generated";

import { AsyncSearch } from "./CustomSelectComponents";

const RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;

type SearchOption = {
  value: string;
  label: string;
  tag: string;
  url: string;
  css: SerializedStyles;
};

type SearchResultRaw =
  | {
      __typename: "OrgEnhet";
      id: string;
      navn: string;
    }
  | {
      __typename: "Ressurs";
      navident: string;
      visningsnavn: string;
      orgTilknytning: {
        orgEnhet: {
          id: string;
          navn: string;
        };
      }[];
    };

const OptionWithTag = (properties: OptionProps<SearchOption, false>) => {
  return (
    <components.Option {...properties}>
      <div
        css={css`
          display: flex;
          justify-content: space-between;
        `}
      >
        <span>{properties.data.label}</span>
        <Tag css={properties.data.css} size="small" variant="info">
          {properties.data.tag}
        </Tag>
      </div>
    </components.Option>
  );
};

export function SearchBar() {
  const navigate = useNavigate();

  return (
    <AsyncSearch
      components={{ Option: OptionWithTag }}
      controlShouldRenderValue={false}
      isClearable={false}
      loadOptions={searchRessurs}
      noOptionsMessage={({ inputValue }: { inputValue: string }) =>
        inputValue.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT
          ? "Du må skrive minst 3 tegn for å søke"
          : `Fant ingen resultater for "${inputValue}"`
      }
      onChange={(selectedOption) =>
        selectedOption && navigate(selectedOption.url)
      }
      placeholder="Søk etter ressurser eller organisasjonsenheter"
    />
  );
}

async function searchRessurs(inputValue: string) {
  if (inputValue.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  return await createOptions(inputValue);
}

async function performSearch(searchString: string): Promise<SearchResultRaw[]> {
  const searchQuery = `query search($searchString: String!, $orgEnhetFilter: OrgEnhetSearchFilter!, $ressursFilter: RessursSearchFilter) {
        search(term: $searchString, orgEnhetFilter: $orgEnhetFilter, ressursFilter: $ressursFilter) {
        __typename
        ... on Ressurs {
          navident
          visningsnavn
          orgTilknytning {
            orgEnhet {
              id
              navn
            }
          }
        }
        ... on OrgEnhet {
          id
          navn
        }
      }
    }`;

  const orgEnhetFilter = {
    orgnivSelection: "ORGNIV_ORGENHET",
    gyldigTomSelection: "IKKE_PASSERT",
  };

  const ressursFilter = {
    sektorSelection: "ALLE",
    statusSelection: "ALLE",
  };

  const result = await axios.post("/nom-api/graphql", {
    operationName: "search",
    query: searchQuery,
    variables: {
      searchString: searchString,
      orgEnhetFilter: orgEnhetFilter,
      ressursFilter: ressursFilter,
    },
  });

  return result.data.data.search;
}

async function navidentSearch(navident: string): Promise<SearchResultRaw[]> {
  const searchQuery = `query searchRessurs($searchString: String!) {
        searchRessurs(term: $searchString) {
        __typename
        ... on Ressurs {
          navident
          visningsnavn
          orgTilknytning {
            orgEnhet {
              id
              navn
            }
          }
        }
      }
    }`;

  const result = await axios.post("/nom-api/graphql", {
    operationName: "searchRessurs",
    query: searchQuery,
    variables: {
      searchString: navident,
    },
  });

  return result.data.data.searchRessurs?.filter(
    (ressurs: Ressurs) => ressurs.navident === navident
  );
}

async function createOptions(inputValue: string): Promise<SearchOption[]> {
  // Får bedre resultat av en eller annen grunn når man tar termene i single-quotes
  const wrappedInputValue = "'" + inputValue + "'";
  const inputValueIsNavident = inputValue.match(/^[A-Za-z]\d{6}$/) !== null;
  const searchResults = inputValueIsNavident
    ? await navidentSearch(inputValue.toUpperCase())
    : await performSearch(wrappedInputValue);

  return searchResults.map((r) => {
    switch (r.__typename) {
      case "Ressurs": {
        return {
          tag: "Ressurs",
          url: `/ressurs/${r.navident}`,
          label: r.visningsnavn,
          css: css`
            background: #e0d8e9;
            border-color: #c0b2d2;
          `,
          value: r.navident,
        };
      }
      case "OrgEnhet": {
        return {
          tag: "OrgEnhet",
          url: /org/ + r.id,
          label: r.navn,
          css: css`
            background: #ebcbd4;
            border-color: #d4a9b6;
          `,
          value: r.id,
        };
      }
    }
  });
}
