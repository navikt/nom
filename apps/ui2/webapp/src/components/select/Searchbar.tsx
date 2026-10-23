import type { SerializedStyles } from "@emotion/react";
import { css } from "@emotion/react";
import { Tag } from "@navikt/ds-react";
import type { NavigateOptions } from "@tanstack/react-router";
import { useNavigate } from "@tanstack/react-router";
import axios from "axios";
import type { OptionProps } from "react-select";
import { components } from "react-select";

import { useHasAdminAccess } from "~/api/useGetSession";
import { isInDevelopment } from "~/utils";

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
  const hasAdminAccess = useHasAdminAccess();

  return (
    <AsyncSearch
      components={{ Option: OptionWithTag }}
      controlShouldRenderValue={false}
      isClearable={false}
      loadOptions={(s) => searchIfEnoughInput(s, hasAdminAccess)}
      noOptionsMessage={({ inputValue }: { inputValue: string }) =>
        inputValue.toLowerCase().replace("nav ", "").trim().length <
        RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT
          ? "Du må skrive lenger for å søke"
          : `Fant ingen resultater for "${inputValue}"`
      }
      onChange={(selectedOption) =>
        selectedOption && navigate(selectedOption.navigationOptions)
      }
      placeholder="Søk etter medarbeider eller organisasjonsenhet"
    />
  );
}

async function searchIfEnoughInput(inputValue: string, hasAdmin: boolean) {
  // Se bort ifra Nav når man søker
  const newSearchTerm = inputValue.toLowerCase().replace("nav ", "").trim();

  if (newSearchTerm.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT) {
    return [];
  }

  return await createOptions(newSearchTerm, hasAdmin);
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
  orgEnhetId: string | null,
  agressoId: string | null,
): Promise<SearchResultRaw[]> {
  const orgenhetQuery = `
    query getOrgEnheter(
      $orgenhetIds: [String!]
      $agressoIds: [String!]
    ) {
      orgEnheter(
        where: {
          ider: $orgenhetIds
          agressoIder: $agressoIds
        }
      ) {
        orgEnhet {
          __typename
          id
          navn
          organiseringer(retning: over) {
            orgEnhet {
              id
            }
          }
        }
      }
    }`;

  const result = await axios.post("/frackend/nom-api/graphql", {
    operationName: "getOrgEnheter",
    query: orgenhetQuery,
    variables: {
      orgenhetIds: orgEnhetId ? [orgEnhetId] : null,
      agressoIds: agressoId ? [agressoId] : null,
    },
  });

  return result.data.data.orgEnheter
    .map((entry: any) => entry.orgEnhet)
    .filter(Boolean)
    .filter((result: any) =>
      agressoId ? (result.organiseringer?.length ?? 0) > 0 : true,
    );
}

export async function queryForRessurs(
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

  return [result.data.data.ressurs].filter((result) => !!result);
}

export async function commonSearch(
  inputValue: string,
  rawResultFilter: (srr: SearchResultRaw) => boolean = () => true,
) {
  const lookupNavident = /^[A-Za-z]\d{6}$/.test(inputValue)
    ? inputValue.toUpperCase()
    : null;
  const lookupTelefon = /^\d{8,}$/.test(inputValue.replaceAll(/\s/g, ""))
    ? inputValue.replaceAll(/\s/g, "")
    : null;

  const lookupOrgenhetId = /^[A-Za-z]{2}\d{3}[A-Za-z]$/.test(
    inputValue.replaceAll(/\s/g, ""),
  )
    ? inputValue.replaceAll(/\s/g, "")
    : null;
  const lookupAgressoId = /^\d+$/.test(inputValue.replaceAll(/\s/g, ""))
    ? inputValue.replaceAll(/\s/g, "")
    : null;

  const lookupRessurs = lookupNavident || lookupTelefon;
  const lookupOrgEnhet = lookupOrgenhetId || lookupAgressoId;

  if (lookupRessurs || lookupOrgEnhet) {
    const result: SearchResultRaw[] = [];

    const promises = [];

    if (lookupRessurs) {
      promises.push(queryForRessurs(lookupNavident, lookupTelefon));
    }

    if (lookupOrgEnhet) {
      promises.push(queryForOrgenhet(lookupOrgenhetId, lookupAgressoId));
    }

    const results = await Promise.all(promises);
    results.forEach((r) => result.push(...r));

    if (/^\d{6}$/.test(inputValue.trim())) {
      const inputHasWhitespace = /\s/.test(inputValue.trim());
      const wrappedInputValue = inputHasWhitespace
        ? "'" + inputValue + "'"
        : inputValue;

      const searchResults = await performSearch(wrappedInputValue);
      result.push(
        ...searchResults.filter((element) => rawResultFilter(element)),
      );
    }

    return result;
  }

  // Fritekstsøk på navn
  // Får bedre resultat av en eller annen grunn når man tar termene i single-quotes
  const inputHasWhitespace = /\s/.test(inputValue.trim());
  const wrappedInputValue = inputHasWhitespace
    ? "'" + inputValue + "'"
    : inputValue;

  const searchResults = await performSearch(wrappedInputValue);
  return searchResults.filter((element) => rawResultFilter(element));
}

function notVikafossenEnhetPredicate(searchResultRaw: SearchResultRaw) {
  const nomIdVikafossen = new Set(["ru664y", "fe178t"]);
  return !(
    searchResultRaw.__typename === "OrgEnhet" &&
    nomIdVikafossen.has(searchResultRaw.id)
  );
}

async function createOptions(
  inputValue: string,
  hasAdmin: boolean,
): Promise<SearchOption[]> {
  const skalFiltrereBortVikafossen = !hasAdmin && !isInDevelopment();

  const filterToUse = skalFiltrereBortVikafossen
    ? notVikafossenEnhetPredicate
    : () => true;

  const searchResults = await commonSearch(inputValue, filterToUse);

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
