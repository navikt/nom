import {css} from "@emotion/css";
import {Tag} from "@navikt/ds-react";
import {useNavigate} from "react-router-dom";
import type {OptionProps} from "react-select";
import {components} from "react-select";
import AsyncSelect from "react-select/async";

const RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT = 3;

type SearchOption = {
    value: string;
    label: string;
    tag: string;
    url: string;
    className: string;
};

type SearchResultRaw =
    | {
    __typename: "OrgEnhet";
    agressoId: string;
    navn: string;
    orgNiv: string;
}
    | {
    __typename: "Ressurs";
    navident: string;
    visningsnavn: string;
    orgTilknytning: {
        orgEnhet: {
            agressoId: string;
            orgNiv: string;
            navn: string;
        };
    }[];
};

const Option = (properties: OptionProps<SearchOption>) => {
    return (
        <components.Option {...properties}>
            <div
                className={css`
                  display: flex;
                  justify-content: space-between;
                `}
            >
                <span>{properties.data.label}</span>
                <Tag className={properties.data.className} size="small" variant="info">
                    {properties.data.tag}
                </Tag>
            </div>
        </components.Option>
    );
};

export function SearchBar() {
    const navigate = useNavigate();

    return (
        <AsyncSelect
            cacheOptions
            className={css`
              cursor: text;
              min-width: 200px;
              border-radius: var(--navds-border-radius-medium);
              border: 1px solid var(--navds-select-color-border);

              &:focus-within {
                outline: none;
                box-shadow: var(--navds-shadow-focus);
              }
            `}
            components={{Option}}
            controlShouldRenderValue={false}
            isClearable
            loadOptions={searchRessurs}
            loadingMessage={() => "Søker..."}
            menuPortalTarget={document.body}
            noOptionsMessage={({inputValue}: { inputValue: string }) =>
                inputValue.length < RESOURCE_SEARCH_TERM_LOWER_LENGTH_LIMIT
                    ? "Du må skrive minst 3 tegn for å søke"
                    : `Fant ingen resultater for "${inputValue}"`
            }
            // NOTE 27 Oct 2022 (Johannes Moskvil): Stupid hack to please TS. SelectedOption can be multiple if used as a multi select therefore ensure only one value is processed
            onChange={(selectedOption) =>
                selectedOption && navigate([selectedOption].flat()[0].url)
            }
            placeholder="Søk etter ressurser eller organisasjonsenheter"
            styles={{
                // Removes default focus-border so it can be replaced with focus from DesignSystem
                control: (base) => ({
                    ...base,
                    boxShadow: "none",
                    border: 0,
                    cursor: "text",
                    div: {div: {color: `var(--navds-dropdown-menu-color)`}},
                }),
                // Make border and size of input box to be identical with those from DesignSystem
                valueContainer: (base) => ({...base, padding: "8px", color: "black"}),
                indicatorSeparator: (base) => ({...base, display: "none"}),
                dropdownIndicator: (base) => ({...base, display: "none"}),
            }}
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

    const searchQuery =
        `query search($searchString: String!, $orgEnhetFilter: OrgEnhetSearchFilter!, $ressursFilter: RessursSearchFilter) {
        search(term: $searchString, orgEnhetFilter: $orgEnhetFilter, ressursFilter: $ressursFilter) {
        __typename
        ... on Ressurs {
          navident
          visningsnavn
          orgTilknytning {
            orgEnhet {
              agressoId
              orgNiv
              navn
            }
          }
        }
        ... on OrgEnhet {
          agressoId
          navn
          orgNiv
        }
      }
    }`;

    const orgEnhetFilter = {
        orgnivSelection: "ORGNIV_ORGENHET",
        gyldigTomSelection: "IKKE_PASSERT"
    }

    const ressursFilter = {
        sektorSelection: "ALLE",
        statusSelection: "ALLE"
    }


    const fetchresult = await fetch("/nom-api/graphql", {
        body: JSON.stringify({
            operationName: "search",
            query: searchQuery,
            variables: {"searchString": searchString, "orgEnhetFilter": orgEnhetFilter, "ressursFilter": ressursFilter},
        }),
        method: "POST",
        headers: {
            "content-type": "application/json",
        },
    });
    const json = await fetchresult.json();
    return json.data.search;
}

const searchResultClassname1 = css`
  background: #e0d8e9;
  border-color: #c0b2d2;
`;

const searchResultClassname2 = css`
  background: #ebcbd4;
  border-color: #d4a9b6;
`;

async function createOptions(inputValue: string): Promise<SearchOption[]> {
    // får bedre resultat av en eller annen grunn når man tar termene i single-quotes
    const wrappedInputValue = "'" + inputValue + "'";
    const results = await performSearch(wrappedInputValue);

    return results.map((r) => {
        switch (r.__typename) {
            case "Ressurs": {
                const oe = r.orgTilknytning[0]?.orgEnhet;
                const url = oe
                    ? "/org/" + oe.agressoId + "/" + oe.orgNiv + "/" + r.navident
                    : "/ressurs/" + r.navident;
                return {
                    tag: "Ressurs",
                    url: url,
                    label: r.visningsnavn,
                    className: searchResultClassname1,
                    value: r.navident,
                };
            }
            case "OrgEnhet": {
                return {
                    tag: "OrgEnhet",
                    url: /org/ + r.agressoId + "/" + r.orgNiv,
                    label: r.navn,
                    className: searchResultClassname2,
                    value: r.agressoId,
                };
            }
        }
    });
}
