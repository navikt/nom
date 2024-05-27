import type { SerializedStyles } from "@emotion/react";
import { css } from "@emotion/react";
import { Tag } from "@navikt/ds-react";
import type { NavigateOptions } from "@tanstack/react-router";
import { useNavigate } from "@tanstack/react-router";
import axios from "axios";
import type { OptionProps } from "react-select";
import { components } from "react-select";

import { AsyncSearch } from "./CustomSelectComponents";

const RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;

type SearchOption = {
  value: string;
  label: string;
  tag: string;
  navigationOptions: NavigateOptions;
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
        selectedOption && navigate(selectedOption.navigationOptions)
      }
      placeholder="Søk etter medarbeider eller organisasjonsenhet"
    />
  );
}

async function searchRessurs(inputValue: string) {
  // Se bort ifra NAV når man søker
  const newSearchTerm = inputValue.toLowerCase().replace("nav ", "");
  if (newSearchTerm.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  return await createOptions(newSearchTerm);
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

  const result = await axios.post("/frackend/nom-api/graphql", {
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

async function queryForOrgenhet(
  orgenhetId: string,
): Promise<SearchResultRaw[]> {
  const orgenhetQuery = `query orgEnhet($orgenhetId: String) {
    orgEnhet(where: {id: $orgenhetId}){
      __typename
      id
      navn
    }
  }`;
  const result = await axios.post("/frackend/nom-api/graphql", {
    operationName: "orgEnhet",
    query: orgenhetQuery,
    variables: {
      orgenhetId: orgenhetId,
    },
  });

  return [result.data.data.orgEnhet];
}

async function queryForRessurs(
  navident: string | null,
  telefonNummer: string | null,
): Promise<SearchResultRaw[]> {
  const ressursQuery = `query ressurs($navident: String, $telefonNummer: String) {
        ressurs(where: {navident: $navident, telefonNummer: $telefonNummer}) {
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

  const result = await axios.post("/frackend/nom-api/graphql", {
    operationName: "ressurs",
    query: ressursQuery,
    variables: {
      navident,
      telefonNummer,
    },
  });

  return [result.data.data.ressurs];
}

async function createOptions(inputValue: string): Promise<SearchOption[]> {
  const lookupNavident = /^[A-Za-z]\d{6}$/.test(inputValue)
    ? inputValue.toUpperCase()
    : null;
  const lookupTelefon = /^\d{8,}$/.test(inputValue.replaceAll(/\s/g, ""))
    ? inputValue.replaceAll(/\s/g, "")
    : null;

  const lookupOrgenhetId = /^[A-Za-z]{2}\d{3}[A-Za-z]$/.test(inputValue)
    ? inputValue
    : null;

  const searchResults = await (async function DO() {
    if (lookupNavident || lookupTelefon) {
      return await queryForRessurs(lookupNavident, lookupTelefon);
    }
    if (lookupOrgenhetId) {
      return await queryForOrgenhet(inputValue);
    }

    // fritekst søk på navn
    // Får bedre resultat av en eller annen grunn når man tar termene i single-quotes
    const wrappedInputValue = "'" + inputValue + "'";
    return performSearch(wrappedInputValue);
  })();

  return searchResults.map((r) => {
    switch (r.__typename) {
      case "Ressurs": {
        return {
          tag: "Medarbeider",
          navigationOptions: {
            to: "/ressurs/$id",
            params: {
              id: r.navident,
            },
          },
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
          navigationOptions: {
            to: "/org/$id",
            params: {
              id: r.id,
            },
          },
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
